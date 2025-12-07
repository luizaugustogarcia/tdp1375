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
        var composed = new int[n];
        val mapping = new int[n];

        for (var i = 0; i < n; i++) {
            composed[i] = i;
        }

        for (var idx = permutations.length - 1; idx >= 0; idx--) {
            Arrays.fill(mapping, -1);

            if (permutations[idx] instanceof Cycle) {
                val cycle = (Cycle) permutations[idx];
                for (var j = 0; j < cycle.size(); j++) {
                    mapping[cycle.get(j)] = cycle.image(cycle.get(j));
                }
            } else {
                for (val cycle : ((MulticyclePermutation) permutations[idx])) {
                    for (var j = 0; j < cycle.size(); j++) {
                        mapping[cycle.get(j)] = cycle.image(cycle.get(j));
                    }
                }
            }

            for (var start = 0; start < n; start++) {
                val current = composed[start];
                val image = mapping[current];
                composed[start] = image == -1 ? current : image;
            }
        }

        val result = new MulticyclePermutation();
        val seen = new BitArray(n);
        val cycle = new IntArrayList();

        for (var start = 0; start < n; start++) {
            if (seen.get(start)) {
                continue;
            }

            if (composed[start] == start) {
                seen.set(start);
                if (include1Cycle) {
                    result.add(Cycle.of(start));
                }
                continue;
            }

            var current = start;
            while (!seen.get(current)) {
                seen.set(current);
                cycle.add(current);
                current = composed[current];
            }

            result.add(Cycle.of(Arrays.copyOfRange(cycle.elements(), 0, cycle.size())));
            cycle.clear();
        }

        return result;
    }
}
