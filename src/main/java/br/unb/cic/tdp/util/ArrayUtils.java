package br.unb.cic.tdp.util;

import java.util.List;

public final class ArrayUtils {

    public static int[] flatten(final List<int[]> list) {
        // First, compute the total size
        int totalLength = 0;
        for (final int[] arr : list) {
            totalLength += arr.length;
        }

        // Allocate result array
        final int[] result = new int[totalLength];
        int destPos = 0;

        // Copy each sub-array
        for (final int[] arr : list) {
            System.arraycopy(arr, 0, result, destPos, arr.length);
            destPos += arr.length;
        }

        return result;
    }
}
