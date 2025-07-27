package br.unb.cic.tdp.base;

import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.proof.ProofStorage;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.Pair;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
public class CommonOperations implements Serializable {

    public static final Cycle[] CANONICAL_PI;

    static {
        CANONICAL_PI = new Cycle[2000];
        for (var i = 1; i < 2000; i++) {
            val pi = new int[i];
            for (var j = 0; j < i; j++) {
                pi[j] = j;
            }
            CANONICAL_PI[i] = Cycle.of(pi);
        }
    }

    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) {
            val stopWatch = new StopWatch();
            stopWatch.start();
            System.out.println(searchForSorting(null,
                    new Configuration("(0 10 6)(1 8 2 14 3)(4 13)(5 12 9)(7 11)"),
                    1.6, Set.of(10, 11, 12, 13, 14)));
            stopWatch.stop();
            System.out.println("Time taken: " + stopWatch.getTime(TimeUnit.MILLISECONDS) + " ms");
        }
    }

    public static int mod(final int a, final int b) {
        var r = a % b;
        if (r < 0) r += b;
        return r;
    }

    /**
     * Creates an array where the cycles in <code>bigGamma</code> can be accessed by the symbols of <code>pi</code>
     * (being the indexes of the resulting array).
     */
    public static Cycle[] cycleIndex(final Collection<Cycle> bigGamma, final Cycle pi) {
        return cyclesIndex(List.of(bigGamma), pi);
    }

    public static Cycle[] cyclesIndex(final List<Collection<Cycle>> components, final Cycle pi) {
        val index = new Cycle[pi.getMaxSymbol() + 1];

        components.forEach(component -> {
            for (val cycle : component) {
                for (final int symbol : cycle.getSymbols()) {
                    index[symbol] = cycle;
                }
            }
        });

        return index;
    }

    public static boolean areSymbolsInCyclicOrder(final Cycle cycle, int... symbols) {
        val symbolIndexes = cycle.getSymbolIndexes();

        var leap = false;
        for (int i = 0; i < symbols.length; i++) {
            if (symbolIndexes[symbols[i]] > symbolIndexes[symbols[(i + 1) % symbols.length]]) {
                if (!leap) {
                    leap = true;
                } else {
                    return false;
                }
            }
        }

        return true;
    }

    public static Optional<List<int[]>> searchForSorting(
            final Configuration initialConfiguration,
            final int[] pivots,
            final int pivotsCount,
            final int[] spi,
            final int[] pi,
            final Stack<int[]> stack,
            final double minRate,
            final AtomicBoolean cancelRequested,
            final int maxDepth
    ) {
        if (cancelRequested.get()) {
            log.info("Search cancelled for " + initialConfiguration.getSpi() + "#" + pivots);
            throw new RuntimeException("Thread interrupted, returning empty optional");
        }

        for (var i = 0; i < pi.length - 2; i++) {
            if (spi[pi[i]] != pi[i]) {
                for (var j = i + 1; j < pi.length - 1; j++) {
                    if (spi[pi[j]] != pi[j]) {
                        for (var k = j + 1; k < pi.length; k++) {
                            if (spi[pi[k]] != pi[k]) {
                                int a = pi[i], b = pi[j], c = pi[k];

                                int[] m = {a, b, c};

                                val newSpi = times(spi, m[0], m[1], m[2]);
                                val newPi = applyTranspositionOptimized(pi, m);

                                var fixedSymbolsWithoutPivots = 0;
                                var movedSymbolsWithoutPivots = 0;
                                for (int l = 0; l < newSpi.length; l++) {
                                    if (pivots[l] == 0) {
                                        if (l == newSpi[l]) {
                                            fixedSymbolsWithoutPivots++;
                                        } else {
                                            movedSymbolsWithoutPivots++;
                                        }
                                    }
                                }

                                val newStackSize = stack.size() + 1;

                                val currentRate = (fixedSymbolsWithoutPivots) / (double) newStackSize;
                                if (fixedSymbolsWithoutPivots > 0) {
                                    if (currentRate >= minRate) {
                                        if (currentRate < bestRate) {
                                            log.info("Lowest currentRate: {}", currentRate);
                                            bestRate = currentRate;
                                        }
                                        stack.push(m);
                                        return Optional.of(stack);
                                    }
                                }

                                val movesLeftBestCase = Math.min(maxDepth - newStackSize, Math.ceil(movedSymbolsWithoutPivots / 3.0)); // each move can add up to 3 adjacencies
                                val totalMoves = newStackSize + movesLeftBestCase;
                                val fixedSymbolsBestCase = Math.min(fixedSymbolsWithoutPivots + (movesLeftBestCase * 3), initialConfiguration.getSpi().getNumberOfSymbols() - pivotsCount);

                                if (fixedSymbolsBestCase >= totalMoves * minRate) {
                                    stack.push(m);
                                    val sorting = searchForSorting(initialConfiguration, pivots, pivotsCount, newSpi, newPi, stack, minRate, cancelRequested, maxDepth);
                                    if (sorting.isPresent()) {
                                        return sorting;
                                    }
                                    stack.pop();
                                }
                            }
                        }
                    }
                }
            }
        }

        return Optional.empty();
    }

    private static int[] times(final int[] spi, final int a, final int b, final int c) {
        val result = new int[spi.length];
        for (var i = 0; i < result.length; i++) {
            var newIndex = i;
            if (i == a) newIndex = c;
            else if (i == b) newIndex = a;
            else if (i == c) newIndex = b;
            result[i] = spi[newIndex];
        }
        return result;
    }

    public static int[] applyTranspositionOptimized(final int[] pi, final int[] move) {
        val a = move[0];
        val b = move[1];
        val c = move[2];

        val indexes = new int[3];
        for (var i = 0; i < pi.length; i++) {
            if (pi[i] == a) indexes[0] = i;
            if (pi[i] == b) indexes[1] = i;
            if (pi[i] == c) indexes[2] = i;
        }

        Arrays.sort(indexes);

        val result = new int[pi.length];
        System.arraycopy(pi, 0, result, 0, indexes[0]);
        System.arraycopy(pi, indexes[1], result, indexes[0], indexes[2] - indexes[1]);
        System.arraycopy(pi, indexes[0], result, indexes[0] + (indexes[2] - indexes[1]), indexes[1] - indexes[0]);
        System.arraycopy(pi, indexes[2], result, indexes[2], pi.length - indexes[2]);

        return result;
    }

    private static double bestRate = Double.MAX_VALUE;

    public static Optional<List<Cycle>> searchForSorting(final ProofStorage proofStorage, final Configuration configuration, final double minRate, final Set<Integer> pivots) {
        val spi = configuration.getSpi();
        val pi = configuration.getPi().getSymbols();

        val pivotsArray = new int[configuration.getPi().size()];
        for (int i = 0; i < pivotsArray.length; i++) {
            if (pivots.contains(i)) {
                pivotsArray[i] = 1; // mark as pivot
            }
        }

        var sorting = searchForSorting(configuration, pivotsArray, pivots.size(), twoLinesNotation(spi), pi, new Stack<>(), minRate, new AtomicBoolean(), 1)
                .or(() -> searchForSorting(configuration, pivotsArray, pivots.size(), twoLinesNotation(spi), pi, new Stack<>(), minRate, new AtomicBoolean(), 3))
                .or(() -> searchForSorting(configuration, pivotsArray, pivots.size(), twoLinesNotation(spi), pi, new Stack<>(), minRate, new AtomicBoolean(), 5))
                .or(() -> searchForSorting(configuration, pivotsArray, pivots.size(), twoLinesNotation(spi), pi, new Stack<>(), minRate, new AtomicBoolean(), Integer.MAX_VALUE))
                .map(moves -> moves.stream().map(Cycle::of).toList());
        if (sorting.isPresent()) {
            return sorting;
        }

        if (configuration.isFull()) {
            val sigma = spi.times(configuration.getPi());
            if (sigma.size() == 1 && sigma.asNCycle().size() == configuration.getPi().size()) {
                sorting = searchForSorting(configuration, new int[configuration.getPi().size()], 0, twoLinesNotation(spi), pi, new Stack<>(), minRate, new AtomicBoolean(), Integer.MAX_VALUE)
                        .map(moves -> moves.stream().map(Cycle::of).toList());

                if (sorting.isEmpty()) {
                    log.error("bad component {}", configuration);
                } else if (proofStorage != null) {
                    proofStorage.saveComponentSorting(configuration, sorting.get());
                }
                return Optional.empty();
            }
        }

        return sorting;
    }

    public static Optional<List<Cycle>> lookFor2Move(final Configuration configuration, final Set<Integer> pivots) {
        val pi = configuration.getPi().getSymbols();

        for (var i = 0; i < pi.length - 2; i++) {
            for (var j = i + 1; j < pi.length - 1; j++) {
                for (var k = j + 1; k < pi.length; k++) {
                    int a = pi[i], b = pi[j], c = pi[k];

                    val move = Cycle.of(a, b, c);
                    val spi = configuration.getSpi().times(move.getInverse());
                    if (spi.stream().filter(cycle -> cycle.size() == 1 && !pivots.contains(cycle.get(0))).count() == 2) {
                        return Optional.of(List.of(move));
                    }
                }
            }
        }

        return Optional.empty();
    }

    public static int[] twoLinesNotation(final MulticyclePermutation spi) {
        val result = new int[spi.getNumberOfSymbols()];
        for (var i = 0; i < result.length; i++) {
            result[i] = spi.image(i);
        }
        return result;
    }

    public static TreeSet<Integer> pivots(final Configuration configuration) {
        return configuration.getSpi().stream()
                .map(cycle -> rightMostSymbol(cycle, configuration.getPi()))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    // zero is the left most
    public static Integer rightMostSymbol(final Cycle cycle, final Cycle pi) {
        val canonicalPi = pi.startingBy(0);
        return Arrays.stream(cycle.getSymbols())
                .boxed()
                .map(s -> Pair.of(s, canonicalPi.indexOf(s)))
                .max(Comparator.comparing(Pair::getRight)).get().getLeft();
    }
}