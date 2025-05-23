package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.CommonOperations;
import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;
import org.paukov.combinatorics3.Generator;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static br.unb.cic.tdp.base.CommonOperations.CANONICAL_PI;
import static br.unb.cic.tdp.base.CommonOperations.pivots;
import static java.lang.String.format;
import static java.util.stream.Stream.concat;

@Slf4j
public class SortOrExtend extends AbstractSortOrExtend {

    private static int n = 0;
    private static final AtomicLong enqueued = new AtomicLong();
    private static final Timer timer = new Timer();

    static {
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                log.info("{}, max n: {}, queue size: {}", Instant.now(), n, enqueued.get());
            }
        }, 60_000, 60_000);
    }

    public SortOrExtend(
            final Pair<Configuration, Set<Integer>> parent,
            final Pair<Configuration, Set<Integer>> configurationPair,
            final ProofStorage storage,
            final double minRate
    ) {
        super(parent, configurationPair, storage, minRate);
        enqueued.incrementAndGet();
    }

    /*
     * Type 1 extension.
     */
    static List<Pair<String, Configuration>> type1Extensions(final Configuration configuration) {
        if (configuration.getSpi().stream().anyMatch(Cycle::isTwoCycle)) {
            return List.of();
        }

        val result = new ArrayList<Pair<String, Configuration>>();

        val n = configuration.getPi().getSymbols().length;

        val newCycle = format("(%d %d %d)", n, n + 2, n + 1);
        val newSpi = new MulticyclePermutation(configuration.getSpi() + newCycle);

        val newSymbols = new int[]{n + 2, n + 1, n};

        for (var i = 0; i < n - 2; i++) {
            for (var j = i + 1; j < n - 1; j++) {
                for (var k = j + 1; k < n; k++) {
                    val extension = new int[n + 3];
                    var originalPos = 0;
                    var newPos = 0;

                    for (var l = 0; l < n; l++) {
                        extension[originalPos++] = configuration.getPi().getSymbols()[l];

                        if (l == i || l == j || l == k) {
                            extension[originalPos++] = newSymbols[newPos++];
                        }
                    }

                    result.add(Pair.of(format("a=%d b=%d c=%d", i, j, k), new Configuration(newSpi, Cycle.of(extension))));
                }
            }
        }

        return result.stream()
                .filter(extension -> isValid(configuration, extension.getRight()))
                .toList();
    }

    /*
     * Type 2 extension.
     */
    static List<Pair<String, Configuration>> type2Extensions(Configuration configuration) {
        return type2SubExtensions(configuration).stream().flatMap(firstPair -> type2SubExtensions(firstPair.getRight()).stream().map(secondPair -> Pair.of("%s; %s".formatted(firstPair.getLeft(), secondPair.getLeft()), secondPair.getRight())))
                .filter(extension -> isValid(configuration, extension.getRight()))
                .toList();
    }

    static List<Pair<String, Configuration>> type2SubExtensions(final Configuration configuration) {
        val result = new ArrayList<Pair<String, Configuration>>();

        val n = configuration.getPi().getSymbols().length;

        {
            val newCycle = format("(%d %d)", n, n + 1);
            val newSpi = new MulticyclePermutation(configuration.getSpi() + newCycle);

            val newSymbols = new int[]{n + 1, n};

            for (var i = 0; i < n - 1; i++) {
                for (var j = i + 1; j < n; j++) {
                    val extension = new int[n + 2];
                    var originalPos = 0;
                    var newPos = 0;

                    for (var k = 0; k < n; k++) {
                        extension[originalPos++] = configuration.getPi().getSymbols()[k];

                        if (k == i || k == j) {
                            extension[originalPos++] = newSymbols[newPos++];
                        }
                    }

                    result.add(Pair.of(format("new cycle, pos a=%d b=%d", i, j), new Configuration(newSpi, Cycle.of(extension))));
                }
            }
        }

        {
            // grows an existing cycle
            for (var cycle : configuration.getSpi()) {
                val newCycle = format("(%s %d)", cycle.toString().substring(0, cycle.toString().length() - 1), n);
                val newSpi = configuration.getSpi().toString().replace(cycle.toString(), newCycle);
                for (var a = 0; a <= n; a++) {
                    val extendedPi = CommonOperations.unorientedExtension(configuration.getPi().getSymbols(), n, a).elements();
                    val extension = new Configuration(new MulticyclePermutation(newSpi), Cycle.of(extendedPi));
                    result.add(Pair.of(format("grown cycle=%s, pos a=%d", cycle, a), extension));
                }
            }
        }

        return result;
    }

    /*
     * Type 3 extension.
     */
    static List<Pair<String, Configuration>> type3Extensions(final Configuration configuration) {
        val result = new ArrayList<Pair<String, Configuration>>();

        val pi = configuration.getPi();
        val spi = configuration.getSpi();
        val n = pi.getSymbols().length;

        // no new symbol
        for (val cycles : Generator.permutation(configuration.getSpi()).k(3)) {
            for (val triple : Generator.cartesianProduct(cycles.get(0).getSymbolsAsList(),
                    cycles.get(1).getSymbolsAsList(), cycles.get(2).getSymbolsAsList())) {
                val move = Cycle.of(triple.get(0), triple.get(1), triple.get(2));
                if (CommonOperations.isOriented(pi, move)) {
                    result.add(Pair.of("%s".formatted(move), new Configuration(spi.times(move.getInverse()), move.times(pi).asNCycle())));
                }
            }
        }

        // one new symbol
        var extendedSpi = new MulticyclePermutation(configuration.getSpi());
        extendedSpi.add(Cycle.of(n));

        for (var a = 0; a <= n; a++) {
            val extendedPi = Cycle.of(CommonOperations.insertAtPosition(pi.getSymbols(), n, a));

            for (val cycles : Generator.permutation(configuration.getSpi()).k(2)) {
                for (val pair : Generator.cartesianProduct(cycles.get(0).getSymbolsAsList(),
                        cycles.get(1).getSymbolsAsList())) {
                    val move = Cycle.of(pair.get(0), pair.get(1), n);
                    if (CommonOperations.isOriented(extendedPi, move)) {
                        result.add(Pair.of("%s".formatted(move), new Configuration(spi.times(move.getInverse()), move.times(extendedPi).asNCycle())));
                    }
                }
            }
        }

        // two new symbols
        extendedSpi = new MulticyclePermutation(configuration.getSpi());
        extendedSpi.add(Cycle.of(n));
        extendedSpi.add(Cycle.of(n + 1));

        val newSymbols = new int[]{n + 1, n};

        for (int i = 0; i < n - 1; i++) {
            for (int j = i + 1; j < n; j++) {
                val extension = new int[n + 2];
                int originalPos = 0;
                int newPos = 0;

                for (int g = 0; g < n; g++) {
                    extension[originalPos++] = configuration.getPi().getSymbols()[g];

                    if (g == i || g == j) {
                        extension[originalPos++] = newSymbols[newPos++];
                    }
                }

                val extendedPi = Cycle.of(extension);

                for (val cycle : configuration.getSpi()) {
                    for (val c : cycle.getSymbols()) {
                        var move = Cycle.of(n, n + 1, c);
                        if (!CommonOperations.isOriented(extendedPi, move)) {
                            move = Cycle.of(n + 1, n, c);
                        }
                        result.add(Pair.of("%s".formatted(move), new Configuration(spi.times(move.getInverse()), move.times(extendedPi).asNCycle())));
                    }
                }
            }
        }


        return result.stream()
                .filter(extension -> isValid(configuration, extension.getRight()))
                .toList();
    }

    private static Stream<Configuration> extensions(final Configuration configuration) {
        return concat(
                type1Extensions(configuration).stream().map(Pair::getRight),
                concat(
                        type2Extensions(configuration).stream().map(Pair::getRight),
                        type3Extensions(configuration).stream().map(Pair::getRight)
                ));
    }

    private static boolean isValid(final Configuration configuration, final Configuration extension) {
        if (extension.isFull()) {
            val numOf2Cycles = configuration.getSpi().stream().filter(Cycle::isTwoCycle).count();
            return numOf2Cycles == 0 || extension.getSpi().stream().noneMatch(Cycle::isTwoCycle);
        }
        return false;
    }

    @Override
    protected Set<Integer> getStandardPivots(final Configuration configuration) {
        return pivots(configuration);
    }

    @Override
    protected void extend(Pair<Configuration, Set<Integer>> configurationPair) {
        val maxSymbol = configurationPair.getLeft().getPi().getMaxSymbol();

        if (maxSymbol > n) {
            n = maxSymbol;
        }

        extensions(configurationPair.getLeft())
                .filter(Configuration::isFull)
                .map(Configuration::getSignature)
                .distinct()
                .map(s -> Configuration.ofSignature(s.getContent()))
                .map(extension -> new SortOrExtend(
                        configurationPair,
                        Pair.of(extension, pivots(extension)),
                        storage,
                        minRate)
                )
                .forEach(ForkJoinTask::fork);
    }

    @Override
    protected void compute() {
        enqueued.decrementAndGet();
        super.compute();
    }

    @Override
    protected Pair<Configuration, Set<Integer>> canonicalize(final Pair<Configuration, Set<Integer>> configurationPair) {
        return getCanonical(configurationPair);
    }

    public static Pair<Configuration, Set<Integer>> getCanonical(final Pair<Configuration, Set<Integer>> configurationPair) {
        val configuration = Configuration.ofSignature(configurationPair.getLeft().getSignature().getContent());
        return canonical(Pair.of(configuration, pivots(configuration)));
    }

    private static Pair<Configuration, Set<Integer>> canonical(final Pair<Configuration, Set<Integer>> configurationPair) {
        var canonical = configurationPair;
        var canonicalStr = configurationPair.toString();

        for (int i = 0; i < configurationPair.getLeft().getSpi().getMaxSymbol(); i++) {
            val rotation = rotate(i, configurationPair.getLeft().getSpi(), configurationPair.getRight());
            if (rotation.toString().compareTo(canonicalStr) < 0) {
                canonical = rotation;
                canonicalStr = rotation.toString();
            }
            val reflection = mirror(rotation.getLeft().getSpi(), rotation.getRight());
            if (reflection.toString().compareTo(canonicalStr) < 0) {
                canonical = reflection;
                canonicalStr = reflection.toString();
            }
        }
        return canonical;
    }

    private static Pair<Configuration, Set<Integer>> rotate(final int i, MulticyclePermutation spi, Set<Integer> pivots) {
        var conjugator = CommonOperations.CANONICAL_PI[spi.getNumberOfSymbols()].getInverse();

        for (int j = 0; j < i; j++) {
            spi = spi.conjugateBy(conjugator);
        }

        for (int j = 0; j < i; j++) {
            pivots = pivots.stream().map(conjugator::image).collect(Collectors.toCollection(TreeSet::new));
        }

        return Pair.of(new Configuration(spi), pivots);
    }

    private static Pair<Configuration, Set<Integer>> mirror(final MulticyclePermutation spi, final Set<Integer> pivots) {
        val pi = CommonOperations.CANONICAL_PI[spi.getNumberOfSymbols()];
        val conjugator = new MulticyclePermutation();
        for (var i = 0; i < pi.size() / 2; i++) {
            conjugator.add(Cycle.of(pi.get(i), pi.get(pi.size() - 1 - i)));
        }
        return Pair.of(new Configuration(spi.conjugateBy(conjugator).getInverse()), pivots.stream()
                .map(conjugator::image)
                .collect(Collectors.toCollection(TreeSet::new)));
    }
}
