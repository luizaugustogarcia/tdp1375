package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import com.google.common.primitives.Bytes;
import org.paukov.combinatorics.Factory;

import java.util.*;

import static br.unb.cic.tdp.CommonOperations.canonicalize;
import static br.unb.cic.tdp.CommonOperations.findSortingSequence;

class OrientedCycleGreaterThan5 {

    /**
     * Generate (4,3)-sequences to apply when there is a cycle in \spi with length
     * greater or equals to 7 that doesn't allow the application of a 2-move.
     */
    public static List<Case> generate() {
        final var orientedCycle = new Cycle("0,3,4,1,5,2,6");
        final var triple = new byte[]{0, 1, 2};

        final var result = new ArrayList<Case>();

        final var verifiedConfigurations = new HashSet<String>();

        final var spi = new MulticyclePermutation(orientedCycle);

        for (final var permutation : Factory.createPermutationGenerator(Factory.createVector(Bytes.asList(orientedCycle.getSymbols())))) {
            final var pi = new Cycle(Bytes.toArray(permutation.getVector()));

            if (pi.isOrientedTriple(triple)) {
                final var rhos = findSortingSequence(pi, spi, new Stack<>(), spi.getNumberOfEvenCycles(),
                        1.375F);

                if (!rhos.isEmpty()) {
                    // If rhos.size() == 1, then there is a 2-move. We are only interested on the
                    // cases which do not allow the application of a 2-move
                    if (rhos.size() > 1) {
                        final var signatures = new TreeSet<String>();
                        for (final var symbol : pi.getSymbols()) {
                            final var cr = canonicalize(spi, pi.getStartingBy(symbol));
                            signatures.add(cr.first.toString());
                        }

                        if (!verifiedConfigurations.contains(signatures.toString())) {
                            result.add(new Case(pi, spi, rhos));
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
