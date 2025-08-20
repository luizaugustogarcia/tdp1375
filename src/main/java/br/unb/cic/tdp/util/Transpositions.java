package br.unb.cic.tdp.util;

import lombok.val;

public class Transpositions {

    public static short[] apply(final short[] pi, final byte i, final byte j, final byte k) {
        val clone = pi.clone();  // Snapshot to allow safe overwrite

        val len1 = j - i;
        val len2 = k - j;

        vectorCopy(pi, j, clone, i, len2);               // Move second block
        vectorCopy(pi, i, clone, i + len2, len1);        // Move first block

        return clone;
    }

    public static void vectorCopy(final short[] src, final int srcPos,
                                  final short[] dst, final int dstPos,
                                  final int length) {
        if (length <= 0) return;

        // One-shot bounds checks so the JIT can hoist them
        java.util.Objects.checkFromIndexSize(srcPos, length, src.length);
        java.util.Objects.checkFromIndexSize(dstPos, length, dst.length);

        // Tunable threshold: sized to cover your "≈20, sometimes a bit more" case
        // Raise/lower after JMH: 24–40 are typical crossover points.
        final int TINY_MAX = 32;
        if (length >= TINY_MAX) {
            System.arraycopy(src, srcPos, dst, dstPos, length);
            return;
        }

        int s = srcPos, d = dstPos, n = length;

        // Copy 8, then 4, then 2, then 1 — minimal branches for n ≤ 31
        while (n >= 8) {
            dst[d] = src[s];
            dst[d + 1] = src[s + 1];
            dst[d + 2] = src[s + 2];
            dst[d + 3] = src[s + 3];
            dst[d + 4] = src[s + 4];
            dst[d + 5] = src[s + 5];
            dst[d + 6] = src[s + 6];
            dst[d + 7] = src[s + 7];
            d += 8;
            s += 8;
            n -= 8;
        }
        if (n >= 4) {
            dst[d] = src[s];
            dst[d + 1] = src[s + 1];
            dst[d + 2] = src[s + 2];
            dst[d + 3] = src[s + 3];
            d += 4;
            s += 4;
            n -= 4;
        }
        if (n >= 2) {
            dst[d] = src[s];
            dst[d + 1] = src[s + 1];
            d += 2;
            s += 2;
            n -= 2;
        }
        if (n == 1) {
            dst[d] = src[s];
        }
    }


    public static byte[] toByteArray(int[] input) {
        byte[] result = new byte[input.length];
        for (int i = 0; i < input.length; i++) {
            result[i] = (byte) input[i]; // truncates to lower 8 bits
        }
        return result;
    }
}
