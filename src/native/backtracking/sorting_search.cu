#include "backtracking/sorting_search.cuh"

#include <algorithm>
#include <cstddef>
#include <cmath>
#include <mutex>
#include <stdexcept>
#include <utility>
#include <vector>

#include "backtracking/slot_manager.cuh"
#include "core/cuda_error.cuh"
#include "core/cuda_memory.cuh"
#include "backtracking/transposition.cuh"

namespace {
    constexpr int WARP_SIZE = 32;

    struct BacktrackingNode {
        uint8_t depth;
        uint8_t evenCycles;
        PackedMove moves[MAX_DEPTH];
        uint8_t spi[MAX_N];
        uint8_t pi[MAX_N];
    };

    struct BacktrackingQueue {
        int head;
        int tail;
        int size;
        int lock;
        int capacity;
        BacktrackingNode *nodes;
    };

    constexpr int DEDUP_MAX_PROBE = 8;
    constexpr unsigned int DEDUP_EMPTY = 0;
    constexpr unsigned int DEDUP_WRITING = 1;
    constexpr unsigned int DEDUP_READY = 2;

    struct DedupEntry {
        unsigned int state;
        unsigned int bestDepth;
        uint8_t spi[MAX_N];
    };

    struct BacktrackingResult {
        int found;
        int solutionDepth;
        int overflowed;
        PackedMove solutionMoves[MAX_DEPTH];
    };

    struct DeviceSearchContext {
        const Move *triples;
        int tripleCount;
        const short *spi0;
        const short *pi0;
        int n;
        int initialEvenCycles;
        int maxDepth;
        int requiredGainByDepth[MAX_DEPTH + 1];
        int requiredGainAtMaxDepth;
        float minRate;

        int queueCount;
        BacktrackingQueue *queues;
        BacktrackingQueue *overflow;

        int *foundFlag;
        int *solutionDepth;
        PackedMove *solutionMoves;
        int *cancelFlag;
        int *overflowFlag;

        DedupEntry *dedupTable;
        int dedupCapacity;
        int reverseTripleOrder;
        int fullSorting;
    };

    struct SlotBuffers {
        int device = -1;
        int queueCount = 0;
        int localCapacity = 0;
        int overflowCapacity = 0;
        int dedupCapacity = 0;
        int cachedTriplesN = -1;
        DeviceAsyncUniquePtr<BacktrackingQueue> queues{};
        DeviceAsyncUniquePtr<BacktrackingQueue> overflow{};
        DeviceAsyncUniquePtr<BacktrackingNode> nodes{};
        DeviceAsyncUniquePtr<BacktrackingNode> overflowNodes{};
        DeviceAsyncUniquePtr<DedupEntry> dedupTable{};
        DeviceAsyncUniquePtr<Move> triples{};
        DeviceAsyncUniquePtr<short> inputVectors{};
        DeviceAsyncUniquePtr<BacktrackingResult> result{};
        HostPinnedUniquePtr<BacktrackingResult> hostResult{};
    };

    struct SlotBufferView {
        BacktrackingQueue *queues = nullptr;
        BacktrackingQueue *overflow = nullptr;
        BacktrackingNode *nodes = nullptr;
        BacktrackingNode *overflowNodes = nullptr;
        DedupEntry *dedupTable = nullptr;
        Move *triples = nullptr;
        short *inputVectors = nullptr;
        BacktrackingResult *result = nullptr;
        BacktrackingResult *hostResult = nullptr;
        bool triplesNeedUpload = false;
    };

    std::mutex gSlotsMutex;
    std::vector<SlotBuffers> gSlots;

    void releaseSlotBuffers(SlotBuffers &buffers) {
        buffers.queues.reset();
        buffers.overflow.reset();
        buffers.nodes.reset();
        buffers.overflowNodes.reset();
        buffers.dedupTable.reset();
        buffers.triples.reset();
        buffers.inputVectors.reset();
        buffers.result.reset();
        buffers.hostResult.reset();
        buffers.queueCount = 0;
        buffers.localCapacity = 0;
        buffers.overflowCapacity = 0;
        buffers.dedupCapacity = 0;
        buffers.cachedTriplesN = -1;
    }

    SlotBufferView viewSlotBuffers(const SlotBuffers &buffers, const int n) noexcept {
        return SlotBufferView{
                buffers.queues.get(),
                buffers.overflow.get(),
                buffers.nodes.get(),
                buffers.overflowNodes.get(),
                buffers.dedupTable.get(),
                buffers.triples.get(),
                buffers.inputVectors.get(),
                buffers.result.get(),
                buffers.hostResult.get(),
                buffers.cachedTriplesN != n
        };
    }

