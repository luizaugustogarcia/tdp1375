package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.permutation.PermutationGroups;
import br.unb.cic.tdp.Configuration;
import com.google.common.primitives.Bytes;
import org.paukov.combinatorics.Factory;

import java.util.*;

import static br.unb.cic.tdp.CommonOperations.applyTransposition;
import static br.unb.cic.tdp.CommonOperations.searchAllApp3Cycles;

public class OddCycles {

    /**
     * Generate the 2-moves (i.e. (1,1)-sequences) to apply when we have two odd
     * cycles in \spi$.
     */
    public static List<Case> generate() {
        return generate(new MulticyclePermutation("(0,1)(2,3)"), 1);
    }

    /**
     * Generate the (2,2)-sequences of 3-cycles to apply when we have two or four
     * odd cycles in \spi$.
     */
    public static List<Case> generate2_2Cases() {
        final var result = new ArrayList<Case>();
        result.addAll(generate(new MulticyclePermutation("(0,1)(2,3)"), 2));
        result.addAll(generate(new MulticyclePermutation("(0,1)(2,3)(4,5)(6,7)"), 2));
        return result;
    }

    private static List<Case> generate(final MulticyclePermutation spi, final int moves) {
        final var result = new ArrayList<Case>();

        final var verifiedConfigurations = new HashSet<Configuration>();

        permutation:
        for (final var permutation : Factory.createPermutationGenerator(Factory.createVector(spi.getSymbols()))) {
            final var pi = new Cycle(Bytes.toArray(permutation.getVector())).getInverse();

            for (final var rho1 : searchAllApp3Cycles(pi)) {
                final var _spi = PermutationGroups.computeProduct(spi, rho1.getInverse());
                final var _pi = applyTransposition(pi, rho1);

                if (_spi.getNumberOfEvenCycles() - spi.getNumberOfEvenCycles() == 2) {
                    if (moves == 1) {
                        final var configuration = new Configuration(spi, pi);
                        if (!verifiedConfigurations.contains(configuration)) {
                            result.add(new Case(spi, pi, Collections.singletonList(rho1)));
                            verifiedConfigurations.add(configuration);
                        }

                        continue permutation;
                    } else {
                        for (final var rho2 : searchAllApp3Cycles(_pi)) {
                            if (PermutationGroups.computeProduct(_spi, rho2.getInverse()).getNumberOfEvenCycles()
                                    - _spi.getNumberOfEvenCycles() == 2) {
                                final var configuration = new Configuration(spi, pi);
                                if (!verifiedConfigurations.contains(configuration)) {
                                    result.add(new Case(spi, pi, Arrays.asList(rho1, rho2)));
                                    verifiedConfigurations.add(configuration);
                                }

                                continue permutation;
                            }
                        }
                    }
                }
            }
        }

        return result;
    }
}
