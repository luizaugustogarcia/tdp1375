package br.unb.cic.tdp.permutation;

import cc.redberry.core.utils.BitArray;
import cern.colt.list.IntArrayList;
import lombok.val;

import java.io.Serializable;
import java.util.Arrays;

public class PermutationGroups implements Serializable {

    public static MulticyclePermutation computeProduct(final Permutation... permutations) {
        return computeProduct(true, permutations);
    }

    public static MulticyclePermutation computeProduct(final boolean include1Cycle, final Permutation... p) {
        var n = 0;
        for (val p1 : p) {
            if (p1 instanceof Cycle) {
                n = Math.max(p1.getMaxSymbol(), n);
            } else {
                for (val c : ((MulticyclePermutation) p1)) {
                    n = Math.max(c.getMaxSymbol(), n);
                }
            }
        }
        return computeProduct(include1Cycle, n + 1, p);
    }

    public static MulticyclePermutation computeProduct(final boolean include1Cycle, final int n, final Permutation... permutations) {
        val composed = new int[n];

        for (var i = 0; i < n; i++) {
            composed[i] = i;
        }

        val mapping = new int[n];
        val mappingSet = new boolean[n];

        for (var idx = permutations.length - 1; idx >= 0; idx--) {
            val perm = permutations[idx];

            if (perm instanceof Cycle) {
                val cycle = (Cycle) perm;
                val symbols = cycle.getSymbols();
                val size = symbols.length;

                for (var j = 0; j < size; j++) {
                    val sym = symbols[j];
                    val nextSym = symbols[(j + 1) % size];
                    mapping[sym] = nextSym;
                    mappingSet[sym] = true;
                }

                for (var start = 0; start < n; start++) {
                    val current = composed[start];
                    if (mappingSet[current]) {
                        composed[start] = mapping[current];
                    }
                }

                for (var j = 0; j < size; j++) {
                    mappingSet[symbols[j]] = false;
                }
            } else {
                val multicycle = (MulticyclePermutation) perm;
                for (val cycle : multicycle) {
                    val symbols = cycle.getSymbols();
                    val size = symbols.length;

                    for (var j = 0; j < size; j++) {
                        val sym = symbols[j];
                        val nextSym = symbols[(j + 1) % size];
                        mapping[sym] = nextSym;
                        mappingSet[sym] = true;
                    }
                }

                for (var start = 0; start < n; start++) {
                    val current = composed[start];
                    if (mappingSet[current]) {
                        composed[start] = mapping[current];
                    }
                }

                for (val cycle : multicycle) {
                    val symbols = cycle.getSymbols();
                    for (var j = 0; j < symbols.length; j++) {
                        mappingSet[symbols[j]] = false;
                    }
                }
            }
        }

        val result = new MulticyclePermutation();
        val seen = new BitArray(n);
        val cycleBuffer = new int[n];

        for (var start = 0; start < n; start++) {
            if (seen.get(start)) {
                continue;
            }

            val target = composed[start];
            if (target == start) {
                seen.set(start);
                if (include1Cycle) {
                    result.add(Cycle.of(start));
                }
                continue;
            }

            var cycleLen = 0;
            var current = start;
            do {
                seen.set(current);
                cycleBuffer[cycleLen++] = current;
                current = composed[current];
            } while (current != start);

            result.add(Cycle.of(Arrays.copyOf(cycleBuffer, cycleLen)));
        }

        return result;
    }
}