    SlotBufferView getSlotBufferView(const int slot, const int n) {
        std::lock_guard<std::mutex> lock(gSlotsMutex);
        if (slot < 0 || static_cast<size_t>(slot) >= gSlots.size()) {
            throw std::invalid_argument("invalid slot");
        }
        return viewSlotBuffers(gSlots[slot], n);
    }

    void markSlotTriplesUploaded(const int slot, const int device, const int n) {
        std::lock_guard<std::mutex> lock(gSlotsMutex);
        if (slot < 0 || static_cast<size_t>(slot) >= gSlots.size()) return;
        auto &buffers = gSlots[slot];
        if (buffers.device == device && buffers.triples != nullptr) {
            buffers.cachedTriplesN = n;
        }
    }

    // ─── Device helper functions ─────────────────────────────────────────────────

    __device__ __forceinline__ void acquire_lock(int *lock) {
        while (atomicCAS(lock, 0, 1) != 0);
    }

    __device__ __forceinline__ void release_lock(int *lock) {
        atomicExch(lock, 0);
    }

    __device__ __forceinline__ bool owner_push(BacktrackingQueue &q, const BacktrackingNode &node) {
        acquire_lock(&q.lock);
        if (q.size >= q.capacity) { release_lock(&q.lock); return false; }
        q.nodes[q.tail] = node;
        q.tail = (q.tail + 1) % q.capacity;
        q.size++;
        __threadfence();
        release_lock(&q.lock);
        return true;
    }

    __device__ __forceinline__ bool owner_pop(BacktrackingQueue &q, BacktrackingNode &node) {
        acquire_lock(&q.lock);
        if (q.size <= 0) { release_lock(&q.lock); return false; }
        q.tail = (q.tail + q.capacity - 1) % q.capacity;
        node = q.nodes[q.tail];
        q.size--;
        __threadfence();
        release_lock(&q.lock);
        return true;
    }

    __device__ __forceinline__ bool stealer_pop(BacktrackingQueue &q, BacktrackingNode &node) {
        if (atomicAdd(&q.size, 0) <= 0) return false;
        acquire_lock(&q.lock);
        if (q.size <= 0) { release_lock(&q.lock); return false; }
        node = q.nodes[q.head];
        q.head = (q.head + 1) % q.capacity;
        q.size--;
        __threadfence();
        release_lock(&q.lock);
        return true;
    }

    __device__ __forceinline__ bool donate_push(BacktrackingQueue &q, const BacktrackingNode &node) {
        if (atomicAdd(&q.size, 0) >= q.capacity) return false;
        acquire_lock(&q.lock);
        if (q.size >= q.capacity) { release_lock(&q.lock); return false; }
        q.head = (q.head + q.capacity - 1) % q.capacity;
        q.nodes[q.head] = node;
        q.size++;
        __threadfence();
        release_lock(&q.lock);
        return true;
    }

    __device__ __forceinline__ bool warp_should_stop(const DeviceSearchContext &ctx) {
        const bool shouldStop = atomicAdd(ctx.foundFlag, 0) != 0
                                || atomicAdd(ctx.cancelFlag, 0) != 0
                                || atomicAdd(ctx.overflowFlag, 0) != 0;
        return __any_sync(0xFFFFFFFF, shouldStop);
    }

    // ─── Deduplication table ─────────────────────────────────────────────────────

    __device__ __forceinline__ unsigned int dedup_hash(const uint8_t *spi, const int n) {
        unsigned int h = 2166136261u;
        for (int i = 0; i < n; ++i) {
            h ^= static_cast<unsigned int>(spi[i]);
            h *= 16777619u;
        }
        return h;
    }

    __device__ __forceinline__ bool dedup_entry_matches(const DedupEntry &entry,
                                                        const uint8_t *spi,
                                                        const int n) {
        for (int i = 0; i < n; ++i) {
            if (entry.spi[i] != spi[i]) return false;
        }
        return true;
    }

    __device__ __forceinline__ bool dedup_insert_empty(DedupEntry &entry,
                                                       const uint8_t *spi,
                                                       const int n,
                                                       const int depth) {
        const unsigned int previousState = atomicCAS(&entry.state, DEDUP_EMPTY, DEDUP_WRITING);
        if (previousState != DEDUP_EMPTY) return false;
        entry.bestDepth = static_cast<unsigned int>(depth);
        for (int i = 0; i < n; ++i) entry.spi[i] = spi[i];
        __threadfence();
        atomicExch(&entry.state, DEDUP_READY);
        return true;
    }

