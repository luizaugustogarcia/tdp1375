package br.unb.cic.tdp.util;

import java.util.Arrays;

public class ByteArrayOperations {

    public static byte[] replace(final byte[] array, final byte a, final byte b) {
        final var replaced = Arrays.copyOf(array, array.length);
        for (var i = 0; i < replaced.length; i++) {
            if (replaced[i] == a)
                replaced[i] = b;
        }
        return replaced;
    }

    public static byte[] replace(final byte[] array, final byte[] substitutionMatrix) {
        for (var i = 0; i < array.length; i++) {
            array[i] = substitutionMatrix[array[i]];
        }
        return array;
    }
}
