package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.Configuration;
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
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static br.unb.cic.tdp.base.CommonOperations.pivots;
import static br.unb.cic.tdp.util.CircularMatcher.isUnorderedSubsequence;
import static br.unb.cic.tdp.util.SymbolInserter.insertSymbols;
import static br.unb.cic.tdp.util.WeakCompositions.weakCompositions;
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

    // optimize by checking if the multiplying the 2-cycles of each step we obtain the same product, and avoiding generating the same product
    // optimize by caching isValid results

    public Stream<Pair<String, Configuration>> type1Extensions(final Configuration configuration) {
        return add2Cycle(configuration)
                .flatMap(extension -> add2Cycle(extension.getRight())
                        .map(pair -> Pair.of(extension.getLeft() + ", " + pair.getLeft(), pair.getRight())))
                .filter(pair -> isValid(configuration, pair.getRight()));
    }

    public Stream<Pair<String, Configuration>> type2Extensions(final Configuration configuration) {
        return add2Cycle(configuration)
                .flatMap(extension -> growOneCycle(extension.getRight())
                        .map(pair -> Pair.of(extension.getLeft() + ", " + pair.getLeft(), pair.getRight())))
                .filter(pair -> isValid(configuration, pair.getRight()));
    }

    public Stream<Pair<String, Configuration>> type3Extensions(final Configuration configuration) {
        return add2Cycle(configuration)
                .flatMap(extension -> mergeTwoCycles(extension.getRight())
                        .map(pair -> Pair.of(extension.getLeft() + ", " + pair.getLeft(), pair.getRight())))
                .filter(pair -> isValid(configuration, pair.getRight()));
    }

    public Stream<Pair<String, Configuration>> type4Extensions(final Configuration configuration) {
        return growOneCycle(configuration)
                .flatMap(extension -> add2Cycle(extension.getRight())
                        .map(pair -> Pair.of(extension.getLeft() + ", " + pair.getLeft(), pair.getRight())))
                .filter(pair -> isValid(configuration, pair.getRight()));
    }

    public Stream<Pair<String, Configuration>> type5Extensions(final Configuration configuration) {
        return growOneCycle(configuration)
                .flatMap(extension -> growOneCycle(extension.getRight())
                        .map(pair -> Pair.of(extension.getLeft() + ", " + pair.getLeft(), pair.getRight())))
                .filter(pair -> isValid(configuration, pair.getRight()));
    }

    public Stream<Pair<String, Configuration>> type6Extensions(final Configuration configuration) {
        return growOneCycle(configuration)
                .flatMap(extension -> mergeTwoCycles(extension.getRight())
                        .map(pair -> Pair.of(extension.getLeft() + ", " + pair.getLeft(), pair.getRight())))
                .filter(pair -> isValid(configuration, pair.getRight()));
    }

    public Stream<Pair<String, Configuration>> type7Extensions(final Configuration configuration) {
        return mergeTwoCycles(configuration)
                .flatMap(extension -> add2Cycle(extension.getRight())
                        .map(pair -> Pair.of(extension.getLeft() + ", " + pair.getLeft(), pair.getRight())))
                .filter(pair -> isValid(configuration, pair.getRight()));
    }

    public Stream<Pair<String, Configuration>> type8Extensions(final Configuration configuration) {
        return mergeTwoCycles(configuration)
                .flatMap(extension -> growOneCycle(extension.getRight())
                        .map(pair -> Pair.of(extension.getLeft() + ", " + pair.getLeft(), pair.getRight())))
                .filter(pair -> isValid(configuration, pair.getRight()));
    }

    public Stream<Pair<String, Configuration>> type9Extensions(final Configuration configuration) {
        return mergeTwoCycles(configuration)
                .flatMap(extension -> mergeTwoCycles(extension.getRight())
                        .map(pair -> Pair.of(extension.getLeft() + ", " + pair.getLeft(), pair.getRight())))
                .filter(pair -> isValid(configuration, pair.getRight()));
    }

    public Stream<Pair<String, Configuration>> mergeTwoCycles(final Configuration configuration) {
        return Generator.combination(configuration.getSpi())
                .simple(2)
                .stream()
                .flatMap(pair -> IntStream.of(pair.get(0).getSymbols())
                        .boxed()
                        .flatMap(a -> IntStream.of(pair.get(1).getSymbols())
                                .boxed()
                                .map(b -> Pair.of(format("joined=[%s, %s], a=%d, b=%d", pair.get(0), pair.get(1), a, b), new Configuration(configuration.getSpi().times(Cycle.of(a, b)), configuration.getPi())))));
    }

    public Stream<Pair<String, Configuration>> growOneCycle(final Configuration configuration) {
        val n = configuration.getPi().size() - 1;
        val result = new ArrayList<Pair<String, Configuration>>();

        for (val composition : weakCompositions(1, n + 1)) {
            val newPi = Cycle.of(insertSymbols(configuration.getPi().getSymbols(), composition, new int[]{n + 1}));

            for (val cycle : configuration.getSpi()) {
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

    public Stream<Pair<String, Configuration>> add2Cycle(final Configuration configuration) {
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

    public Stream<Configuration> extensions(final Configuration configuration) {
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
        );
    }

    @SafeVarargs
    public static <T> Stream<T> concatStreams(final Stream<T>... streams) {
        return Stream.of(streams)
                .flatMap(s -> s);
    }

    protected boolean isValid(final Configuration configuration, final Configuration extension) {
        return extension.isFull() &&
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

    public static void main(String[] args) {
        val configuration = new Configuration("(0 1)(2 3)");
        val sortOrExtend = new SortOrExtend(null, Pair.of(configuration, Set.of()), null, 0.0);

        System.out.println("Type 1 Extensions:");
        sortOrExtend.type1Extensions(configuration).forEach(pair -> System.out.println(pair.getLeft() + " -> " + pair.getRight()));

        System.out.println("Type 2 Extensions:");
        sortOrExtend.type2Extensions(configuration).forEach(pair -> System.out.println(pair.getLeft() + " -> " + pair.getRight()));
    }
}
