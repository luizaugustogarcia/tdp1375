package br.unb.cic.tdp.util;

import cern.colt.list.ByteArrayList;

import java.util.Arrays;

public class ByteArrayOperations {

    public static byte[] removeSymbol(final byte[] array, final byte symbol) {
        final var temp = new ByteArrayList(Arrays.copyOf(array, array.length));
        temp.remove(temp.indexOf(symbol));
        return Arrays.copyOfRange(temp.elements(), 0, temp.size());
    }

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

    public static int compare(final byte[] a, final byte[] b) {
        for (int i = 0; i < a.length; i++) {
            if (a[i] > b[i]) {
                return 1;
            } else if (a[i] < b[i]) {
                return -1;
            }
        }

        return 0;
    }
}
