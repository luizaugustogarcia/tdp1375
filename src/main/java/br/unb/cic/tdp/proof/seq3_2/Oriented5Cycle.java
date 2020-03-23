package br.unb.cic.tdp.proof.seq3_2;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import com.google.common.primitives.Bytes;
import org.apache.commons.math3.util.Pair;
import org.paukov.combinatorics.Factory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import static br.unb.cic.tdp.base.CommonOperations.searchFor2Move;
import static br.unb.cic.tdp.base.CommonOperations.searchForSortingSeq;

public class Oriented5Cycle {

    /**
     * Generate (3,2)-sequences to apply when there is a cycle in \spi with length
     * equals to 5 that doesn't allow the application of a 2-move.
     */
    public static List<Pair<Configuration, List<Cycle>>> generate() {
        final var orientedCycle = new Cycle("(0,3,1,4,2)");
        final var triple = new byte[]{0, 1, 2};

        final var result = new ArrayList<Pair<Configuration, List<Cycle>>>();

        final var verifiedConfigurations = new HashSet<Configuration>();

        final var spi = new MulticyclePermutation(orientedCycle);

        for (final var permutation : Factory.createPermutationGenerator(Factory.createVector(spi.getSymbols()))) {
            final var pi = new Cycle(Bytes.toArray(permutation.getVector()));

            // It is the case to avoid combinations originating 2-moves because the symbols
            // in the only cycle of spi are indeed the symbols of the actual 5-cycle
            if (pi.isOriented(triple) && searchFor2Move(spi, pi) == null) {
                final var rhos = searchForSortingSeq(pi, spi, new Stack<>(), spi.getNumberOfEvenCycles(),
                        1.5F);

                if (!rhos.isEmpty()) {
                    if (rhos.size() > 1) {
                        final var config = new Configuration(spi, pi);
                        if (!verifiedConfigurations.contains(config)) {
                            result.add(new Pair<>(config, rhos));
                            verifiedConfigurations.add(config);
                        }
                    }
                } else {
                    throw new RuntimeException("ERROR");
                }
            }
        }

        return result;
    }
}