    __device__ __forceinline__ bool dedup_try_admit(DedupEntry *dedupTable,
                                                    const int dedupCapacity,
                                                    const uint8_t *spi,
                                                    const int n,
                                                    const int depth) {
        if (dedupTable == nullptr || dedupCapacity <= 0) return true;

        const auto candidateDepth = static_cast<unsigned int>(depth);
        const unsigned int h = dedup_hash(spi, n);
        const unsigned int startSlot = h % static_cast<unsigned int>(dedupCapacity);
        int firstEmpty = -1;
        bool improvedExisting = false;
        bool dominated = false;

        for (int probe = 0; probe < DEDUP_MAX_PROBE; ++probe) {
            const unsigned int idx = (startSlot + probe) % static_cast<unsigned int>(dedupCapacity);
            DedupEntry &entry = dedupTable[idx];
            const unsigned int previousState = atomicAdd(&entry.state, 0);
            if (previousState == DEDUP_EMPTY) {
                if (firstEmpty < 0) firstEmpty = static_cast<int>(idx);
                continue;
            }
            if (previousState != DEDUP_READY) continue;
            if (!dedup_entry_matches(entry, spi, n)) continue;

            const unsigned int previousBest = atomicMin(&entry.bestDepth, candidateDepth);
            if (previousBest <= candidateDepth) dominated = true;
            else improvedExisting = true;
        }

        if (dominated) return false;
        if (improvedExisting) return true;
        if (firstEmpty >= 0) {
            DedupEntry &entry = dedupTable[firstEmpty];
            if (dedup_insert_empty(entry, spi, n, depth)) return true;
        }
        return true;
    }

    // ─── Even-cycle counting ─────────────────────────────────────────────────────

    __device__ __forceinline__ int count_even_cycles(const uint8_t *spi, const int n) {
        uint64_t visited = 0;
        int count = 0;
        for (int s = 0; s < n; ++s) {
            if (visited & (1ULL << s)) continue;
            int size = 0;
            int cur = s;
            do {
                visited |= (1ULL << cur);
                size++;
                cur = spi[cur];
            } while (cur != s);
            if (size & 1) count++;
        }
        return count;
    }

    // ─── Pruning logic ───────────────────────────────────────────────────────────

    __device__ __forceinline__ bool satisfies_rate(const DeviceSearchContext &ctx,
                                                   const int evenCycles,
                                                   const int depth) {
        if (depth <= 0) return false;
        if (ctx.fullSorting) {
            if (evenCycles != ctx.n) return false;
        }
        const int gained = evenCycles - ctx.initialEvenCycles;
        const float achievedRatio = static_cast<float>(gained) / static_cast<float>(depth);
        return gained > 0 && achievedRatio >= ctx.minRate;
    }

    __device__ __forceinline__ bool can_still_reach(const DeviceSearchContext &ctx,
                                                    const int evenCycles,
                                                    const int depth) {
        const int gained = evenCycles - ctx.initialEvenCycles;
        const int movesLeft = ctx.maxDepth - depth;
        if (movesLeft < 0) return false;

        const int maxPossibleEvenCycles = ctx.n - evenCycles;

        if (ctx.fullSorting) {
            const int lowerBound = (maxPossibleEvenCycles + 1) / 2;
            if (depth + lowerBound > ctx.maxDepth) return false;

            const int optimisticGain = gained + 2 * movesLeft;
            if (static_cast<float>(optimisticGain) < ceilf(ctx.minRate * (depth + ceilf(maxPossibleEvenCycles / 2.0f)))) return false;
        } else {
            const int optimisticGain = gained + 2 * movesLeft;
            const int requiredGainAtMaxDepth = static_cast<int>(
                    floorf(ctx.minRate * static_cast<float>(ctx.maxDepth)));
            if (optimisticGain < requiredGainAtMaxDepth) return false;

            const int lowerBound = (maxPossibleEvenCycles + 1) / 2;
            const int bestCaseDepth = depth + lowerBound;
            if (bestCaseDepth > 0) {
                const float bestCaseRatio = (float)(gained + maxPossibleEvenCycles) / (float)bestCaseDepth;
                if (bestCaseRatio < ctx.minRate) return false;
            }
        }
        return true;
    }

