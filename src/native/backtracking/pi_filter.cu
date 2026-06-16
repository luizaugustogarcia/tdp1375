#include "backtracking/pi_filter.cuh"

#include <algorithm>
#include <cstddef>
#include <cstdint>
#include <vector>

#include <cuda_runtime.h>

#include "core/cuda_error.cuh"
#include "core/cuda_memory.cuh"

namespace {

    constexpr int PI_FILTER_MAX_N = 63;
    constexpr int MAX_OUTPUT = 1024 * 1024;
    constexpr int BATCH_SIZE = 256 * 1024 * 1024;

    struct PiFilterContext {
        const uint8_t *cycle;
        const uint8_t *cycleIndexes;
        int cycleLen;
        const uint8_t *orientedTriple;
        int tripleLen;
        int n;
        const uint64_t *factorials;

        uint8_t *outputBuffer;
        int *outputCount;
        int maxOutput;

        uint64_t batchOffset;
        uint64_t batchSize;

        bool skip2MoveCheck;
    };

    __device__ void factoradic_unrank(uint64_t index, uint8_t *perm, int n, const uint64_t *factorials) {
        // pi[0] = 0 is always fixed; enumerate permutations of {1..n-1} in positions 1..n-1
        perm[0] = 0;
        uint8_t available[PI_FILTER_MAX_N];
        for (int i = 0; i < n - 1; ++i) available[i] = (uint8_t)(i + 1);

        for (int pos = 0; pos < n - 1; ++pos) {
            uint64_t fact = factorials[n - 2 - pos];
            int choice = (int)(index / fact);
            index %= fact;
            perm[pos + 1] = available[choice];
            for (int j = choice; j < n - 2 - pos; ++j) {
                available[j] = available[j + 1];
            }
        }
    }

    __device__ __forceinline__ bool symbols_in_cyclic_order_3(const uint8_t *symbolIndexes, int a, int b, int c) {
        int ia = symbolIndexes[a];
        int ib = symbolIndexes[b];
        int ic = symbolIndexes[c];
        int leaps = 0;
        if (ia > ib) leaps++;
        if (ib > ic) leaps++;
        if (ic > ia) leaps++;
        return leaps <= 1;
    }

    __device__ __forceinline__ bool symbols_in_cyclic_order(const uint8_t *symbolIndexes, const uint8_t *symbols, int count) {
        int leaps = 0;
        for (int i = 0; i < count; ++i) {
            int cur = symbolIndexes[symbols[i]];
            int next = symbolIndexes[symbols[(i + 1) % count]];
            if (cur > next) {
                leaps++;
                if (leaps > 1) return false;
            }
        }
        return true;
    }

    __device__ __forceinline__ int cycle_distance(const uint8_t *cycleIndexes, int cycleLen, int a, int b) {
        int ai = cycleIndexes[a];
        int bi = cycleIndexes[b];
        if (bi >= ai) return bi - ai;
        return (cycleLen - ai) + bi;
    }

    __device__ bool has_2_move(const uint8_t *piSymbolIndexes,
                               const uint8_t *cycle, const uint8_t *cycleIndexes, int cycleLen) {
        // before = cycle.isEven() ? 1 : 0
        // A cycle is "even" (in the Java code) when its size is odd
        int before = (cycleLen % 2 == 1) ? 1 : 0;

        for (int i = 0; i < cycleLen - 2; ++i) {
            for (int j = i + 1; j < cycleLen - 1; ++j) {
                for (int k = j + 1; k < cycleLen; ++k) {
                    int a = cycle[i];
                    int b = cycle[j];
                    int c = cycle[k];

                    if (symbols_in_cyclic_order_3(piSymbolIndexes, a, b, c)) {
                        int kab = cycle_distance(cycleIndexes, cycleLen, a, b);
                        int kbc = cycle_distance(cycleIndexes, cycleLen, b, c);
                        int kca = cycle_distance(cycleIndexes, cycleLen, c, a);

                        int after = 0;
                        if (kab % 2 == 1) after++;
                        if (kbc % 2 == 1) after++;
                        if (kca % 2 == 1) after++;

                        if (after - before == 2) return true;
                    }
                }
            }
        }
        return false;
    }

    __global__ void pi_filter_kernel(const PiFilterContext ctx) {
        uint64_t gid = (uint64_t)blockIdx.x * blockDim.x + threadIdx.x;
        if (gid >= ctx.batchSize) return;

        uint64_t permIndex = ctx.batchOffset + gid;

        uint8_t pi[PI_FILTER_MAX_N];
        factoradic_unrank(permIndex, pi, ctx.n, ctx.factorials);

        // Build symbolIndexes for pi (position of each symbol)
        uint8_t piSymbolIndexes[PI_FILTER_MAX_N];
        for (int i = 0; i < ctx.n; ++i) {
            piSymbolIndexes[pi[i]] = (uint8_t)i;
        }

        // Check oriented triple is in cyclic order in pi
        if (!symbols_in_cyclic_order(piSymbolIndexes, ctx.orientedTriple, ctx.tripleLen)) return;

        // piInv = reverse of pi array, so position of symbol s in piInv = (n-1) - position of s in pi
        uint8_t piInvSymbolIndexes[PI_FILTER_MAX_N];
        for (int i = 0; i < ctx.n; ++i) {
            piInvSymbolIndexes[i] = (uint8_t)((ctx.n - 1) - piSymbolIndexes[i]);
        }

        // Check isOriented: cycle symbols must NOT be in cyclic order in pi^{-1}
        if (symbols_in_cyclic_order(piInvSymbolIndexes, ctx.cycle, ctx.cycleLen)) return;

        // Check no 2-move exists
        if (!ctx.skip2MoveCheck && has_2_move(piSymbolIndexes, ctx.cycle, ctx.cycleIndexes, ctx.cycleLen)) return;

        // Pi passes all filters — output
        int outIdx = atomicAdd(ctx.outputCount, 1);
        if (outIdx < ctx.maxOutput) {
            for (int i = 0; i < ctx.n; ++i) {
                ctx.outputBuffer[outIdx * ctx.n + i] = pi[i];
            }
        }
    }
}

