package br.unb.cic.tdp.base;

import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.proof.ProofStorage;
import br.unb.cic.tdp.util.VectorizedByteTransposition;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.Pair;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
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


    static final Cache<StateKey, Boolean> NO_SOLUTION_CACHE = Caffeine.newBuilder()
            .maximumSize(1_000_000)
            .build();

    public static void main(String[] args) {
        for (int i = 0; i < 100; i++) {
            val stopWatch = new StopWatch();
            stopWatch.start();
            System.out.println(searchForSorting(null,
                    new Configuration("(0 10 6)(1 8 2 14 3)(4 13)(5 12 9)(7 11)"),
                    1.6, Set.of(10, 11, 12, 13, 14)));
            stopWatch.stop();
            System.out.println("Time taken: " + stopWatch.getTime(TimeUnit.MILLISECONDS) + " ms");
        }

    }

    private static void verify_ijk(List<int[]> sorting, double rate, Configuration configuration, Set<Integer> pivots) {
        var pi = toByteArray(configuration.getPi().getSymbols());
        var spi = configuration.getSpi();

        log.info("Pi0: {}", pi);
        log.info("Spi0: {}\n", Arrays.toString(twoLinesNotation(spi)));

        for (val m : sorting) {
            byte a = pi[m[0]];
            byte b = pi[m[1]];
            byte c = pi[m[2]];
            spi = spi.times(Cycle.of(a, b, c).getInverse());
            log.info("Spi: {}", Arrays.toString(twoLinesNotation(spi)));
            // print pi and spi
            pi = VectorizedByteTransposition.applyTransposition(pi, (byte) m[0], (byte) m[1], (byte) m[2]);
            log.info("Pi: {}", pi);
            log.info("i,j,k: {}", Arrays.toString(m));
            log.info("move: {}", Cycle.of(a, b, c));
            System.out.println();
        }
        val fixedNonPivot = spi.stream()
                .filter(Cycle::isTrivial)
                .map(c -> c.getSymbols()[0])
                .filter(s -> !pivots.contains(s))
                .count();

        if ((fixedNonPivot / (double) sorting.size()) < rate) {
            throw new RuntimeException("Sorting does not meet the rate requirement: " + rate);
        }
    }

    private static void verify(List<Cycle> sorting, double rate, Configuration configuration, Set<Integer> pivots) {
        var pi = toByteArray(configuration.getPi().getSymbols());
        var spi = configuration.getSpi();
        for (val m : sorting) {
            pi = VectorizedByteTransposition.applyTransposition(pi, (byte) ArrayUtils.indexOf(pi, (byte) m.get(0)),
                    (byte) ArrayUtils.indexOf(pi, (byte) m.get(1)),
                    (byte) ArrayUtils.indexOf(pi, (byte) m.get(2)));
            spi = spi.times(m.getInverse());
            // print pi and spi
            log.info("Pi: {}", pi);
            log.info("Spi: {}", Arrays.toString(twoLinesNotation(spi)));
            System.out.println();
        }
        val fixedNonPivot = spi.stream()
                .filter(Cycle::isTrivial)
                .map(c -> c.getSymbols()[0])
                .filter(s -> !pivots.contains(s))
                .count();

        if ((fixedNonPivot / (double) sorting.size()) < rate) {
            throw new RuntimeException("Sorting does not meet the rate requirement: " + rate);
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
                for (val symbol : cycle.getSymbols()) {
                    index[symbol] = cycle;
                }
            }
        });

        return index;
    }

    public static Optional<List<int[]>> searchForSorting(
            final byte[] pivots,
            final int pivotsCount,
            final byte[] spi,
            final byte[] pi,
            final Stack<int[]> stack,
            final double minRate,
            final int maxDepth
    ) {
        final int n = spi.length;
        final int nonPivot = n - pivotsCount;
        final int newStackSize = stack.size() + 1; // depth + 1 for this candidate layer

        // ---- Build masks and baseline fixed count (O(n)) ----
        int npMask = 0;         // bit 1 where NOT a pivot
        int fixedAllMask = 0;   // bit 1 where spi[l] == l (regardless of pivot)
        int fixedNPmask = 0;    // bit 1 where NOT pivot AND spi[l] == l
        for (int l = 0; l < n; ++l) {
            final boolean np = (pivots[l] == 0);
            final boolean fix = (spi[l] == l);
            if (np) npMask |= (1 << l);
            if (fix) fixedAllMask |= (1 << l);
            if (np && fix) fixedNPmask |= (1 << l);
        }
        final int fixed0 = Integer.bitCount(fixedNPmask);

        // These temporaries are intentionally initialized here and reused
        int a, b, c;
        int av = 0, bv = 0, cv = 0;
        int pre = 0, post;
        int fixed, moved;
        int totalMoves = 0, fixedBestCase = 0;
        byte movesLeftBestCase = 0;

        // ---- Optional gating using the last move on the stack (exact same semantics) ----
        if (!stack.isEmpty()) {
            final int[] last = stack.getLast();
            a = last[0];
            b = last[1];
            c = last[2];

            // PRE fixed contribution among {a,b,c} over NON-pivots
            if (((npMask >>> a) & 1) != 0 && av == a) pre++;
            if (((npMask >>> b) & 1) != 0 && bv == b) pre++;
            if (((npMask >>> c) & 1) != 0 && cv == c) pre++;

            // POST after 3-cycle (a b c): spi[a]=cv, spi[b]=av, spi[c]=bv
            post = 0;
            if (((npMask >>> a) & 1) != 0 && cv == a) post++;
            if (((npMask >>> b) & 1) != 0 && av == b) post++;
            if (((npMask >>> c) & 1) != 0 && bv == c) post++;

            fixed = fixed0 + (post - pre);
            moved = nonPivot - fixed;

            // Bound: max remaining moves each adds up to 3 fixed non-pivots
            movesLeftBestCase = (byte) Math.min(maxDepth - newStackSize, (moved + 2) / 3);
            totalMoves = newStackSize + movesLeftBestCase;
            fixedBestCase = Math.min(fixed + 3 * movesLeftBestCase, nonPivot);
        }

        // Proceed if at root OR bound allows it (identical condition)
        if (stack.empty() || fixedBestCase >= (int) Math.ceil(minRate * totalMoves)) {

            // Make a snapshot for the cache key exactly here (same place/semantics)
            final byte[] spiCopy = Arrays.copyOf(spi, spi.length);
            final StateKey key = new StateKey(pi, spiCopy, movesLeftBestCase);

            // Cache look-up only for shallow depths (same condition)
            final boolean useCache = (!stack.isEmpty() && stack.size() <= 3);
            if (useCache && NO_SOLUTION_CACHE.getIfPresent(key) != null) {
                return Optional.empty(); // unfruitful branch, already visited
            }

            // Early-solution threshold for this layer
            final double needEarly = minRate * newStackSize;

            // ---- Enumerate triples ----
            for (byte i = 0; i < pi.length - 2; i++) {
                a = pi[i] & 0xFF;
                if (((fixedAllMask >>> a) & 1) != 0) continue;

                for (byte j = (byte) (i + 1); j < pi.length - 1; j++) {
                    b = pi[j] & 0xFF;
                    if (((fixedAllMask >>> b) & 1) != 0) continue;

                    for (byte k = (byte) (j + 1); k < pi.length; k++) {
                        c = pi[k] & 0xFF;
                        if (((fixedAllMask >>> c) & 1) != 0) continue;

                        // Values currently at those symbols
                        av = spi[a];
                        bv = spi[b];
                        cv = spi[c];

                        // PRE fixed contribution among {a,b,c} over NON-pivots
                        pre = 0;
                        if (((npMask >>> a) & 1) != 0 && av == a) pre++;
                        if (((npMask >>> b) & 1) != 0 && bv == b) pre++;
                        if (((npMask >>> c) & 1) != 0 && cv == c) pre++;

                        // POST after 3-cycle (a b c): spi[a]=cv, spi[b]=av, spi[c]=bv
                        post = 0;
                        if (((npMask >>> a) & 1) != 0 && cv == a) post++;
                        if (((npMask >>> b) & 1) != 0 && av == b) post++;
                        if (((npMask >>> c) & 1) != 0 && bv == c) post++;

                        fixed = fixed0 + (post - pre);
                        moved = nonPivot - fixed;

                        // Early-solution check (avoid division)
                        if (fixed > 0 && fixed >= needEarly) {
                            final double currentRate = fixed / (double) newStackSize;
                            if (currentRate < bestRate) {
                                log.info("Lowest currentRate: {}", currentRate);
                                bestRate = currentRate;
                            }
                            stack.push(new int[]{a, b, c});
                            return Optional.of(stack);
                        }

                        // Bound: max remaining moves each adds up to 3 fixed non-pivots
                        movesLeftBestCase = (byte) Math.min(maxDepth - newStackSize, (moved + 2) / 3);
                        totalMoves = newStackSize + movesLeftBestCase;
                        fixedBestCase = Math.min(fixed + 3 * movesLeftBestCase, nonPivot);

                        if (fixedBestCase >= (int) Math.ceil(minRate * totalMoves)) {
                            // Apply transposition in-place, recurse, then undo (unchanged semantics)
                            spi[a] = (byte) cv;
                            spi[b] = (byte) av;
                            spi[c] = (byte) bv;

                            final byte[] newPi = VectorizedByteTransposition.applyTransposition(pi, i, j, k);
                            stack.push(new int[]{a, b, c});

                            final var sorting = searchForSorting(pivots, pivotsCount, spi, newPi, stack, minRate, maxDepth);
                            if (sorting.isPresent()) {
                                return sorting;
                            }
                            stack.pop();

                            // Undo
                            spi[a] = (byte) av;
                            spi[b] = (byte) bv;
                            spi[c] = (byte) cv;
                        }
                    }
                }
            }

            // Populate cache on failure for shallow depths (identical condition & timing)
            if (useCache) {
                NO_SOLUTION_CACHE.put(key, Boolean.TRUE);
            }
        }

        return Optional.empty();
    }


    private static boolean isStackAt(Stack<int[]> stack, int i, int a, int b, int c) {
        return stack.size() >= i && Cycle.of(stack.get(i - 1)).equals(Cycle.of(a, b, c));
    }

    private static double bestRate = Double.MAX_VALUE;

    public static byte[] toByteArray(int[] input) {
        byte[] result = new byte[input.length];
        for (int i = 0; i < input.length; i++) {
            result[i] = (byte) input[i]; // truncates to lower 8 bits
        }
        return result;
    }

    public static Optional<List<Cycle>> searchForSorting(final ProofStorage proofStorage, final Configuration configuration, final double minRate, final Set<Integer> pivots) {
        val spi = configuration.getSpi();
        val pi = toByteArray(configuration.getPi().getSymbols());

        val pivotsArray = new byte[configuration.getPi().size()];
        for (int i = 0; i < pivotsArray.length; i++) {
            if (pivots.contains(i)) {
                pivotsArray[i] = 1; // mark as pivot
            }
        }

        val twoLinesNotation = toByteArray(twoLinesNotation(spi));
        var sorting = searchForSorting(pivotsArray, pivots.size(), twoLinesNotation, pi, new Stack<>(), minRate, Integer.MAX_VALUE)
//                .or(() -> searchForSorting(pivotsArray, pivots.size(), twoLinesNotation, pi, new Stack<>(), minRate, 3))
//                .or(() -> searchForSorting(pivotsArray, pivots.size(), twoLinesNotation, pi, new Stack<>(), minRate, 5))
//                .or(() -> searchForSorting(pivotsArray, pivots.size(), twoLinesNotation, pi, new Stack<>(), minRate, Integer.MAX_VALUE))
                .map(moves -> moves.stream().map(Cycle::of).toList());
        if (sorting.isPresent()) {
            return sorting;
        }

//        if (configuration.isFull()) {
//            val sigma = spi.times(configuration.getPi());
//            if (sigma.size() == 1 && sigma.asNCycle().size() == configuration.getPi().size()) {
//                sorting = searchForSorting(new byte[configuration.getPi().size()], 0, twoLinesNotation, pi, new Stack<>(), minRate, Integer.MAX_VALUE)
//                        .map(moves -> moves.stream().map(Cycle::of).toList());
//
//                if (sorting.isEmpty()) {
//                    log.error("bad component {}", configuration);
//                } else if (proofStorage != null) {
//                    proofStorage.saveComponentSorting(configuration, sorting.get());
//                }
//                return Optional.empty();
//            }
//        }

        return sorting;
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

    @ToString
    static class StateKey {
        private byte[] pi;
        private byte[] spiCopy;
        private byte movesLeftBestCase;

        public StateKey(byte[] pi, byte[] spiCopy, byte movesLeftBestCase) {
            this.pi = pi;
            this.spiCopy = spiCopy;
            this.movesLeftBestCase = movesLeftBestCase;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof StateKey)) return false;
            StateKey stateKey = (StateKey) o;
            return movesLeftBestCase == stateKey.movesLeftBestCase &&
                    Arrays.equals(pi, stateKey.pi) &&
                    Arrays.equals(spiCopy, stateKey.spiCopy);
        }

        @Override
        public int hashCode() {
            return 31 * (Arrays.hashCode(pi) + Arrays.hashCode(spiCopy) + movesLeftBestCase);
        }
    }
}