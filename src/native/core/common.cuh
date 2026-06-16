#pragma once
#include <cstdint>

struct Move {
    short firstIndex;
    short secondIndex;
    short thirdIndex;
};

#ifndef MAX_DEPTH
#define MAX_DEPTH 15
#endif

#ifndef MAX_N
#define MAX_N 63
#endif

using PackedMove = uint32_t;

constexpr int PACKED_MOVE_INDEX_BITS = 6;
constexpr PackedMove PACKED_MOVE_INDEX_MASK = (1u << PACKED_MOVE_INDEX_BITS) - 1u;

static_assert(MAX_N <= 64, "PackedMove supports MAX_N up to 64");

__host__ __device__ __forceinline__ int combinations_of_three(int elementCount) {
    if (elementCount < 3) {
        return 0;
    }
    return (elementCount * (elementCount - 1) * (elementCount - 2)) / 6;
}

__host__ __device__ __forceinline__
PackedMove pack_move(short i, short j, short k) {
    return static_cast<PackedMove>((static_cast<PackedMove>(i) & PACKED_MOVE_INDEX_MASK)
                                   | ((static_cast<PackedMove>(j) & PACKED_MOVE_INDEX_MASK) << PACKED_MOVE_INDEX_BITS)
                                   | ((static_cast<PackedMove>(k) & PACKED_MOVE_INDEX_MASK) << (PACKED_MOVE_INDEX_BITS * 2)));
}

__host__ __device__ __forceinline__
void unpack_move(PackedMove m, short &i, short &j, short &k) {
    i = static_cast<short>(m & PACKED_MOVE_INDEX_MASK);
    j = static_cast<short>((m >> PACKED_MOVE_INDEX_BITS) & PACKED_MOVE_INDEX_MASK);
    k = static_cast<short>((m >> (PACKED_MOVE_INDEX_BITS * 2)) & PACKED_MOVE_INDEX_MASK);
}

template<class T>
__host__ __device__ __forceinline__ T dmin(T a, T b) { return a < b ? a : b; }
