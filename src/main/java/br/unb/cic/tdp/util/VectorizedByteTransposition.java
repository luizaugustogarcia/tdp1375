package br.unb.cic.tdp.util;

import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.VectorSpecies;
import lombok.val;

public class VectorizedByteTransposition {

    private static final VectorSpecies<Byte> SPECIES = ByteVector.SPECIES_128;

    public static byte[] applyTransposition(final byte[] pi, final byte i, final byte j, final byte k) {
        val clone = pi.clone();  // Snapshot to allow safe overwrite

        val len1 = j - i;
        val len2 = k - j;

        vectorCopy(pi, j, clone, i, len2);               // Move second block
        vectorCopy(pi, i, clone, i + len2, len1);        // Move first block

        return clone;
    }

    private static void vectorCopy(final byte[] src, final int srcPos, final byte[] dst, final int dstPos, final int length) {
        var i = 0;
        val upperBound = SPECIES.loopBound(length);

        while (i < upperBound) {
            val v = ByteVector.fromArray(SPECIES, src, srcPos + i);
            v.intoArray(dst, dstPos + i);
            i += SPECIES.length();
        }
        for (; i < length; i++) {
            dst[dstPos + i] = src[srcPos + i];
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
