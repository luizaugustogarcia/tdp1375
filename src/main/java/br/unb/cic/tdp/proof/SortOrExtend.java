package br.unb.cic.tdp.proof;

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

import static br.unb.cic.tdp.base.CommonOperations.*;
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
    public List<Pair<String, Configuration>> type1Extensions(final Configuration configuration) {
        return type1SubExtensions(configuration).stream()
                .flatMap(firstPair -> type1SubExtensions(firstPair.getRight()).stream()
                        .map(secondPair -> Pair.of("%s; %s".formatted(firstPair.getLeft(), secondPair.getLeft()), secondPair.getRight())))
                .filter(extension -> isValid(configuration, extension.getRight()))
                .toList();
    }

    private List<Pair<String, Configuration>> type1SubExtensions(final Configuration configuration) {
        val result = new ArrayList<Pair<String, Configuration>>();

        val n = configuration.getPi().getSymbols().length;

        {
            // adds a new 2-cycle, introducing two new symbols
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
                for (val symbol : cycle.getSymbols()) {
                    val cycle_ = Arrays.stream(cycle.startingBy(symbol).getSymbols())
                            .boxed()
                            .map(Object::toString)
                            .collect(Collectors.joining(" "));

                    val newCycle = format("(%s %d)", cycle_, n);
                    val newSpi = configuration.getSpi().toString().replace(cycle.toString(), newCycle);
                    for (var a = 0; a <= n; a++) {
                        val extendedPi = unorientedExtension(configuration.getPi().getSymbols(), n, a).elements();
                        val extension = new Configuration(new MulticyclePermutation(newSpi), Cycle.of(extendedPi));
                        result.add(Pair.of(format("grown cycle=%s, pos a=%d", cycle, a), extension));
                    }
                }
            }
        }

        {
            // joins two existing cycles
            Generator.combination(configuration.getSpi())
                    .simple(2)
                    .forEach(pair -> {
                        for (val a : pair.get(0).getSymbols()) {
                            for (val b : pair.get(1).getSymbols()) {
                                val extension = new Configuration(configuration.getSpi().times(Cycle.of(a, b)), configuration.getPi());
                                result.add(Pair.of(format("joined=[%s, %s], a=%d, b=%d", pair.get(0), pair.get(1), a, b), extension));
                            }
                        }
                    });
        }

        return result;
    }

    public Stream<Configuration> extensions(final Configuration configuration) {
        return type1Extensions(configuration).stream().map(Pair::getRight);
    }

    protected boolean isValid(final Configuration configuration, final Configuration extension) {
        val numOf2Cycles = configuration.getSpi().stream().filter(Cycle::isTwoCycle).count();
        val thereWasNo2CyclesOrTheNumberDecreased = numOf2Cycles == 0 || extension.getSpi().stream().noneMatch(Cycle::isTwoCycle);

        val numOfOpenGates = configuration.openGates().count();
        val wasFullOrNumOfOpenGatesDecreased = numOfOpenGates == 0 || extension.openGates().count() < numOfOpenGates;

        return extension.openGates().count() <= 2 && thereWasNo2CyclesOrTheNumberDecreased && wasFullOrNumOfOpenGatesDecreased;
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
        System.out.println(new Configuration("(7 2 0)(5 3 1)(8 6 4)").hashCode());
    }
}
