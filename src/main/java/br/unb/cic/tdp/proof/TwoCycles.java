package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.CommonOperations;
import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;
import org.paukov.combinatorics3.Generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.Collectors;

import static br.unb.cic.tdp.base.CommonOperations.insertAtPosition;
import static br.unb.cic.tdp.base.CommonOperations.unorientedExtension;
import static java.lang.String.format;
import static java.util.function.Predicate.not;
import static java.util.stream.Stream.concat;

public class TwoCycles {

    @SneakyThrows
    public static void generate(final String outputDir, final double minRate) {
        try (val pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors())) {
            val proofStorage = new H2ProofStorage("%s/2cycles".formatted(outputDir));
            pool.execute(new TwoCyclesSortOrExtend(new Configuration("(0)"), new Configuration("(0 2)(1 3)"), proofStorage, minRate));
            pool.execute(new TwoCyclesSortOrExtend(new Configuration("(0)"), new Configuration("(0 2)(1 4 3)"), proofStorage, minRate));
            pool.shutdown();
        }
    }

    private static class TwoCyclesSortOrExtend extends AbstractSortOrExtend {

        public TwoCyclesSortOrExtend(final Configuration parent,
                                     final Configuration configuration,
                                     final ProofStorage storage,
                                     final double minRate) {
            super(parent, configuration, storage, minRate);
        }

        @Override
        protected Set<Integer> getPivots(Configuration configuration) {
            return configuration.getSpi().stream()
                    .filter(not(Cycle::isTwoCycle))
                    .map(Cycle::getMinSymbol)
                    .collect(Collectors.toSet());
        }

        @Override
        protected void extend(final Configuration noSortingConfig) {
            val num2Cycles = noSortingConfig.getSpi().stream().filter(Cycle::isTwoCycle).count();
            concat(
                    type1Extensions(noSortingConfig).stream(),
                    concat(
                            type2Extensions(noSortingConfig).stream(),
                            type3Extensions(noSortingConfig).stream()
                    )
            )
                    .map(Pair::getRight)
                    .filter(extension -> extension.openGates().count() <= 1)
                    .filter(extension -> extension.getSpi().stream().filter(Cycle::isTwoCycle).count() >= num2Cycles)
                    .map(extension -> new TwoCyclesSortOrExtend(noSortingConfig, extension, storage, minRate))
                    .forEach(ForkJoinTask::fork);
        }

        /*
         * Type 1 extension.
         */
        private static List<Pair<String, Configuration>> type1Extensions(final Configuration configuration) {
            val result = new ArrayList<Pair<String, Configuration>>();

            val n = configuration.getPi().getSymbols().length;

            for (var a = 0; a < n; a++) {
                for (var b = 0; b < n; b++) {
                    for (var c = b; c < n; c++) {
                        if (!(a == b && b == c)) {
                            val newCycle = format("(%d %d %d)", n, n + 2, n + 1);
                            val extendedPi = unorientedExtension(configuration.getPi().getSymbols(), n, a, b, c).elements();
                            val extension = new Configuration(new MulticyclePermutation(configuration.getSpi() + newCycle), Cycle.of(extendedPi));
                            result.add(Pair.of(format("a=%d b=%d c=%d", a, b, c), extension));
                        }
                    }
                }
            }

            return result;
        }

        /*
         * Type 2 extension.
         */
        private static List<Pair<String, Configuration>> type2Extensions(final Configuration configuration) {
            val result = new ArrayList<Pair<String, Configuration>>();

            val n = configuration.getPi().getSymbols().length;

            // adds a new 2-cycle, introducing two new symbols
            for (var a = 0; a < n; a++) {
                var extendedPi = unorientedExtension(configuration.getPi().getSymbols(), n, a).elements();
                for (var b = 0; b < n + 1; b++) {
                    extendedPi = unorientedExtension(extendedPi, n + 1, b).elements();
                    val newCycle = format("(%d %d)", n, n + 1);
                    val extension = new Configuration(new MulticyclePermutation(configuration.getSpi() + newCycle), Cycle.of(extendedPi));
                    result.add(Pair.of(format("a=%d b=%d", a, b), extension));
                }
            }

            // grows an existing cycle
            for (var cycle : configuration.getSpi()) {
                if (!cycle.isTwoCycle()) {
                    for (var a = 0; a < n; a++) {
                        val newCycle = format("(%s %d)", cycle.toString().substring(0, cycle.toString().length() - 1), n);
                        val extendedPi = unorientedExtension(configuration.getPi().getSymbols(), n, a).elements();
                        val extension = new Configuration(new MulticyclePermutation(configuration.getSpi().toString().replace(cycle.toString(), newCycle.toString())), Cycle.of(extendedPi));
                        result.add(Pair.of(format("cycle=%s a=%d", cycle, a), extension));
                    }
                }
            }

            return result;
        }

        /*
         * Type 3 extension.
         */
        private static List<Pair<String, Configuration>> type3Extensions(final Configuration configuration) {
            val result = new ArrayList<Pair<String, Configuration>>();

            val pi = configuration.getPi();
            val spi = configuration.getSpi();
            val n = pi.getSymbols().length;

            // no new symbol
            for (val cycles : Generator.permutation(configuration.getSpi().stream().filter(not(Cycle::isTwoCycle)).collect(Collectors.toList())).k(3)) {
                for (val triple : Generator.cartesianProduct(cycles.get(0).getSymbolsAsList(),
                        cycles.get(1).getSymbolsAsList(), cycles.get(2).getSymbolsAsList())) {
                    val move = Cycle.of(triple.get(0), triple.get(1), triple.get(2));
                    if (CommonOperations.isOriented(pi, move)) {
                        result.add(Pair.of("", new Configuration(spi.times(move.getInverse()), move.times(pi).asNCycle())));
                    }
                }
            }

            // one new symbol
            var extendedSpi = new MulticyclePermutation(configuration.getSpi());
            extendedSpi.add(Cycle.of(n));

            for (var a = 0; a <= n; a++) {
                val extendedPi = Cycle.of(insertAtPosition(pi.getSymbols(), n, a));

                for (val cycles : Generator.permutation(configuration.getSpi().stream().filter(not(Cycle::isTwoCycle)).collect(Collectors.toList())).k(2)) {
                    for (val pair : Generator.cartesianProduct(cycles.get(0).getSymbolsAsList(),
                            cycles.get(1).getSymbolsAsList())) {
                        val move = Cycle.of(pair.get(0), pair.get(1), n);
                        if (CommonOperations.isOriented(extendedPi, move)) {
                            result.add(Pair.of("", new Configuration(spi.times(move.getInverse()), move.times(extendedPi).asNCycle())));
                        }
                    }
                }
            }

            // two new symbols
            extendedSpi = new MulticyclePermutation(configuration.getSpi());
            extendedSpi.add(Cycle.of(n));
            extendedSpi.add(Cycle.of(n + 1));

            for (var a = 0; a <= n; a++) {
                var extendedPi = Cycle.of(insertAtPosition(pi.getSymbols(), n, a));
                for (var b = 0; b <= n + 1; b++) {
                    extendedPi = Cycle.of(insertAtPosition(extendedPi.getSymbols(), n + 1, b));

                    for (val cycle : configuration.getSpi()) {
                        if (cycle.isTwoCycle()) {
                            for (val c : cycle.getSymbols()) {
                                var move = Cycle.of(n, n + 1, c);
                                if (CommonOperations.isOriented(extendedPi, move)) {
                                    result.add(Pair.of("", new Configuration(spi.times(move.getInverse()), move.times(extendedPi).asNCycle())));
                                }
                                move = Cycle.of(n + 1, n, c);
                                if (CommonOperations.isOriented(extendedPi, move)) {
                                    result.add(Pair.of("", new Configuration(spi.times(move.getInverse()), move.times(extendedPi).asNCycle())));
                                }
                            }
                        }
                    }
                }
            }

            return result;
        }
    }
}