    __device__ __forceinline__ void apply_move_pi_u8(const uint8_t *pi,
                                                     const int n,
                                                     const int i,
                                                     const int j,
                                                     const int k,
                                                     uint8_t *out) {
        for (int p = 0; p < n; ++p) {
            out[p] = pi[transposed_src_index(p, i, j, k)];
        }
    }

    // ─── Initialization kernel ───────────────────────────────────────────────────

    __global__ void initialize_queues_kernel(const Move *triples,
                                             const int tripleCount,
                                             BacktrackingQueue *queues,
                                             const int queueCount,
                                             BacktrackingNode *nodes,
                                             const int localCapacity,
                                             BacktrackingQueue *overflow,
                                             BacktrackingNode *overflowNodes,
                                             const int overflowCapacity,
                                             DedupEntry *dedupTable,
                                             const int dedupCapacity,
                                             int *overflowFlag,
                                             const short *spi0,
                                             const short *pi0,
                                             const int n,
                                             const int initialEvenCycles,
                                             const int reverseTripleOrder) {
        const int queueIndex = static_cast<int>(blockIdx.x * blockDim.x + threadIdx.x);
        if (queueIndex < queueCount) {
            BacktrackingQueue queue{};
            queue.head = 0;
            queue.lock = 0;
            queue.capacity = localCapacity;
            queue.nodes = nodes + static_cast<size_t>(queueIndex) * localCapacity;

            int rootCount = 0;
            if (queueIndex < tripleCount) {
                const int queueRoots = 1 + (tripleCount - 1 - queueIndex) / queueCount;
                if (queueRoots > localCapacity) {
                    atomicExch(overflowFlag, 1);
                }
                const int rootLimit = min(localCapacity, queueRoots);
                for (int rootIndex = 0; rootIndex < rootLimit; ++rootIndex) {
                    const int fwdTripleIndex = queueIndex + rootIndex * queueCount;
                    const int tripleIndex = reverseTripleOrder
                        ? (tripleCount - 1 - fwdTripleIndex)
                        : fwdTripleIndex;
                    const Move move = triples[tripleIndex];
                    BacktrackingNode node{};
                    node.depth = 1;
                    node.moves[0] = pack_move(move.firstIndex, move.secondIndex, move.thirdIndex);

                    const short a = pi0[move.firstIndex];
                    const short b = pi0[move.secondIndex];
                    const short c = pi0[move.thirdIndex];
                    for (int p = 0; p < n; ++p) {
                        node.spi[p] = static_cast<uint8_t>(spi0[p]);
                    }
                    node.spi[a] = static_cast<uint8_t>(spi0[c]);
                    node.spi[b] = static_cast<uint8_t>(spi0[a]);
                    node.spi[c] = static_cast<uint8_t>(spi0[b]);

                    node.evenCycles = static_cast<uint8_t>(count_even_cycles(node.spi, n));

                    if (!dedup_try_admit(dedupTable, dedupCapacity, node.spi, n, node.depth)) {
                        continue;
                    }

                    for (int p = 0; p < n; ++p) {
                        node.pi[p] = static_cast<uint8_t>(
                                pi0[transposed_src_index(p, move.firstIndex, move.secondIndex, move.thirdIndex)]);
                    }

                    queue.nodes[rootCount++] = node;
                }
            }

            queue.tail = (rootCount == localCapacity) ? 0 : rootCount;
            queue.size = rootCount;
            queues[queueIndex] = queue;
        }

        if (blockIdx.x == 0 && threadIdx.x == 0) {
            overflow->head = 0;
            overflow->tail = 0;
            overflow->size = 0;
            overflow->lock = 0;
            overflow->capacity = overflowCapacity;
            overflow->nodes = overflowNodes;
        }
    }

    // ─── Persistent search kernel ────────────────────────────────────────────────

