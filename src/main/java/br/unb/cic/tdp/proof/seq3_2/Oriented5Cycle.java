package br.unb.cic.tdp.proof.seq3_2;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import com.google.common.primitives.Ints;
import org.apache.commons.math3.util.Pair;
import org.paukov.combinatorics.Factory;

import java.util.*;

import static br.unb.cic.tdp.base.CommonOperations.*;

public class Oriented5Cycle {

    public static void main(String[] args) {
        generate().forEach(p -> System.out.println(p.getFirst().toString() + "," + p.getSecond().toString()));
    }

    /**
     * Generate (3,2)-sequences to apply when there is a cycle in \spi with length
     * equals to 5 that doesn't allow the application of a 2-move.
     */
    public static List<Pair<Configuration, List<Cycle>>> generate() {
        final var orientedCycle = Cycle.create("(0,3,1,4,2)");
        final var triple = new int[]{0, 1, 2};

        final var result = new ArrayList<Pair<Configuration, List<Cycle>>>();

        final var verifiedConfigurations = new HashSet<Configuration>();

        final var spi = new MulticyclePermutation(orientedCycle);

        for (final var permutation : Factory.createPermutationGenerator(Factory.createVector(spi.getSymbols()))) {
            final var pi = Cycle.create(Ints.toArray(permutation.getVector()));
            final var config = new Configuration(spi, pi);

            if (areSymbolsInCyclicOrder(pi, triple)) {
                if (!verifiedConfigurations.contains(config)) {
                    verifiedConfigurations.add(config);

                    final var _2Move = searchFor2MoveFromOrientedCycle(spi, pi);
                    if (_2Move.isEmpty()) {
                        final var moves = searchForSortingSeq(pi, spi, new Stack<>(), 1, 1.5F);
                        assert !moves.isEmpty() : "ERROR";
                        result.add(new Pair<>(config, moves));
                    } else {
                        result.add(new Pair<>(config, Arrays.asList(_2Move.get())));
                    }
                }
            }
        }

        return result;
    }
}
