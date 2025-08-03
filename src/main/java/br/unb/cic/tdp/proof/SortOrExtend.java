package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.base.PivotedConfiguration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.util.ArrayUtils;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;
import org.paukov.combinatorics3.Generator;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static br.unb.cic.tdp.base.CommonOperations.pivots;
import static br.unb.cic.tdp.base.PivotedConfiguration.of;
import static br.unb.cic.tdp.util.CircularMatcher.isUnorderedSubsequence;
import static br.unb.cic.tdp.util.SymbolInserter.insertSymbols;
import static br.unb.cic.tdp.util.WeakCompositions.weakCompositions;
import static java.lang.String.format;

@Slf4j
public class SortOrExtend extends AbstractSortOrExtend {

    protected static int n = 0;
    private static final AtomicLong enqueued = new AtomicLong();
    private static final Timer timer = new Timer();
    private static final ThreadLocal<Boolean> SHOULD_NOT_EXTEND_BY_ADDING_TWO_CYCLES = ThreadLocal.withInitial(() -> false);

    static {
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                log.info("{}, max n: {}, queue size: {}", Instant.now(), n, enqueued.get());
            }
        }, 60_000, 60_000);
    }

    public SortOrExtend(
            final PivotedConfiguration parent,
            final PivotedConfiguration pivotedConfiguration,
            final ProofStorage storage,
            final double minRate) {
        super(parent, pivotedConfiguration, storage, minRate);
    }

    // Model: G-grow one cycle, A-add a 2-cycle, M-merge two cycles
    // type 1: AA
    // type 2: AG
    // type 3: AM
    // type 4: GA
    // type 5: GG
    // type 6: GM
    // type 7: MA
    // type 8: MG
    // type 9: MM

    // optimize by caching isValid results

    public Stream<Pair<String, Configuration>> type1Extensions(final Configuration configuration) {
        if (SHOULD_NOT_EXTEND_BY_ADDING_TWO_CYCLES.get()) {
            return Stream.empty();
        }
        return chain(this::add2Cycle, this::add2Cycle, configuration);
    }

    public Stream<Pair<String, Configuration>> type2Extensions(final Configuration configuration) {
        if (SHOULD_NOT_EXTEND_BY_ADDING_TWO_CYCLES.get()) {
            return Stream.empty();
        }
        return chain(this::add2Cycle, this::growOneCycle, configuration);
    }

    public Stream<Pair<String, Configuration>> type3Extensions(final Configuration configuration) {
        if (SHOULD_NOT_EXTEND_BY_ADDING_TWO_CYCLES.get()) {
            return Stream.empty();
        }
        return chain(this::add2Cycle, this::mergeTwoCycles, configuration);
    }

    public Stream<Pair<String, Configuration>> type4Extensions(final Configuration configuration) {
        if (SHOULD_NOT_EXTEND_BY_ADDING_TWO_CYCLES.get()) {
            return Stream.empty();
        }
        return chain(this::growOneCycle, this::add2Cycle, configuration);
    }

    public Stream<Pair<String, Configuration>> type5Extensions(final Configuration configuration) {
        return chain(this::growOneCycle, this::growOneCycle, configuration);
    }

    public Stream<Pair<String, Configuration>> type6Extensions(final Configuration configuration) {
        return chain(this::growOneCycle, this::mergeTwoCycles, configuration);
    }

    public Stream<Pair<String, Configuration>> type7Extensions(final Configuration configuration) {
        if (SHOULD_NOT_EXTEND_BY_ADDING_TWO_CYCLES.get()) {
            return Stream.empty();
        }
        return chain(this::mergeTwoCycles, this::add2Cycle, configuration);
    }

    public Stream<Pair<String, Configuration>> type8Extensions(final Configuration configuration) {
        return chain(this::mergeTwoCycles, this::growOneCycle, configuration);
    }

    public Stream<Pair<String, Configuration>> type9Extensions(final Configuration configuration) {
        return chain(this::mergeTwoCycles, this::mergeTwoCycles, configuration);
    }

    private Stream<Pair<String, Configuration>> mergeTwoCycles(final Configuration configuration) {
        val twoCycles = configuration.getSpi().stream()
                .filter(Cycle::isTwoCycle)
                .toList();
        if (SHOULD_NOT_EXTEND_BY_ADDING_TWO_CYCLES.get() && !twoCycles.isEmpty()) {
            return twoCycles.stream().flatMap(c1 -> configuration.getSpi().stream()
                    .flatMap(c2 -> {
                        if (c1 == c2) {
                            return Stream.empty();
                        }
                        return IntStream.of(c1.getSymbols())
                                .boxed()
                                .flatMap(a -> IntStream.of(c2.getSymbols())
                                        .boxed()
                                        .map(b -> Pair.of(format("joined=[%s, %s], a=%d, b=%d", c2.get(0), c2.get(1), a, b), new Configuration(configuration.getSpi().times(Cycle.of(a, b)), configuration.getPi()))));
                    }));
        }

        return Generator.combination(configuration.getSpi())
                .simple(2)
                .stream()
                .flatMap(pair -> IntStream.of(pair.get(0).getSymbols())
                        .boxed()
                        .flatMap(a -> IntStream.of(pair.get(1).getSymbols())
                                .boxed()
                                .map(b -> Pair.of(format("joined=[%s, %s], a=%d, b=%d", pair.get(0), pair.get(1), a, b), new Configuration(configuration.getSpi().times(Cycle.of(a, b)), configuration.getPi())))));
    }

    private Stream<Pair<String, Configuration>> growOneCycle(final Configuration configuration) {
        val n = configuration.getPi().size() - 1;
        val result = new ArrayList<Pair<String, Configuration>>();

        for (val composition : weakCompositions(1, n + 1)) {
            val newPi = Cycle.of(insertSymbols(configuration.getPi().getSymbols(), composition, new int[]{n + 1}));

            // TODO: why this test?
            val spi = SHOULD_NOT_EXTEND_BY_ADDING_TWO_CYCLES.get() ? configuration.getSpi().stream().filter(Cycle::isTwoCycle).toList() : configuration.getSpi();
            for (val cycle : spi) {
                for (val comp : weakCompositions(1, cycle.size())) {
                    var newSpi = new MulticyclePermutation(configuration.getSpi());
                    newSpi.removeByReference(cycle);
                    var newCycle = Cycle.of(insertSymbols(cycle.getSymbols(), comp, new int[]{n + 1}));
                    newSpi.add(newCycle);
                    result.add(Pair.of(format("grown cycle %s by 1", cycle), new Configuration(newSpi, newPi)));
                }
            }
        }

        return result.stream();
    }

    private Stream<Pair<String, Configuration>> add2Cycle(final Configuration configuration) {
        val n = configuration.getPi().size() - 1;
        val result = new ArrayList<Pair<String, Configuration>>();

        val twoCycle = Cycle.of(n + 1, n + 2);

        for (val composition : weakCompositions(2, n + 1)) {
            val newPi = Cycle.of(insertSymbols(configuration.getPi().getSymbols(), composition, new int[]{n + 1, n + 2}));

            var newSpi = new MulticyclePermutation(configuration.getSpi());
            newSpi.add(twoCycle);
            result.add(Pair.of(format("added 2-cycle at positions %s", Arrays.toString(composition)), new Configuration(newSpi, newPi)));
        }

        return result.stream();
    }

    private Stream<Pair<String, Configuration>> chain(
            final Function<Configuration, Stream<Pair<String, Configuration>>> op1,
            final Function<Configuration, Stream<Pair<String, Configuration>>> op2,
            final Configuration configuration
    ) {
        return op1.apply(configuration)
                .flatMap(extension -> op2.apply(extension.getRight())
                        .map(pair -> Pair.of(extension.getLeft() + ", " + pair.getLeft(), pair.getRight())))
                .filter(pair -> isValid(configuration, pair.getRight()));
    }

    public List<Configuration> allExtensions(final Configuration configuration) {
        try {
            if (configuration.getSpi().stream().anyMatch(Cycle::isTwoCycle)) {
                SHOULD_NOT_EXTEND_BY_ADDING_TWO_CYCLES.set(true);
            }

            return concatStreams(
                    type1Extensions(configuration).map(Pair::getRight),
                    type2Extensions(configuration).map(Pair::getRight),
                    type3Extensions(configuration).map(Pair::getRight),
                    type4Extensions(configuration).map(Pair::getRight),
                    type5Extensions(configuration).map(Pair::getRight),
                    type6Extensions(configuration).map(Pair::getRight),
                    type7Extensions(configuration).map(Pair::getRight),
                    type8Extensions(configuration).map(Pair::getRight),
                    type9Extensions(configuration).map(Pair::getRight)
            ).toList();
        } finally {
            SHOULD_NOT_EXTEND_BY_ADDING_TWO_CYCLES.set(false);
        }
    }

    @SafeVarargs
    public static <T> Stream<T> concatStreams(final Stream<T>... streams) {
        return Stream.of(streams)
                .flatMap(s -> s);
    }

    protected boolean isValid(final Configuration configuration, final Configuration extension) {
        val isValid = extension.isFull() &&
                extension.getSpi().stream().filter(Cycle::isTwoCycle).count() <= 2 &&
                onlyOneComponent(extension);
        return isValid;
    }

    protected static boolean onlyOneComponent(final Configuration extension) {
        return noTrivialComponent(extension) && (extension.getPi().size() < 5 || (noSingleOriented3CycleComponent(extension) && noIntersecting2CyclesComponent(extension)));
    }

    private static boolean noTrivialComponent(final Configuration extension) {
        return extension.getSpi().stream().noneMatch(Cycle::isTrivial);
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
    public TreeSet<Integer> sortingPivots(final Configuration configuration) {
        return pivots(configuration);
    }

    @Override
    protected void extend(final PivotedConfiguration pivotedConfiguration) {
        val maxSymbol = pivotedConfiguration.getConfiguration().getPi().getMaxSymbol();

        if (maxSymbol > n) {
            n = maxSymbol;
        }

        allExtensions(pivotedConfiguration.getConfiguration())
                .stream()
                .map(Configuration::getSignature)
                .distinct() // de-duplicate extensions
                .map(s -> Configuration.ofSignature(s.getContent())) // canonical computation rely on this instantiation from signature
                .map(extension -> new SortOrExtend(pivotedConfiguration, of(extension, sortingPivots(extension)), storage, minRate))
                .forEach(ForkJoinTask::fork);
    }

    @Override
    protected void compute() {
        enqueued.decrementAndGet();
        super.compute();
    }
}
