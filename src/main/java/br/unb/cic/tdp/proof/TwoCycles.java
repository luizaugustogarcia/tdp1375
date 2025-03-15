package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import lombok.val;

import java.util.*;
import java.util.stream.Collectors;

import static br.unb.cic.tdp.base.CommonOperations.searchForSorting;
import static br.unb.cic.tdp.base.CommonOperations.twoLinesNotation;
import static br.unb.cic.tdp.base.Configuration.signature;

public class TwoCycles {

    public static void main(String[] args) {
        val minRate = 1.6;

        // one 2-cycle intersecting another 2-cycle
        val configuration = new Configuration(new MulticyclePermutation("(0 2)(1 3)"));
        System.out.println(configuration.getSpi() + "-" + searchForSorting(configuration, new HashSet<>(), twoLinesNotation(configuration.getSpi()), configuration.getPi().getSymbols(), new Stack<>(), minRate)
                .map(s -> s.stream().map(Arrays::toString).collect(Collectors.joining(", "))).stream().findFirst());

        // one 2-cycle intersecting one 3-segment
        extensions(new Configuration(new MulticyclePermutation("(0 2)(1 4 3)"))).stream().distinct().forEach(extension -> {
            val spi = extension.getSpi();
            val threeCycle = spi.stream().filter(c -> c.size() == 3).findFirst().get();
            val twoCycle = spi.stream().filter(c -> c.size() == 2).findFirst().get();
            System.out.println(spi + "-" + searchForSorting(extension, Set.of(twoCycle.get(0), threeCycle.get(0)), twoLinesNotation(spi), extension.getPi().getSymbols(), new Stack<>(), minRate)
                    .map(s -> s.stream().map(Arrays::toString).collect(Collectors.joining(", "))).stream().findFirst());
        });

        // one 2-cycle intersecting one 4-segment
        extensions(new Configuration(new MulticyclePermutation("(0 2)(1 5 4 3)"))).stream().distinct().forEach(extension -> {
            val spi = extension.getSpi();
            val fourCycle = spi.stream().filter(c -> c.size() == 4).findFirst().get();
            val twoCycle = spi.stream().filter(c -> c.size() == 2).findFirst().get();
            System.out.println(spi + "-" + searchForSorting(extension, Set.of(twoCycle.get(0), fourCycle.get(0)), twoLinesNotation(spi), extension.getPi().getSymbols(), new Stack<>(), minRate)
                    .map(s -> s.stream().map(Arrays::toString).collect(Collectors.joining(", "))).stream().findFirst());
        });
    }

    private static List<Configuration> extensions(final Configuration configuration) {
        val result = new ArrayList<Configuration>();

        val newLabel = configuration.getSpi().size() + 1;

        val signature = signature(configuration.getSpi(), configuration.getPi());

        for (int a = 0; a < signature.length; a++) {
            if (configuration.getOpenGates().contains(a)) {
                for (int b = 0; b < signature.length; b++) {
                    if (a != b) {
                       // result.add(ofSignature(unorientedExtension(signature, newLabel, a, b).elements()));
                    }
                }
            }
        }

        return result;
    }
}
