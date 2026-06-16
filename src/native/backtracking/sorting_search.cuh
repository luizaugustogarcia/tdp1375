#pragma once

#include <cstddef>
#include <vector>

#include <cuda_runtime.h>

#include "core/common.cuh"

void allocateGpuSortingSearchSlot(int slot, int device, size_t queueBytesBudget, int dedupTableSize);

std::vector<Move> performGpuSortingSearch(std::vector<short> pi,
                                          std::vector<short> spi,
                                          int initialEvenCycles,
                                          double minRate,
                                          int maximumDepth,
                                          bool fullSorting,
                                          cudaStream_t stream,
                                          int slot,
                                          int maxWarps = -1);

void destroyGpuSortingSearchSlots();
