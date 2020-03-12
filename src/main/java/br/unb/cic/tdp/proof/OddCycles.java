package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.OrientedConfiguration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import com.google.common.primitives.Bytes;
import org.paukov.combinatorics.Factory;

import java.util.*;

import static br.unb.cic.tdp.base.CommonOperations.createCycleIndex;
import static br.unb.cic.tdp.base.CommonOperations.generateAll0_2Moves;

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

        final var verifiedConfigurations = new HashSet<OrientedConfiguration>();
        permutation:
        for (final var permutation : Factory.createPermutationGenerator(Factory.createVector(spi.getSymbols()))) {
            final var pi = new Cycle(Bytes.toArray(permutation.getVector())).getInverse();
            final var spiCycleIndex = createCycleIndex(spi, pi);

            final var iterator = generateAll0_2Moves(pi, spiCycleIndex).iterator();
            while (iterator.hasNext()) {
                final var move = iterator.next();
                final var rho1 = move.getKey();

                if (move.getValue() == 2) {
                    if (moves == 1) {
                        final var configuration = new OrientedConfiguration(spi, pi);
                        if (!verifiedConfigurations.contains(configuration)) {
                            result.add(new Case(spi, pi, Collections.singletonList(rho1)));
                            verifiedConfigurations.add(configuration);
                        }

                        continue permutation;
                    } else {
                        final var _iterator = generateAll0_2Moves(pi, spiCycleIndex).iterator();
                        while (_iterator.hasNext()) {
                            final var move2 = _iterator.next();
                            final var rho2 = move2.getKey();
                            if (move2.getValue() == 2) {
                                final var configuration = new OrientedConfiguration(spi, pi);
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
