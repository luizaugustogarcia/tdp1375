package br.unb.cic.tdp.permutation;

import java.util.Arrays;

public class RingKey {
    private final int[] canon;

    RingKey(int[] a) {
        canon = canonicalRotation(a);
    }

    // Booth’s algorithm – O(n)
    private static int[] canonicalRotation(final int[] a) {
        int n = a.length;
        if (n == 0) return a.clone();
        int i = 0, j = 1, k = 0;
        while (i < n && j < n && k < n) {
            int ai = a[(i + k) % n], aj = a[(j + k) % n];
            if (ai == aj) {
                k++;
                continue;
            }
            if (ai < aj) j += k + 1;
            else i += k + 1;
            if (i == j) ++j;
            k = 0;
        }
        int start = Math.min(i, j) % n;
        int[] out = new int[n];
        for (int t = 0; t < n; ++t) out[t] = a[(start + t) % n];
        return out;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof RingKey rk && Arrays.equals(canon, rk.canon);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(canon);
    }
}