    __global__ void sorting_search_persistent_kernel(const DeviceSearchContext ctx) {
        const int warpId = static_cast<int>(blockIdx.x);
        const int lane = static_cast<int>(threadIdx.x);

        uint8_t spi[MAX_N];
        uint8_t pi[MAX_N];

        while (true) {
            if (warp_should_stop(ctx)) return;

            BacktrackingNode node;
            bool hasNode = false;

            if (lane == 0) {
                hasNode = owner_pop(ctx.queues[warpId], node);
                if (!hasNode) {
                    for (int i = 1; i < ctx.queueCount; ++i) {
                        int victim = (warpId + i) % ctx.queueCount;
                        if (stealer_pop(ctx.queues[victim], node)) { hasNode = true; break; }
                    }
                }
                if (!hasNode) {
                    hasNode = stealer_pop(*ctx.overflow, node);
                }
            }

            int foundNode = __shfl_sync(0xFFFFFFFF, hasNode ? 1 : 0, 0);
            if (!foundNode) {
                int noWork = 0;
                if (lane == 0) {
                    bool anyWork = false;
                    for (int q = 0; q < ctx.queueCount; ++q) {
                        if (atomicAdd(&ctx.queues[q].size, 0) > 0) { anyWork = true; break; }
                    }
                    noWork = (!anyWork && atomicAdd(&ctx.overflow->size, 0) <= 0) ? 1 : 0;
                }
                noWork = __shfl_sync(0xFFFFFFFF, noWork, 0);
                if (noWork != 0) return;
                continue;
            }

            node.depth = (uint8_t)__shfl_sync(0xFFFFFFFF, node.depth, 0);
            node.evenCycles = (uint8_t)__shfl_sync(0xFFFFFFFF, static_cast<int>(node.evenCycles), 0);
            for (int d = 0; d < ctx.maxDepth; ++d) {
                node.moves[d] = static_cast<PackedMove>(__shfl_sync(0xFFFFFFFF, node.moves[d], 0));
            }
            for (int i = 0; i < ctx.n; ++i) {
                node.spi[i] = (uint8_t)__shfl_sync(0xFFFFFFFF, node.spi[i], 0);
                node.pi[i] = (uint8_t)__shfl_sync(0xFFFFFFFF, node.pi[i], 0);
            }
            if (node.depth == 0 || node.depth > ctx.maxDepth || node.depth > MAX_DEPTH) continue;

            for (int i = 0; i < ctx.n; ++i) {
                spi[i] = node.spi[i];
                pi[i] = node.pi[i];
            }
            const int parentEvenCycles = node.evenCycles;

            if (satisfies_rate(ctx, parentEvenCycles, node.depth)) {
                if (atomicCAS(ctx.foundFlag, 0, 1) == 0) {
                    *ctx.solutionDepth = node.depth;
                    for (int d = 0; d < node.depth; ++d) ctx.solutionMoves[d] = node.moves[d];
                }
                return;
            }

            if (node.depth >= ctx.maxDepth || !can_still_reach(ctx, parentEvenCycles, node.depth)) continue;

            const int childDepth = node.depth + 1;
            for (int tBase = 0; tBase < ctx.tripleCount; tBase += WARP_SIZE) {
                if (warp_should_stop(ctx)) return;

                const int t = ctx.reverseTripleOrder
                    ? (ctx.tripleCount - 1 - tBase - lane)
                    : (tBase + lane);
                bool foundChild = false;
                bool wantsPush = false;
                Move solutionMove{};
                BacktrackingNode child;

                if (t >= 0 && t < ctx.tripleCount) {
                    const Move move = ctx.triples[t];
                    const uint8_t a = pi[move.firstIndex];
                    const uint8_t b = pi[move.secondIndex];
                    const uint8_t c = pi[move.thirdIndex];

                    const uint8_t av = spi[a];
                    const uint8_t bv = spi[b];
                    const uint8_t cv = spi[c];

                    uint8_t childSpi[MAX_N];
                    for (int i = 0; i < ctx.n; ++i) childSpi[i] = spi[i];
                    childSpi[a] = cv;
                    childSpi[b] = av;
                    childSpi[c] = bv;

                    const int childEvenCycles = count_even_cycles(childSpi, ctx.n);

                    if (satisfies_rate(ctx, childEvenCycles, childDepth)) {
                        foundChild = true;
                        solutionMove = move;
                    } else if (childDepth < ctx.maxDepth && can_still_reach(ctx, childEvenCycles, childDepth)) {
                        if (dedup_try_admit(ctx.dedupTable, ctx.dedupCapacity, childSpi, ctx.n, childDepth)) {
                            child.depth = (uint8_t)childDepth;
                            for (int d = 0; d < node.depth; ++d) child.moves[d] = node.moves[d];
                            child.moves[node.depth] = pack_move(move.firstIndex, move.secondIndex, move.thirdIndex);

                            for (int i = 0; i < ctx.n; ++i) child.spi[i] = childSpi[i];
                            child.evenCycles = static_cast<uint8_t>(childEvenCycles);

                            uint8_t childPi[MAX_N];
                            apply_move_pi_u8(pi, ctx.n, move.firstIndex, move.secondIndex, move.thirdIndex, childPi);
                            for (int i = 0; i < ctx.n; ++i) child.pi[i] = childPi[i];

                            wantsPush = true;
                        }
                    }
                }

                const unsigned foundBallot = __ballot_sync(0xFFFFFFFF, foundChild ? 1 : 0);
                if (foundBallot != 0) {
                    const int solutionLane = __ffs(foundBallot) - 1;
                    if (lane == solutionLane && atomicCAS(ctx.foundFlag, 0, 1) == 0) {
                        *ctx.solutionDepth = childDepth;
                        for (int d = 0; d < node.depth; ++d) ctx.solutionMoves[d] = node.moves[d];
                        ctx.solutionMoves[node.depth] = pack_move(
                                solutionMove.firstIndex,
                                solutionMove.secondIndex,
                                solutionMove.thirdIndex);
                    }
                    return;
                }

                unsigned pushBallot = __ballot_sync(0xFFFFFFFF, wantsPush ? 1 : 0);
                while (pushBallot != 0) {
                    const int pushLane = __ffs(pushBallot) - 1;
                    if (lane == pushLane) {
                        bool pushed = owner_push(ctx.queues[warpId], child);
                        if (!pushed) {
                            for (int i = 1; i < ctx.queueCount; ++i) {
                                int receiver = (warpId + i) % ctx.queueCount;
                                if (donate_push(ctx.queues[receiver], child)) { pushed = true; break; }
                            }
                        }
                        if (!pushed) {
                            if (!donate_push(*ctx.overflow, child)) {
                                atomicExch(ctx.overflowFlag, 1);
                            }
                        }
                    }
                    pushBallot &= ~(1u << pushLane);
                }
            }
        }
    }

