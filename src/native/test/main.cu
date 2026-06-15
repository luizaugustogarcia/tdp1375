#include <cuda_runtime.h>

#include <chrono>
#include <cstdio>
#include <cstddef>
#include <exception>
#include <iostream>
#include <stdexcept>
#include <string>
#include <vector>

#include "core/cuda_error.cuh"
#include "core/cuda_stream.cuh"
#include "backtracking/slot_manager.cuh"
#include "backtracking/sorting_search.cuh"
#include "backtracking/transposition.cuh"

namespace {
    void verifySolution(const std::vector<Move> &moves) {
        std::vector<short> spi = {6, 0, 13, 1, 9, 3, 5, 4, 12, 7, 8, 2, 10, 11};
        std::vector<short> pi = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13};
        const int n = static_cast<int>(spi.size());

        for (const Move &move : moves) {
            const short a = pi[move.firstIndex];
            const short b = pi[move.secondIndex];
            const short c = pi[move.thirdIndex];
            const short av = spi[a];
            const short bv = spi[b];
            const short cv = spi[c];
            spi[a] = cv;
            spi[b] = av;
            spi[c] = bv;

            std::vector<short> newPi(n);
            for (int p = 0; p < n; ++p) {
                newPi[p] = pi[transposed_src_index(p, move.firstIndex, move.secondIndex, move.thirdIndex)];
            }
            pi = std::move(newPi);
        }

        int evenCycles = 0;
        std::vector<bool> visited(n, false);
        for (int s = 0; s < n; ++s) {
            if (visited[s]) continue;
            int size = 0, cur = s;
            do { visited[cur] = true; size++; cur = spi[cur]; } while (cur != s);
            if (size & 1) evenCycles++;
        }

        const double rate = static_cast<double>(evenCycles - 4) / static_cast<double>(moves.size());
        if (rate <= 1.375) {
            throw std::runtime_error("Rate verification FAILED");
        }
        if (moves.size() != 4) {
            throw std::runtime_error("Expected 4 moves but got " + std::to_string(moves.size()));
        }
    }
}

int main() {
    try {
        clearCudaRuntimePoisoned();
        initGpuSortSlots(1, 1, 300ULL * 1024 * 1024, 1 << 20);
        cudaStream_t rawStream = nullptr;
        CHECK_CUDA(cudaStreamCreateWithFlags(&rawStream, cudaStreamNonBlocking));
        StreamGuard streamGuard(rawStream);

        const auto startTime = std::chrono::high_resolution_clock::now();

        // spi for (0 6 5 3 1)(2 13 11)(4 9 7)(8 12 10)
        // Initial even cycles = 4
        std::vector<Move> moves = performGpuSortingSearch(
            {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13},
            {6, 0, 13, 1, 9, 3, 5, 4, 12, 7, 8, 2, 10, 11},
            4,
            1.375,
            7,
            false,
            rawStream,
            0,
            1);

        const auto endTime = std::chrono::high_resolution_clock::now();
        const double elapsedMs = std::chrono::duration<double, std::milli>(endTime - startTime).count();

        verifySolution(moves);

        // Convert positional indices to element values (matching CPU SortingSearch output)
        std::vector<short> currentPi = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13};
        std::cout << "Found " << moves.size() << " moves:" << std::endl;
        for (const auto &move : moves) {
            const int a = currentPi[move.firstIndex];
            const int b = currentPi[move.secondIndex];
            const int c = currentPi[move.thirdIndex];
            std::cout << "  (" << a << ", " << b << ", " << c << ")" << std::endl;
            std::vector<short> newPi(currentPi.size());
            for (int p = 0; p < static_cast<int>(currentPi.size()); ++p) {
                newPi[p] = currentPi[transposed_src_index(p, move.firstIndex, move.secondIndex, move.thirdIndex)];
            }
            currentPi = std::move(newPi);
        }
        std::printf("Search took %.3f ms\n", elapsedMs);

        destroyGpuSortSlots();
        return 0;
    } catch (const std::exception &e) {
        std::fprintf(stderr, "ERROR: %s\n", e.what());
        return 1;
    }
}
