package br.unb.cic.tdp.permutation;

import cc.redberry.core.utils.BitArray;
import cern.colt.list.IntArrayList;
import lombok.val;

import java.io.Serializable;
import java.util.Arrays;

public class PermutationGroups implements Serializable {

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
        val functions = new int[permutations.length][n];

        // initializing
        for (var i = 0; i < permutations.length; i++)
            Arrays.fill(functions[i], -1);

        for (var i = 0; i < permutations.length; i++) {
            if (permutations[i] instanceof Cycle) {
                val cycle = (Cycle) permutations[i];
                for (var j = 0; j < cycle.size(); j++) {
                    functions[i][cycle.get(j)] = cycle.image(cycle.get(j));
                }
            } else {
                for (val cycle : ((MulticyclePermutation) permutations[i])) {
                    for (var j = 0; j < cycle.size(); j++) {
                        functions[i][cycle.get(j)] = cycle.image(cycle.get(j));
                    }
                }
            }
        }

        val result = new MulticyclePermutation();

        val cycle = new IntArrayList();
        val seen = new BitArray(n);
        var counter = 0;
        while (counter < n) {
            var start = seen.nextZeroBit(0);

            var image = start;
            for (var i = functions.length - 1; i >= 0; i--) {
                image = functions[i][image] == -1 ? image : functions[i][image];
            }

            if (image == start) {
                ++counter;
                seen.set(start);
                if (include1Cycle)
                    result.add(Cycle.of(start));
                continue;
            }
            while (!seen.get(start)) {
                seen.set(start);
                ++counter;
                cycle.add(start);

                image = start;
                for (var i = functions.length - 1; i >= 0; i--) {
                    image = functions[i][image] == -1 ? image : functions[i][image];
                }

                start = image;
            }

            result.add(Cycle.of(Arrays.copyOfRange(cycle.elements(), 0, cycle.size())));
            cycle.clear();
        }
        
        return result;
    }
}
