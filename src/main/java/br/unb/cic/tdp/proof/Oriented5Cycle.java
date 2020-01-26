package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.CommonOperations;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import com.google.common.primitives.Bytes;
import org.paukov.combinatorics.Factory;

import java.util.*;

class Oriented5Cycle {

    /**
     * Generate (3,2)-sequences to apply when there is a cycle in \spi with length
     * equals to 5 that doesn't allow the application of a 2-move.
     */
    static List<Case> generate() {
        final var orientedCycle = new Cycle("(0,3,1,4,2)");
        final var orientedTriple = new byte[]{0, 1, 2};

        final var result = new ArrayList<Case>();

        final var verifiedConfigurations = new HashSet<String>();

        final var spi = new MulticyclePermutation(orientedCycle);

        for (final var permutation : Factory.createPermutationGenerator(Factory.createVector(spi.getSymbols()))) {
            final var pi = new Cycle(Bytes.toArray(permutation.getVector()));

            // It is the case to avoid combinations originating 2-moves because the symbols
            // in the only cycle of spi are indeed the symbols of the actual 5-cycle
            if (pi.areSymbolsInCyclicOrder(orientedTriple) && CommonOperations.searchFor2Move(spi, pi) == null) {
                final var rhos = CommonOperations.findSortingSequence(pi.getSymbols(), spi, new Stack<>(), spi.getNumberOfEvenCycles(),
                        1.5F);

                if (!rhos.isEmpty()) {
                    if (rhos.size() > 1) {
                        final var signatures = new TreeSet<String>();
                        for (final var symbol : pi.getSymbols()) {
                            final var cr = CommonOperations.canonicalize(spi, pi.getStartingBy(symbol).getSymbols());
                            signatures.add(cr.getValue0().toString());
                        }

                        if (!verifiedConfigurations.contains(signatures.toString())) {
                            result.add(new Case(pi.getSymbols(), spi, rhos));
                            verifiedConfigurations.add(signatures.toString());
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