PiFilterResult performGpuPiFilter(
    const uint8_t *cycle,
    int cycleLen,
    const uint8_t *orientedTriple,
    int tripleLen,
    int n,
    bool skip2MoveCheck
) {
    if (n > PI_FILTER_MAX_N) throw std::invalid_argument("n exceeds PI_FILTER_MAX_N");
    if (n < 1) throw std::invalid_argument("n must be at least 1");

    uint64_t totalPerms = 1;
    for (int i = 2; i <= n - 1; ++i) totalPerms *= i;

    // Allocate device memory
    uint8_t *dCycle = nullptr;
    uint8_t *dCycleIndexes = nullptr;
    uint8_t *dTriple = nullptr;
    uint8_t *dOutput = nullptr;
    int *dOutputCount = nullptr;
    uint64_t *dFactorials = nullptr;

    CHECK_CUDA(cudaMalloc(&dCycle, cycleLen));
    CHECK_CUDA(cudaMalloc(&dCycleIndexes, n));
    CHECK_CUDA(cudaMalloc(&dTriple, tripleLen));
    CHECK_CUDA(cudaMalloc(&dOutput, (size_t)MAX_OUTPUT * n));
    CHECK_CUDA(cudaMalloc(&dOutputCount, sizeof(int)));
    CHECK_CUDA(cudaMalloc(&dFactorials, (size_t)n * sizeof(uint64_t)));
    CHECK_CUDA(cudaMemset(dOutputCount, 0, sizeof(int)));

    std::vector<uint8_t> hostCycleIndexes(n, 0);
    for (int i = 0; i < cycleLen; ++i) {
        hostCycleIndexes[cycle[i]] = static_cast<uint8_t>(i);
    }

    std::vector<uint64_t> hostFactorials(n, 1);
    for (int i = 1; i < n; ++i) {
        hostFactorials[i] = hostFactorials[i - 1] * static_cast<uint64_t>(i);
    }

    CHECK_CUDA(cudaMemcpy(dCycle, cycle, cycleLen, cudaMemcpyHostToDevice));
    CHECK_CUDA(cudaMemcpy(dCycleIndexes, hostCycleIndexes.data(), n, cudaMemcpyHostToDevice));
    CHECK_CUDA(cudaMemcpy(dTriple, orientedTriple, tripleLen, cudaMemcpyHostToDevice));
    CHECK_CUDA(cudaMemcpy(dFactorials, hostFactorials.data(), (size_t)n * sizeof(uint64_t), cudaMemcpyHostToDevice));

    // Launch in batches
    constexpr int BLOCK_SIZE = 256;
    uint64_t batchSize = BATCH_SIZE;

    for (uint64_t offset = 0; offset < totalPerms; offset += batchSize) {
        uint64_t thisBatch = std::min(batchSize, totalPerms - offset);

        PiFilterContext ctx{};
        ctx.cycle = dCycle;
        ctx.cycleIndexes = dCycleIndexes;
        ctx.cycleLen = cycleLen;
        ctx.orientedTriple = dTriple;
        ctx.tripleLen = tripleLen;
        ctx.n = n;
        ctx.factorials = dFactorials;
        ctx.outputBuffer = dOutput;
        ctx.outputCount = dOutputCount;
        ctx.maxOutput = MAX_OUTPUT;
        ctx.batchOffset = offset;
        ctx.batchSize = thisBatch;
        ctx.skip2MoveCheck = skip2MoveCheck;

        int gridSize = (int)((thisBatch + BLOCK_SIZE - 1) / BLOCK_SIZE);
        pi_filter_kernel<<<gridSize, BLOCK_SIZE>>>(ctx);
        CHECK_CUDA(cudaGetLastError());
    }
    CHECK_CUDA(cudaDeviceSynchronize());

    // Read results
    int hostCount = 0;
    CHECK_CUDA(cudaMemcpy(&hostCount, dOutputCount, sizeof(int), cudaMemcpyDeviceToHost));
    if (hostCount > MAX_OUTPUT) hostCount = MAX_OUTPUT;

    std::vector<uint8_t> hostOutput((size_t)hostCount * n);
    if (hostCount > 0) {
        CHECK_CUDA(cudaMemcpy(hostOutput.data(), dOutput, (size_t)hostCount * n, cudaMemcpyDeviceToHost));
    }

    // Free device memory
    cudaFree(dCycle);
    cudaFree(dCycleIndexes);
    cudaFree(dTriple);
    cudaFree(dOutput);
    cudaFree(dOutputCount);
    cudaFree(dFactorials);

    // Build result
    PiFilterResult result;
    result.permutations.reserve(hostCount);
    for (int i = 0; i < hostCount; ++i) {
        std::vector<uint8_t> perm(n);
        for (int j = 0; j < n; ++j) {
            perm[j] = hostOutput[(size_t)i * n + j];
        }
        result.permutations.push_back(std::move(perm));
    }

    return result;
}
