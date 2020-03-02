package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import com.google.common.primitives.Bytes;
import org.paukov.combinatorics.Factory;

import java.util.*;

import static br.unb.cic.tdp.CommonOperations.*;

public class Cases3_2 {

    /**
     * Generate the (3,2)-sequences to apply when we have either two interleaving
     * pairs or three intersecting 3-cycles in \spi.
     *
     * @return a list of cases.
     */
    public static List<Case> generate() {
        final var result = new ArrayList<Case>();
        result.add(new Case(new MulticyclePermutation("(0,4,2)(1,5,3)"), new Cycle("0,1,2,3,4,5"),
                Arrays.asList(new Cycle("0,2,4"), new Cycle("3,1,5"), new Cycle("2,4,0"))));
        result.addAll(generate(new MulticyclePermutation("(0,1,2)(3,4,5)(6,7,8)")));
        return result;
    }

    private static List<Case> generate(final MulticyclePermutation spi) {
        final var result = new ArrayList<Case>();

        final var verifiedConfigurations = new HashSet<Configuration>();

        for (final var permutation : Factory.createPermutationGenerator(Factory.createVector(spi.getSymbols()))) {
            final var pi = new Cycle(Bytes.toArray(permutation.getVector()));
            if (spi.stream().noneMatch(cycle -> isOriented(pi, cycle))) {
                final var openGates = openGatesPerCycle(spi, pi.getInverse());
                if (openGates.values().stream().mapToInt(j -> j).sum() <= 2) {
                    final var rhos = searchForSortingSeq(pi, spi, new Stack<>(), 3, 1.5F);

                    final var configuration = new Configuration(spi, pi);
                    if (!verifiedConfigurations.contains(configuration)) {
                        result.add(new Case(spi, pi, rhos));
                        verifiedConfigurations.add(configuration);
                    }
                }
            }
        }

        return result;
    }
}
