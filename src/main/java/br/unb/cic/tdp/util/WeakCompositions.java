package br.unb.cic.tdp.util;

import lombok.val;

import java.util.ArrayList;
import java.util.List;

public final class WeakCompositions {

    public static List<int[]> weakCompositions(final int k, final int n) {
        if (k < 0 || n <= 0)
            throw new IllegalArgumentException("k ≥ 0 and n > 0 required");

        val result = new ArrayList<int[]>();
        val vector = new int[n];
        backtrack(0, k, n, vector, result);
        return result;
    }

    private static void backtrack(
            final int pos,
            final int remaining,
            final int n,
            final int[] vec,
            final List<int[]> acc
    ) {
        if (pos == n - 1) {
            vec[pos] = remaining;
            acc.add(vec.clone());
            return;
        }
        for (var v = 0; v <= remaining; v++) {
            vec[pos] = v;
            backtrack(pos + 1, remaining - v, n, vec, acc);
        }
    }
}
