package br.unb.cic.tdp.proof.seq1_1;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import com.google.common.primitives.Ints;
import org.apache.commons.math3.util.Pair;
import org.paukov.combinatorics.Factory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static br.unb.cic.tdp.base.CommonOperations.*;

public class OddCycles {

    /**
     * Generate the 2-moves (i.e. (1,1)-sequences) to apply when we have two odd
     * cycles in \spi$.
     */
    public static List<Pair<Configuration, List<Cycle>>> generate() {
        return generate(new MulticyclePermutation("(0,1)(2,3)"));
    }

    private static List<Pair<Configuration, List<Cycle>>> generate(final MulticyclePermutation spi) {
        final var result = new ArrayList<Pair<Configuration, List<Cycle>>>();

        final var verifiedConfigurations = new HashSet<Configuration>();
        permutation:
        for (final var permutation : Factory.createPermutationGenerator(Factory.createVector(spi.getSymbols()))) {
            final var pi = Cycle.create(Ints.toArray(permutation.getVector())).getInverse();
            final var iterator = generateAll0And2Moves(spi, pi).iterator();
            while (iterator.hasNext()) {
                final var pair = iterator.next();
                final var move = pair.getKey();

                final var configuration = new Configuration(spi, pi);
                if (!verifiedConfigurations.contains(configuration)) {
                    result.add(new Pair<>(configuration, Collections.singletonList(move)));
                    verifiedConfigurations.add(configuration);
                }

                continue permutation;
            }
        }

        return result;
    }
}
