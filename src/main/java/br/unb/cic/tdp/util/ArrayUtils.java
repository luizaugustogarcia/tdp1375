package br.unb.cic.tdp.util;

import lombok.val;

import java.util.List;

public final class ArrayUtils {

    public static int[] flatten(final List<int[]> list) {
        var totalLength = 0;
        for (final int[] arr : list) {
            totalLength += arr.length;
        }

        val result = new int[totalLength];
        var destPos = 0;

        for (val arr : list) {
            System.arraycopy(arr, 0, result, destPos, arr.length);
            destPos += arr.length;
        }

        return result;
    }
}
