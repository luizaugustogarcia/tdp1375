package br.unb.cic.tdp.base;

import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.proof.ProofStorage;
import br.unb.cic.tdp.util.Transpositions;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.Pair;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;
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
        for (int i = 0; i < 100; i++) {
            val stopWatch = new StopWatch();
            stopWatch.start();
            System.out.println(searchForSorting(null,
                    new Configuration("(0 10 2 13 5)(1 11 4)(3 15 6)(7 14 12)(8 17)(9 16)"),
                    1.66, Set.of(0, 11, 14, 15, 16, 17)));
            stopWatch.stop();
            System.out.println("Time taken: " + stopWatch.getTime(TimeUnit.MILLISECONDS) + " ms");
        }
    }

    private static Optional<List<int[]>> verify_ijk(short[][] sorting, double rate, Configuration configuration, Set<Integer> pivots) {
        var _pi = configuration.getPi();
        var pi = toShortArray(_pi.getSymbols());
        var spi = configuration.getSpi();

        val result = new ArrayList<int[]>(sorting.length);

        for (val m : sorting) {
            val a = pi[m[0]];
            val b = pi[m[1]];
            val c = pi[m[2]];
            val move = Cycle.of(a, b, c);
            spi = spi.times(move.getInverse());
            try {
                pi = Transpositions.apply(pi, (byte) m[0], (byte) m[1], (byte) m[2]);
            } catch (ArrayIndexOutOfBoundsException e) {
                throw e;
            }
            _pi = move.times(_pi).asNCycle();
            result.add(new int[]{a, b, c});
        }

        val fixedNonPivot = spi.stream()
                .filter(Cycle::isTrivial)
                .map(c -> c.getSymbols()[0])
                .filter(s -> !pivots.contains(s))
                .count();

        if ((fixedNonPivot / (double) sorting.length) < rate) {
            throw new RuntimeException("Sorting does not meet the rate requirement: " + rate);
        }

        return result.isEmpty() ? Optional.empty() : Optional.of(result);
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

    public static Optional<List<int[]>> searchForSorting(
            final short[] pivots,
            final int pivotsCount,
            final short[] spi,
            final short[] pi,
            final Stack<int[]> stack,
            final double minRate,
            final int maxDepth,
            final StopWatch stopWatch,
            final int maxTimeMillis
    ) {
        if (stopWatch.getTime() > maxTimeMillis) {
            return Optional.empty();
        }
        val n = spi.length;
        val nonPivot = n - pivotsCount;
        val newStackSize = stack.size() + 1;  // depth + 1 for this candidate layer

        // Build masks and baseline fixed count ONCE (O(n))
        var npMask = 0;         // 1 where NOT a pivot
        var fixedAllMask = 0;   // 1 where spi[l] == l (regardless of pivot)
        var fixedNPmask = 0;    // 1 where NOT pivot AND spi[l] == l
        for (int l = 0; l < n; ++l) {
            final boolean np = (pivots[l] == 0);
            final boolean fix = (spi[l] == l);
            if (np) npMask |= (1 << l);
            if (fix) fixedAllMask |= (1 << l);
            if (np && fix) fixedNPmask |= (1 << l);
        }
        val fixed0 = Integer.bitCount(fixedNPmask);

        val needEarly = minRate * newStackSize;

        for (byte i = 0; i < pi.length - 2; i++) {
            val a = pi[i] & 0xFF;
            if (((fixedAllMask >>> a) & 1) != 0) continue;

            for (byte j = (byte) (i + 1); j < pi.length - 1; j++) {
                val b = pi[j] & 0xFF;
                if (((fixedAllMask >>> b) & 1) != 0) continue;

                for (byte k = (byte) (j + 1); k < pi.length; k++) {
                    val c = pi[k] & 0xFF;
                    if (((fixedAllMask >>> c) & 1) != 0) continue;

                    // Values currently at those symbols
                    val av = spi[a];
                    val bv = spi[b];
                    val cv = spi[c];

                    // PRE fixed contribution among {a,b,c} over NON-pivots
                    var pre = 0;
                    if (((npMask >>> a) & 1) != 0 && av == a) pre++;
                    if (((npMask >>> b) & 1) != 0 && bv == b) pre++;
                    if (((npMask >>> c) & 1) != 0 && cv == c) pre++;

                    // POST after 3-cycle (a b c): spi[a]=cv, spi[b]=av, spi[c]=bv
                    var post = 0;
                    if (((npMask >>> a) & 1) != 0 && cv == a) post++;
                    if (((npMask >>> b) & 1) != 0 && av == b) post++;
                    if (((npMask >>> c) & 1) != 0 && bv == c) post++;

                    val fixed = fixed0 + (post - pre);
                    val moved = nonPivot - fixed;

                    // Early-solution check: avoid division
                    if (fixed > 0 && fixed >= needEarly) {
                        val currentRate = fixed / (double) newStackSize;
                        if (currentRate < bestRate) {
                            log.info("Lowest currentRate: {}", currentRate);
                            bestRate = currentRate;
                        }
                        stack.push(new int[]{a, b, c});
                        return Optional.of(stack);
                    }

                    // Bound: max remaining moves each adds up to 3 fixed non-pivots
                    val movesLeftBestCase = Math.min(maxDepth - newStackSize, (moved + 2) / 3);
                    val totalMoves = newStackSize + movesLeftBestCase;
                    val fixedBestCase = Math.min(fixed + 3 * movesLeftBestCase, nonPivot);

                    if (fixedBestCase >= (int) Math.ceil(minRate * totalMoves)) {
                        // Proceed: actually apply the transposition only now
                        // mutate spi in-place for recursion, and undo on backtrack
                        // Save originals (we already have av,bv,cv)
                        spi[a] = cv;
                        spi[b] = av;
                        spi[c] = bv;

                        val newPi = Transpositions.apply(pi, i, j, k);
                        stack.push(new int[]{a, b, c});
                        val sorting = searchForSorting(pivots, pivotsCount, spi, newPi, stack, minRate, maxDepth, stopWatch, maxTimeMillis);
                        if (sorting.isPresent()) {
                            return sorting;
                        }
                        stack.pop();

                        // Undo side effect
                        spi[a] = av;
                        spi[b] = bv;
                        spi[c] = cv;
                    }
                }
            }
        }

        return Optional.empty();
    }

    private static double bestRate = Double.MAX_VALUE;

    public static short[] toShortArray(int[] input) {
        short[] result = new short[input.length];
        for (int i = 0; i < input.length; i++) {
            result[i] = (short) input[i]; // truncates to lower 8 bits
        }
        return result;
    }

    public static Optional<List<Cycle>> searchForSorting(final ProofStorage proofStorage, final Configuration configuration, final double minRate, final Set<Integer> pivots) {
        val spi = configuration.getSpi();
        val pi = toShortArray(configuration.getPi().getSymbols());

        val pivs = new short[configuration.getPi().size()];
        for (int i = 0; i < pivs.length; i++) {
            if (pivots.contains(i)) {
                pivs[i] = 1; // mark as pivot
            }
        }

        val twoLinesNotation = twoLinesNotation(spi);

        val maxDepth = minRate == 1.6 ? 5 : 6;
        var sorting = searchForSorting(pivs, pivots.size(), twoLinesNotation, pi, new Stack<>(), minRate, 1, new StopWatch(), 100)
                .or(() -> searchForSorting(pivs, pivots.size(), twoLinesNotation, pi, new Stack<>(), minRate, 3, new StopWatch(), 100))
                .or(() -> verify_ijk(GPUSorter.syncSort(pi, pivs, twoLinesNotation, minRate, maxDepth), minRate, configuration, pivots))
                .map(moves -> moves.stream().map(Cycle::of).toList());
        if (sorting.isPresent()) {
            return sorting;
        }

        if (configuration.isFull()) {
            val sigma = spi.times(configuration.getPi());
            if (sigma.size() == 1 && sigma.asNCycle().size() == configuration.getPi().size()) {
                sorting = searchForSorting(new short[configuration.getPi().size()], 0, twoLinesNotation, pi, new Stack<>(), minRate, Integer.MAX_VALUE, new StopWatch(), Integer.MAX_VALUE)
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

    public static short[] twoLinesNotation(final MulticyclePermutation spi) {
        val result = new short[spi.getNumberOfSymbols()];
        for (var i = 0; i < result.length; i++) {
            result[i] = (short) spi.image(i);
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