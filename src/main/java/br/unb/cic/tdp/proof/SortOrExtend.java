package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.util.ArrayUtils;
import br.unb.cic.tdp.util.SymbolInserter;
import br.unb.cic.tdp.util.WeakCompositions;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;
import org.paukov.combinatorics3.Generator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static br.unb.cic.tdp.base.CommonOperations.pivots;
import static br.unb.cic.tdp.util.CircularMatcher.isUnorderedSubsequence;
import static java.lang.String.format;

@Slf4j
public class SortOrExtend extends AbstractSortOrExtend {

    protected static int n = 0;
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
    public Stream<Pair<String, Configuration>> type1Extensions(final Configuration configuration) {
        return preProcess(configuration)
                .flatMap(config -> type1SubExtensions(config)
                        .flatMap(firstPair -> type1SubExtensions(firstPair.getRight())
                                .map(secondPair -> Pair.of("%s; %s".formatted(firstPair.getLeft(), secondPair.getLeft()), secondPair.getRight())))
                        .filter(extension -> isValid(configuration, extension.getRight())));
    }

    private Stream<Pair<String, Configuration>> type1SubExtensions(final Configuration configuration) {
        // joins two existing cycles
        return Generator.combination(configuration.getSpi())
                .simple(2)
                .stream()
                .flatMap(pair -> IntStream.of(pair.get(0).getSymbols())
                        .boxed()
                        .flatMap(a -> IntStream.of(pair.get(1).getSymbols())
                                .boxed()
                                .map(b -> Pair.of(format("joined=[%s, %s], a=%d, b=%d", pair.get(0), pair.get(1), a, b), new Configuration(configuration.getSpi().times(Cycle.of(a, b)), configuration.getPi())))));
    }

    public Stream<Configuration> extensions(final Configuration configuration) {
        return type1Extensions(configuration).map(Pair::getRight);
    }

    private Stream<Configuration> preProcess(final Configuration configuration) {
        val n = configuration.getPi().size() - 1;

        val result = new ArrayList<Configuration>();
        result.add(configuration);

        var newSpi = new MulticyclePermutation(configuration.getSpi());
        newSpi.add(Cycle.of(n + 1));

        for (val composition : WeakCompositions.generate(1, n + 1)) {
            val newPi = Cycle.of(SymbolInserter.insertSymbols(configuration.getPi().getSymbols(), composition, new int[]{n + 1}));
            result.add(new Configuration(newSpi, newPi));
        }

        newSpi = new MulticyclePermutation(configuration.getSpi());
        newSpi.add(Cycle.of(n + 1));
        newSpi.add(Cycle.of(n + 2));

        for (val composition : WeakCompositions.generate(2, n + 1)) {
            val newPi = Cycle.of(SymbolInserter.insertSymbols(configuration.getPi().getSymbols(), composition, new int[]{n + 1, n + 2}));
            result.add(new Configuration(newSpi, newPi));
        }

        newSpi = new MulticyclePermutation(configuration.getSpi());
        newSpi.add(Cycle.of(n + 1));
        newSpi.add(Cycle.of(n + 2));
        newSpi.add(Cycle.of(n + 3));

        for (val composition : WeakCompositions.generate(3, n + 1)) {
            val newPi = Cycle.of(SymbolInserter.insertSymbols(configuration.getPi().getSymbols(), composition, new int[]{n + 1, n + 2, n + 3}));
            result.add(new Configuration(newSpi, newPi));
        }

        newSpi = new MulticyclePermutation(configuration.getSpi());
        newSpi.add(Cycle.of(n + 1));
        newSpi.add(Cycle.of(n + 2));
        newSpi.add(Cycle.of(n + 3));
        newSpi.add(Cycle.of(n + 4));

        for (val composition : WeakCompositions.generate(4, n + 1)) {
            val newPi = Cycle.of(SymbolInserter.insertSymbols(configuration.getPi().getSymbols(), composition, new int[]{n + 1, n + 2, n + 3, n + 4}));
            result.add(new Configuration(newSpi, newPi));
        }

        return result.stream();
    }

    protected boolean isValid(final Configuration configuration, final Configuration extension) {
        return extension.isFull() &&
                extension.getSpi().stream().noneMatch(Cycle::isTrivial) &&
                extension.getSpi().stream().filter(Cycle::isTwoCycle).count() <= 2 &&
                onlyOneComponent(extension);
    }

    private static boolean onlyOneComponent(final Configuration extension) {
        return extension.getPi().size() < 5 || (noSingleOriented3CycleComponent(extension) && noIntersecting2CyclesComponent(extension));
    }

    private static boolean noIntersecting2CyclesComponent(final Configuration extension) {
        val symbolsList = extension.getSpi().stream()
                .filter(Cycle::isTwoCycle)
                .map(Cycle::getSymbols)
                .toList();
        val s = ArrayUtils.flatten(symbolsList);
        if (s.length < 4) {
            return true;
        }
        return !isUnorderedSubsequence(s, extension.getPi());
    }

    private static boolean noSingleOriented3CycleComponent(final Configuration extension) {
        return extension.getSpi().stream()
                .filter(cycle -> cycle.size() == 3)
                .noneMatch(cycle -> isUnorderedSubsequence(cycle.getSymbols(), extension.getPi()));
    }


    @Override
    public Set<Integer> sortingPivots(final Configuration configuration) {
        return pivots(configuration);
    }

    @Override
    protected void extend(final Pair<Configuration, Set<Integer>> configurationPair) {
        val maxSymbol = configurationPair.getLeft().getPi().getMaxSymbol();

        if (maxSymbol > n) {
            n = maxSymbol;
        }

        extensions(configurationPair.getLeft())
                .map(Configuration::getSignature)
                .distinct()
                .map(s -> Configuration.ofSignature(s.getContent())) // canonical computation rely on this instantiation from signature
                .map(extension -> new SortOrExtend(
                        configurationPair,
                        Pair.of(extension, sortingPivots(extension)),
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
}
