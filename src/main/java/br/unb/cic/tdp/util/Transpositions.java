package br.unb.cic.tdp.util;

import lombok.val;

import java.util.Arrays;

public class Transpositions {

    public static short[] apply(final short[] pi, final byte i, final byte j, final byte k) {
        val result = Arrays.copyOf(pi, pi.length);

        System.arraycopy(pi, j, result, i, k - j);
        System.arraycopy(pi, i, result, i + (k - j), j - i);

        return result;
    }
}
