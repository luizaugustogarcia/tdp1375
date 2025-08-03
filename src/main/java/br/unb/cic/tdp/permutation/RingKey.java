package br.unb.cic.tdp.permutation;

import lombok.val;

import java.util.Arrays;

public class RingKey {
    private final int[] canon;

    RingKey(final int[] a) {
        canon = canonicalRotation(a);
    }

    public static int[] canonicalRotation(final int[] a) {
        val n = a.length;
        if (n == 0) return a.clone();
        var i = 0;
        var j = 1;
        var k = 0;
        while (i < n && j < n && k < n) {
            val ai = a[(i + k) % n];
            val aj = a[(j + k) % n];
            if (ai == aj) {
                k++;
                continue;
            }
            if (ai < aj) j += k + 1;
            else i += k + 1;
            if (i == j) ++j;
            k = 0;
        }
        val start = Math.min(i, j) % n;
        val out = new int[n];
        for (var t = 0; t < n; ++t) out[t] = a[(start + t) % n];
        return out;
    }

    @Override
    public boolean equals(final Object o) {
        return o instanceof RingKey rk && Arrays.equals(canon, rk.canon);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(canon);
    }
}