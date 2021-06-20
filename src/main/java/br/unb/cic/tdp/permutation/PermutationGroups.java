package br.unb.cic.tdp.permutation;

import br.unb.cic.tdp.base.CommonOperations;
import cc.redberry.core.utils.BitArray;
import cern.colt.list.ByteArrayList;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;

public class PermutationGroups implements Serializable {

    public static MulticyclePermutation computeProduct(final Collection<Permutation> permutations) {
        return computeProduct(true, permutations.toArray(new Permutation[permutations.size()]));
    }

    public static MulticyclePermutation computeProduct(final Permutation... permutations) {
        return computeProduct(true, permutations);
    }

    public static MulticyclePermutation computeProduct(final boolean include1Cycle, final Permutation... p) {
        var n = 0;
        for (final var p1 : p) {
            if (p1 instanceof Cycle) {
                n = Math.max(((Cycle) p1).getMaxSymbol(), n);
            } else {
                for (final var c : ((MulticyclePermutation) p1)) {
                    n = Math.max(c.getMaxSymbol(), n);
                }
            }
        }
        return computeProduct(include1Cycle, n + 1, p);
    }

    public static MulticyclePermutation computeProduct(final boolean include1Cycle, final int n, final Permutation... permutations) {
        final var functions = new byte[permutations.length][n];

        // initializing
        for (var i = 0; i < permutations.length; i++)
            Arrays.fill(functions[i], (byte) -1);

        for (var i = 0; i < permutations.length; i++) {
            if (permutations[i] instanceof Cycle) {
                final var cycle = (Cycle) permutations[i];
                for (var j = 0; j < cycle.size(); j++) {
                    functions[i][cycle.get(j)] = cycle.image(cycle.get(j));
                }
            } else {
                for (final var cycle : ((MulticyclePermutation) permutations[i])) {
                    for (var j = 0; j < cycle.size(); j++) {
                        functions[i][cycle.get(j)] = cycle.image(cycle.get(j));
                    }
                }
            }
        }

        final var result = new MulticyclePermutation();

        final var cycle = new ByteArrayList();
        final var seen = new BitArray(n);
        var counter = 0;
        while (counter < n) {
            var start = (byte) seen.nextZeroBit(0);

            var image = start;
            for (var i = functions.length - 1; i >= 0; i--) {
                image = functions[i][image] == -1 ? image : functions[i][image];
            }

            if (image == start) {
                ++counter;
                seen.set(start);
                if (include1Cycle)
                    result.add(new Cycle(start));
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

            result.add(new Cycle(Arrays.copyOfRange(cycle.elements(), 0, cycle.size())));
            cycle.clear();
        }

        return result;
    }
}
