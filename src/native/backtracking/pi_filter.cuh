#pragma once

#include <cstdint>
#include <vector>

struct PiFilterResult {
    std::vector<std::vector<uint8_t>> permutations;
};

PiFilterResult performGpuPiFilter(
    const uint8_t *cycle,
    int cycleLen,
    const uint8_t *orientedTriple,
    int tripleLen,
    int n,
    bool skip2MoveCheck
);