    int getQueueCount(const int device) {
        static std::mutex queueCountMutex;
        static std::vector<int> queueCounts;

        std::lock_guard<std::mutex> lock(queueCountMutex);
        if (static_cast<size_t>(device) >= queueCounts.size()) {
            queueCounts.resize(static_cast<size_t>(device) + 1, 0);
        }

        int &cached = queueCounts[static_cast<size_t>(device)];
        if (cached > 0) return cached;

        int multiprocessors = 0;
        CHECK_CUDA(cudaDeviceGetAttribute(&multiprocessors, cudaDevAttrMultiProcessorCount, device));
        int activeBlocksPerSM = 1;
        CHECK_CUDA(cudaOccupancyMaxActiveBlocksPerMultiprocessor(
                &activeBlocksPerSM, sorting_search_persistent_kernel, WARP_SIZE, 0));
        cudaDeviceProp props{};
        CHECK_CUDA(cudaGetDeviceProperties(&props, device));

        const int occupancyTarget = std::max(1, multiprocessors * std::max(1, activeBlocksPerSM));
        const int gridXLimit = std::max(1, props.maxGridSize[0]);
        cached = std::min(occupancyTarget, gridXLimit);
        return cached;
    }
}

void allocateGpuSortingSearchSlot(const int slot, const int device,
                                   const size_t queueBytesBudget, const int dedupTableSize) {
    std::lock_guard<std::mutex> lock(gSlotsMutex);
    if (static_cast<size_t>(slot) >= gSlots.size()) {
        gSlots.resize(static_cast<size_t>(slot) + 1);
    }

    auto &buffers = gSlots[slot];
    buffers.device = device;

    const int queueCount = getQueueCount(device);
    const size_t overflowBudget = queueBytesBudget / 4;
    const size_t localBudget = queueBytesBudget - overflowBudget;
    const int localCapacity = std::max(2, (int)(localBudget / (queueCount * sizeof(BacktrackingNode))));
    const int overflowCapacity = std::max(1024, (int)(overflowBudget / sizeof(BacktrackingNode)));

    buffers.queues = allocateDeviceBuffer<BacktrackingQueue>(
            static_cast<size_t>(queueCount), "sort.queues");
    buffers.overflow = allocateDeviceBuffer<BacktrackingQueue>(1, "sort.overflowQueue");
    buffers.nodes = allocateDeviceBuffer<BacktrackingNode>(
            static_cast<size_t>(queueCount) * static_cast<size_t>(localCapacity), "sort.nodes");
    buffers.overflowNodes = allocateDeviceBuffer<BacktrackingNode>(
            static_cast<size_t>(overflowCapacity), "sort.overflowNodes");
    if (dedupTableSize > 0) {
        buffers.dedupTable = allocateDeviceBuffer<DedupEntry>(
                static_cast<size_t>(dedupTableSize), "sort.dedupTable");
    }
    buffers.triples = allocateDeviceBuffer<Move>(
            static_cast<size_t>(combinations_of_three(MAX_N)), "sort.triples");
    buffers.inputVectors = allocateDeviceBuffer<short>(
            static_cast<size_t>(MAX_N) * 2, "sort.inputVectors");
    buffers.result = allocateDeviceBuffer<BacktrackingResult>(1, "sort.result");
    buffers.hostResult = makePinnedHostBuffer<BacktrackingResult>(1);

    buffers.queueCount = queueCount;
    buffers.localCapacity = localCapacity;
    buffers.overflowCapacity = overflowCapacity;
    buffers.dedupCapacity = dedupTableSize;
    buffers.cachedTriplesN = -1;
}

