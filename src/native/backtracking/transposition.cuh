#pragma once

#include "core/common.cuh"

__host__ __device__ inline int transposed_src_index(const int p, const int i, const int j, const int k) {
    const int lenA = j - i;
    const int lenB = k - j;
    const int inLow  = (p >= i) & (p < i + lenB);
    const int inHigh = (p >= i + lenB) & (p < k);
    return p + inLow * lenA - inHigh * lenB;
}

__host__ __device__
inline void apply_move_pi(const short *pi, const int n, const int i, const int j, const int k, short *out) {
    for (int p = 0; p < n; ++p) {
        const int s = transposed_src_index(p, i, j, k);
        out[p] = pi[s];
    }
}
