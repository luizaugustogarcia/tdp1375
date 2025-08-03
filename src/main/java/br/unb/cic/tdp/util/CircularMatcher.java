package br.unb.cic.tdp.util;

import br.unb.cic.tdp.permutation.Cycle;
import lombok.val;

public final class CircularMatcher {

    public static boolean isUnorderedSubsequence(final int[] s, final Cycle pi) {
        outer: for (var i = 0; i < pi.size(); i++) {
            val first = pi.get(i);
            if (contains(s, first)) {
                for (var j = 0; j < s.length; j++) {
                    if (!contains(s, pi.pow(first, j))) {
                        continue outer;
                    }
                }
                return true;
            }
        }
        return false;
    }

    private static boolean contains(final int[] array, final int s) {
        for (var i = 0; i < array.length; i++) {
            if (array[i] == s) {
                return true;
            }
        }
        return false;
    }
}
