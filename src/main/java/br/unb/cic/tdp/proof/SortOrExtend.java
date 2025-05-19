package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.CommonOperations;
import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;
import org.paukov.combinatorics3.Generator;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static br.unb.cic.tdp.base.CommonOperations.*;
import static java.lang.String.format;
import static java.util.stream.Stream.concat;

public class SortOrExtend extends AbstractSortOrExtend {

    private static int n = 0;
    private static final AtomicLong enqueued = new AtomicLong();
    private static final Timer timer = new Timer();

    static {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.printf("%s, queue size: %d%n", Instant.now(), enqueued.get());
                System.out.printf("max n: %d%n", n);
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

        final int[] newSymbols = {n + 2, n + 1, n};

        for (int i = 0; i < n - 2; i++) {
            for (int j = i + 1; j < n - 1; j++) {
                for (int k = j + 1; k < n; k++) {
                    int[] extension = new int[n + 3];
                    int originalPos = 0;
                    int newPos = 0;

                    for (int g = 0; g < n; g++) {
                        extension[originalPos++] = configuration.getPi().getSymbols()[g];

                        if (g == i || g == j || g == k) {
                            extension[originalPos++] = newSymbols[newPos++];
                        }
                    }

                    result.add(Pair.of(format("a=%d b=%d c=%d", i, j, k), new Configuration(newSpi, Cycle.of(extension))));
                }
            }
        }

        return result;
    }

    /*
     * Type 2 extension.
     */
    static List<Pair<String, Configuration>> type2Extensions(final Configuration configuration) {
        val result = new ArrayList<Pair<String, Configuration>>();

        val n = configuration.getPi().getSymbols().length;

        {
            val newCycle = format("(%d %d)", n, n + 1);
            val newSpi = new MulticyclePermutation(configuration.getSpi() + newCycle);

            final int[] newSymbols = {n + 1, n};

            for (int i = 0; i < n - 1; i++) {
                for (int j = i + 1; j < n; j++) {
                    int[] extension = new int[n + 2];
                    int originalPos = 0;
                    int newPos = 0;

                    for (int g = 0; g < n; g++) {
                        extension[originalPos++] = configuration.getPi().getSymbols()[g];

                        if (g == i || g == j) {
                            extension[originalPos++] = newSymbols[newPos++];
                        }
                    }

                    result.add(Pair.of(format("a=%d b=%d", i, j), new Configuration(newSpi, Cycle.of(extension))));
                }
            }
        }

        {
            // grows an existing cycle
            for (var cycle : configuration.getSpi()) {
                val newCycle = format("(%s %d)", cycle.toString().substring(0, cycle.toString().length() - 1), n);
                val newSpi = configuration.getSpi().toString().replace(cycle.toString(), newCycle);
                for (var a = 0; a <= n; a++) {
                    val extendedPi = unorientedExtension(configuration.getPi().getSymbols(), n, a).elements();
                    val extension = new Configuration(new MulticyclePermutation(newSpi), Cycle.of(extendedPi));
                    result.add(Pair.of(format("cycle=%s a=%d", cycle, a), extension));
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
            val extendedPi = Cycle.of(insertAtPosition(pi.getSymbols(), n, a));

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

        final int[] newSymbols = {n + 1, n};

        for (int i = 0; i < n - 1; i++) {
            for (int j = i + 1; j < n; j++) {
                int[] extension = new int[n + 2];
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

        return result;
    }

    private static Stream<Configuration> extensions(final Configuration configuration) {
        val numOf2Cycles = configuration.getSpi().stream().filter(Cycle::isTwoCycle).count();

        return concat(
                type1Extensions(configuration).stream().map(Pair::getRight),
                concat(
                        type2Extensions(configuration).stream().map(Pair::getRight).flatMap(extension -> type2Extensions(extension).stream()).map(Pair::getRight),
                        type3Extensions(configuration).stream().map(Pair::getRight)
                ))
                .filter(extension -> numOf2Cycles == 0 || extension.getSpi().stream().noneMatch(Cycle::isTwoCycle));
    }

    @Override
    protected Set<Integer> getStandardPivots(final Configuration configuration) {
        return pivots(configuration);
    }

    private static Set<Integer> pivots(Configuration configuration) {
        return configuration.getSpi().stream()
                .map(cycle -> rightMostSymbol(cycle, configuration.getPi()))
                .collect(Collectors.toSet());
    }

    public static Integer rightMostSymbol(final Cycle cycle, final Cycle pi) {
        return Arrays.stream(cycle.getSymbols())
                .boxed()
                .map(s -> Pair.of(s, s == 0 ? pi.size() : pi.indexOf(s))) // zero is the rightmost symbol
                .max(Comparator.comparing(Pair::getRight)).get().getLeft();
    }

    @Override
    protected void extend(Pair<Configuration, Set<Integer>> configurationPair) {
        int maxSymbol = configurationPair.getLeft().getPi().getMaxSymbol();

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
        val configuration = configurationPair.getLeft();
        return canonical(Pair.of(Configuration.ofSignature(configuration.getSignature().getContent()), pivots(configuration)));
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
        var conjugator = CANONICAL_PI[spi.getNumberOfSymbols()].getInverse();

        for (int j = 0; j < i; j++) {
            spi = spi.conjugateBy(conjugator);
        }

        for (int j = 0; j < i; j++) {
            pivots = pivots.stream().map(conjugator::image).collect(Collectors.toCollection(TreeSet::new));
        }

        return Pair.of(new Configuration(spi), pivots);
    }

    private static Pair<Configuration, Set<Integer>> mirror(final MulticyclePermutation spi, final Set<Integer> pivots) {
        val pi = CANONICAL_PI[spi.getNumberOfSymbols()];
        val conjugator = new MulticyclePermutation();
        for (var i = 0; i < pi.size() / 2; i++) {
            conjugator.add(Cycle.of(pi.get(i), pi.get(pi.size() - 1 - i)));
        }
        return Pair.of(new Configuration(spi.conjugateBy(conjugator).getInverse()), pivots.stream()
                .map(conjugator::image)
                .collect(Collectors.toCollection(TreeSet::new)));
    }

    public static void main(String[] args) {
        extensions(new Configuration("(0 5 2)(1 7 4)(3 8 6)"))
                .filter(Configuration::isFull)
                .map(Configuration::getSignature)
                .distinct()
                .map(s -> Configuration.ofSignature(s.getContent()))
                .forEach(System.out::println);
    }
}