void destroyGpuSortingSearchSlots() {
    std::lock_guard<std::mutex> lock(gSlotsMutex);
    int originalDevice = -1;
    CHECK_CUDA(cudaGetDevice(&originalDevice));

    for (auto &buffers : gSlots) {
        if (buffers.device < 0) continue;
        if (originalDevice != buffers.device) CHECK_CUDA(cudaSetDevice(buffers.device));
        releaseSlotBuffers(buffers);
        buffers.device = -1;
    }

    gSlots.clear();
    if (originalDevice >= 0) CHECK_CUDA(cudaSetDevice(originalDevice));
}

std::vector<Move> performGpuSortingSearch(std::vector<short> pi,
                                          std::vector<short> spi,
                                          const int initialEvenCycles,
                                          const double minRate,
                                          const int maximumDepth,
                                          const bool fullSorting,
                                          const cudaStream_t stream,
                                          const int slot,
                                          const int maxWarps) {
    if (spi.size() != pi.size()) {
        throw std::invalid_argument("performGpuSortingSearch: inconsistent input sizes");
    }
    const int n = static_cast<int>(spi.size());
    if (n > MAX_N) throw std::invalid_argument("performGpuSortingSearch: n exceeds MAX_N");
    if (maximumDepth <= 0 || maximumDepth > MAX_DEPTH) throw std::invalid_argument("performGpuSortingSearch: invalid maximumDepth");
    clearGpuSortCancel(slot);

    const int tripleCount = combinations_of_three(n);
    if (tripleCount == 0) return {};

    int currentDevice = 0;
    CHECK_CUDA(cudaGetDevice(&currentDevice));

    int queueCount;
    int localCapacity;
    int overflowCapacity;
    int dedupCapacity;
    {
        std::lock_guard<std::mutex> lock(gSlotsMutex);
        if (slot < 0 || static_cast<size_t>(slot) >= gSlots.size()) {
            throw std::invalid_argument("invalid slot");
        }
        const auto &buffers = gSlots[slot];
        queueCount = buffers.queueCount;
        localCapacity = buffers.localCapacity;
        overflowCapacity = buffers.overflowCapacity;
        dedupCapacity = buffers.dedupCapacity;
    }
    if (maxWarps > 0 && maxWarps < queueCount) queueCount = maxWarps;
    queueCount = std::min(queueCount, tripleCount);

    const auto slotView = getSlotBufferView(slot, n);

    if (slotView.dedupTable != nullptr && dedupCapacity > 0) {
        CHECK_CUDA(cudaMemsetAsync(slotView.dedupTable, 0,
                                   sizeof(DedupEntry) * static_cast<size_t>(dedupCapacity), stream));
    }

    if (slotView.triplesNeedUpload) {
        auto triples = std::vector<Move>();
        triples.reserve(static_cast<size_t>(tripleCount));
        for (short i = 0; i < n - 2; ++i) {
            for (short j = i + 1; j < n - 1; ++j) {
                for (short k = j + 1; k < n; ++k) {
                    triples.push_back(Move{i, j, k});
                }
            }
        }
        CHECK_CUDA(cudaMemcpyAsync(slotView.triples, triples.data(),
                                   sizeof(Move) * triples.size(), cudaMemcpyHostToDevice, stream));
        markSlotTriplesUploaded(slot, currentDevice, n);
    }

    auto inputVectors = std::vector<short>(static_cast<size_t>(n) * 2);
    std::copy(pi.begin(), pi.end(), inputVectors.begin());
    std::copy(spi.begin(), spi.end(), inputVectors.begin() + n);
    CHECK_CUDA(cudaMemcpyAsync(slotView.inputVectors, inputVectors.data(),
                               sizeof(short) * inputVectors.size(), cudaMemcpyHostToDevice, stream));

    int *dCancel = getGpuSortCancelDevicePointer(slot);
    if (dCancel == nullptr) throw std::runtime_error("performGpuSortingSearch: missing slot cancel flag");

    BacktrackingResult initialResult{};
    initialResult.solutionDepth = -1;
    CHECK_CUDA(cudaMemcpyAsync(slotView.result, &initialResult, sizeof(BacktrackingResult),
                               cudaMemcpyHostToDevice, stream));
    CHECK_CUDA(cudaMemsetAsync(dCancel, 0, sizeof(int), stream));

    const short *dPi0 = slotView.inputVectors;
    const short *dSpi0 = slotView.inputVectors + n;
    auto *resultBytes = reinterpret_cast<char *>(slotView.result);
    auto *dFound = reinterpret_cast<int *>(resultBytes + offsetof(BacktrackingResult, found));
    auto *dSolutionDepth = reinterpret_cast<int *>(resultBytes + offsetof(BacktrackingResult, solutionDepth));
    auto *dOverflow = reinterpret_cast<int *>(resultBytes + offsetof(BacktrackingResult, overflowed));
    auto *dSolutionMoves = reinterpret_cast<PackedMove *>(resultBytes + offsetof(BacktrackingResult, solutionMoves));

    DeviceSearchContext host{};
    host.tripleCount = tripleCount;
    host.n = n;
    host.maxDepth = maximumDepth;
    host.queueCount = queueCount;
    host.initialEvenCycles = initialEvenCycles;
    host.minRate = static_cast<float>(minRate);
    host.fullSorting = fullSorting ? 1 : 0;

    constexpr int INIT_THREADS = 128;
    const int initBlocks = (queueCount + INIT_THREADS - 1) / INIT_THREADS;
    initialize_queues_kernel<<<initBlocks, INIT_THREADS, 0, stream>>>(
            slotView.triples, tripleCount,
            slotView.queues, queueCount,
            slotView.nodes, localCapacity,
            slotView.overflow, slotView.overflowNodes, overflowCapacity,
            slotView.dedupTable, dedupCapacity,
            dOverflow, dSpi0, dPi0, n, initialEvenCycles,
            (queueCount == 1) ? 1 : 0);
    CHECK_CUDA(cudaGetLastError());

    host.triples = slotView.triples;
    host.spi0 = dSpi0;
    host.pi0 = dPi0;
    host.queues = slotView.queues;
    host.overflow = slotView.overflow;
    host.foundFlag = dFound;
    host.solutionDepth = dSolutionDepth;
    host.solutionMoves = dSolutionMoves;
    host.cancelFlag = dCancel;
    host.overflowFlag = dOverflow;
    host.dedupTable = slotView.dedupTable;
    host.dedupCapacity = dedupCapacity;
    host.reverseTripleOrder = (queueCount == 1) ? 1 : 0;

    sorting_search_persistent_kernel<<<queueCount, WARP_SIZE, 0, stream>>>(host);
    CHECK_CUDA(cudaGetLastError());

    while (!isGpuSortCancelled(slot)) {
        const cudaError_t status = cudaStreamQuery(stream);
        if (status == cudaSuccess) break;
        if (status != cudaErrorNotReady) CHECK_CUDA(status);
    }

    CHECK_CUDA(cudaMemcpyAsync(slotView.hostResult, slotView.result,
                               sizeof(BacktrackingResult), cudaMemcpyDeviceToHost, stream));
    CHECK_CUDA(cudaStreamSynchronize(stream));

    const BacktrackingResult resultSnapshot = *slotView.hostResult;
    const int found = resultSnapshot.found;
    const int solutionDepth = resultSnapshot.solutionDepth;
    const int overflowed = resultSnapshot.overflowed;
    const bool cancelled = isGpuSortCancelled(slot);

    if (!found && cancelled) {
        throw GpuSortCancelledException("GPU sorting search cancelled");
    }
    if (!found && overflowed != 0) {
        throw GpuMemoryExhaustedException("GPU sorting search queue overflow");
    }
    if (!found || solutionDepth <= 0) return {};

    std::vector<Move> result;
    for (int d = 0; d < solutionDepth; ++d) {
        short i, j, k;
        unpack_move(resultSnapshot.solutionMoves[d], i, j, k);
        result.push_back(Move{i, j, k});
    }
    return result;
}
