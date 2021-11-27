package br.unb.cic.tdp.proof.seq3_2;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import com.google.common.primitives.Ints;
import org.apache.commons.math3.util.Pair;
import org.paukov.combinatorics.Factory;

import java.util.*;

import static br.unb.cic.tdp.base.CommonOperations.*;

public class Cases3_2 {

    public static void main(String[] args) {
        generate().forEach(p -> System.out.println(p.getFirst().getCanonical().getSpi() + "->" +
                p.getFirst().getCanonical().translatedSorting(p.getFirst(), p.getSecond())));
    }

    /**
     * Generate the (3,2)-sequences to apply when we have either two interleaving
     * pairs or three intersecting 3-cycles in \spi.
     *
     * @return a list of cases.
     */
    public static List<Pair<Configuration, List<Cycle>>> generate() {
        final var result = new ArrayList<Pair<Configuration, List<Cycle>>>();
        result.add(new Pair<>(new Configuration(new MulticyclePermutation("(0,4,2)(1,5,3)")),
                Arrays.asList(Cycle.create("0,2,4"), Cycle.create("3,1,5"), Cycle.create("2,4,0"))));
        result.addAll(generate(new MulticyclePermutation("(0,1,2)(3,4,5)(6,7,8)")));
        return result;
    }

    private static List<Pair<Configuration, List<Cycle>>> generate(final MulticyclePermutation spi) {
        final var result = new ArrayList<Pair<Configuration, List<Cycle>>>();

        final var verifiedConfigurations = new HashSet<Configuration>();

        for (final var permutation : Factory.createPermutationGenerator(Factory.createVector(spi.getSymbols()))) {
            final var pi = Cycle.create(Ints.toArray(permutation.getVector()));
            if (spi.stream().noneMatch(cycle -> isOriented(pi, cycle))) {
                final var openGates = getOpenGates(spi, pi);
                if (openGates.size() <= 2) {
                    final var moves = searchForSortingSeq(pi, spi, new Stack<>(), 3, 1.5F);

                    final var configuration = new Configuration(spi, pi);
                    if (!verifiedConfigurations.contains(configuration)) {
                        result.add(new Pair<>(configuration, moves));
                        verifiedConfigurations.add(configuration);
                    }
                }
            }
        }

        return result;
    }
}
