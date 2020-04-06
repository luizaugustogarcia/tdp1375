package br.unb.cic.tdp.base;

import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.permutation.PermutationGroups;
import br.unb.cic.tdp.util.Pair;
import br.unb.cic.tdp.util.Triplet;
import cern.colt.list.ByteArrayList;
import cern.colt.list.FloatArrayList;
import lombok.SneakyThrows;
import org.paukov.combinatorics.Factory;
import org.paukov.combinatorics.Generator;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class CommonOperations implements Serializable {

    public static final Cycle[] CANONICAL_PI;
    public static int numberOfCoresToUse = Runtime.getRuntime().availableProcessors();

    static {
        CANONICAL_PI = new Cycle[50];
        for (var i = 1; i < 50; i++) {
            final var pi = new byte[i];
            for (var j = 0; j < i; j++) {
                pi[j] = (byte) j;
            }
            CANONICAL_PI[i] = new Cycle(pi);
        }
    }

    /**
     * Assumes \sigma=(0,1,2,...,n).
     */
    public static Cycle simplify(Cycle pi) {
        FloatArrayList _pi = new FloatArrayList();
        for (int i = 0; i < pi.getSymbols().length; i++) {
            _pi.add(pi.getSymbols()[i]);
        }

        ByteArrayList sigma = new ByteArrayList();
        for (int i = 0; i < _pi.size(); i++) {
            sigma.add((byte) i);
        }

        MulticyclePermutation sigmaPiInverse = PermutationGroups.computeProduct(new Cycle(sigma), pi.getInverse());

        Cycle bigCycle;
        while ((bigCycle = sigmaPiInverse.stream().filter(c -> c.size() > 3).findFirst().orElse(null)) != null) {
            byte leftMostSymbol = leftMostSymbol(bigCycle, pi);
            float newSymbol = _pi.get(_pi.indexOf(leftMostSymbol) - 1) + 0.001F;
            _pi.beforeInsert(_pi.indexOf(bigCycle.pow(leftMostSymbol, -2)), newSymbol);

            FloatArrayList piCopy = new FloatArrayList(Arrays.copyOf(_pi.elements(), _pi.size()));
            piCopy.sort();

            ByteArrayList newPi = new ByteArrayList();
            for (int i = 0; i < piCopy.size(); i++) {
                newPi.add((byte) piCopy.indexOf(_pi.get(i)));
            }

            sigma = new ByteArrayList();
            for (int i = 0; i < newPi.size(); i++) {
                sigma.add((byte) i);
            }

            sigmaPiInverse = PermutationGroups.computeProduct(new Cycle(sigma), new Cycle(newPi).getInverse());

            _pi = new FloatArrayList();
            for (int i = 0; i < newPi.size(); i++) {
                _pi.add(newPi.get(i));
            }
            pi = new Cycle(newPi);
        }

        return pi.getStartingBy((byte) 0);
    }

    private static byte leftMostSymbol(Cycle bigCycle, Cycle pi) {
        for (int i = 1; i < pi.getSymbols().length; i++)
            if (bigCycle.contains(pi.get(i)))
                return pi.get(i);
        return -1;
    }

    public static Cycle applyTransposition(final Cycle pi, final Cycle rho) {
        final var a = rho.get(0);
        final var b = rho.get(1);
        final var c = rho.get(2);

        final var indexes = new int[3];
        for (var i = 0; i < pi.size(); i++) {
            if (pi.get(i) == a)
                indexes[0] = i;
            if (pi.get(i) == b)
                indexes[1] = i;
            if (pi.get(i) == c)
                indexes[2] = i;
        }

        Arrays.sort(indexes);

        final var result = new byte[pi.size()];
        System.arraycopy(pi.getSymbols(), 0, result, 0, indexes[0]);
        System.arraycopy(pi.getSymbols(), indexes[1], result, indexes[0], indexes[2] - indexes[1]);
        System.arraycopy(pi.getSymbols(), indexes[0], result, indexes[0] + (indexes[2] - indexes[1]), indexes[1] - indexes[0]);
        System.arraycopy(pi.getSymbols(), indexes[2], result, indexes[2], pi.size() - indexes[2]);

        return new Cycle(result);
    }

    public static int mod(int a, int b) {
        int r = a % b;
        if (r < 0)
            r += b;
        return r;
    }

    /**
     * Creates an array where the cycles in <code>spi</code> can be accessed by the symbols of <code>pi</code>
     * (being the indexes of the resulting array).
     */
    public static Cycle[] cycleIndex(final List<Cycle> spi, final Cycle pi) {
        final var index = new Cycle[pi.getMaxSymbol() + 1];
        for (final var muCycle : spi) {
            for (final int symbol : muCycle.getSymbols()) {
                index[symbol] = muCycle;
            }
        }
        return index;
    }

    public static boolean isOpenGate(final List<Cycle> cycles, final Cycle piInverse, final Cycle[] cyclesBySymbols,
                                     final int right, final int left) {
        final var gates = left < right ? right - left : piInverse.size() - (left - right);
        for (var i = 1; i < gates; i++) {
            final var index = (i + left) % piInverse.size();
            final var cycle = cyclesBySymbols[piInverse.get(index)];
            if (cycle != null && cycles.contains(cycle))
                return false;
        }
        return true;
    }

    public static Map<Cycle, Integer> openGatesPerCycle(final List<Cycle> cycles, final Cycle piInverse) {
        final var cycleIndex = cycleIndex(cycles, piInverse);

        final Map<Cycle, Integer> result = new HashMap<>();
        for (final var muCycle : cycles) {
            for (var i = 0; i < muCycle.getSymbols().length; i++) {
                final var left = piInverse.indexOf(muCycle.get(i));
                final var right = piInverse.indexOf(muCycle.image(muCycle.get(i)));
                // O(n)
                if (isOpenGate(cycles, piInverse, cycleIndex, right, left)) {
                    if (!result.containsKey(muCycle))
                        result.put(muCycle, 0);
                    result.put(muCycle, result.get(muCycle) + 1);
                }
            }
        }
        return result;
    }

    /**
     * Checks whether or not a given sequence of \rho's is a 11/8-sequence.
     */
    public static boolean is11_8(MulticyclePermutation spi, Cycle pi, final List<Cycle> rhos) {
        final var before = spi.getNumberOfEvenCycles();
        for (final var rho : rhos) {
            if (pi.isApplicable(rho)) {
                pi = applyTransposition(pi, rho);
                spi = PermutationGroups.computeProduct(spi, rho.getInverse());
            } else {
                return false;
            }
        }
        final var after = spi.getNumberOfEvenCycles();
        return after > before && (float) rhos.size() / ((after - before) / 2) <= ((float) 11 / 8);
    }

    public static boolean areSymbolsInCyclicOrder(final Cycle target, byte... symbols) {
        final var symbolIndexes = new byte[target.getMaxSymbol() + 1];

        for (var i = 0; i < target.size(); i++) {
            symbolIndexes[target.get(i)] = (byte) i;
        }

        boolean leap = false;
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

    public static Triplet<MulticyclePermutation, Cycle, List<Cycle>> canonicalize(final MulticyclePermutation spi,
                                                                                  final Cycle pi, final List<Cycle> rhos) {
        var maxSymbol = pi.getMaxSymbol();

        final var substitutionMatrix = new byte[maxSymbol + 1];

        for (var i = 0; i < pi.size(); i++) {
            substitutionMatrix[pi.get(i)] = (byte) i;
        }

        final var _pi = Arrays.copyOf(pi.getSymbols(), pi.size());

        replace(_pi, substitutionMatrix);

        final var _rhos = new ArrayList<Cycle>();
        if (rhos != null) {
            for (final var rho : rhos) {
                final var _rho = Arrays.copyOf(rho.getSymbols(), 3);
                replace(_rho, substitutionMatrix);
                _rhos.add(new Cycle(_rho));
            }
        }

        final var _spi = new MulticyclePermutation();

        for (final var cycle : spi) {
            final var _cycle = Arrays.copyOf(cycle.getSymbols(), cycle.size());
            replace(_cycle, substitutionMatrix);
            _spi.add(new Cycle(_cycle));
        }

        return new Triplet<>(_spi, new Cycle(_pi), _rhos);
    }

    public static void replace(final byte[] array, final byte[] substitutionMatrix) {
        for (var i = 0; i < array.length; i++) {
            array[i] = substitutionMatrix[array[i]];
        }
    }

    /**
     * Find a sorting sequence whose approximation ratio lies between 1 and
     * <code>maxRatio</code>.
     */
    public static List<Cycle> searchForSortingSeq(final Cycle pi, final MulticyclePermutation mu, final Stack<Cycle> rhos,
                                                  final int initialNumberOfEvenCycles, final float maxRatio) {
        return searchForSortingSeq(pi, mu, rhos, initialNumberOfEvenCycles, 1, maxRatio);
    }

    /**
     * Find a sorting sequence whose approximation ratio lies between
     * <code>minRatio</code> and <code>maxRatio</code>.
     */
    private static List<Cycle> searchForSortingSeq(final Cycle pi, final MulticyclePermutation mu, final Stack<Cycle> rhos,
                                                   final int initialNumberOfEvenCycles, final float minRatio,
                                                   final float maxRatio) {
        final var n = pi.size();

        final var lowerBound = (n - mu.getNumberOfEvenCycles()) / 2;
        final var minAchievableRatio = (float) (rhos.size() + lowerBound) / ((n - initialNumberOfEvenCycles) / 2);

        final var spiCycleIndex = cycleIndex(mu, pi);

        // Do not allow it to exceed the max ratio
        if (minAchievableRatio <= maxRatio) {
            final var delta = (mu.getNumberOfEvenCycles() - initialNumberOfEvenCycles);
            final var instantRatio = delta > 0
                    ? (float) (rhos.size() * 2) / (mu.getNumberOfEvenCycles() - initialNumberOfEvenCycles)
                    : 0;
            if (0 < instantRatio && minRatio <= instantRatio && instantRatio <= maxRatio) {
                return rhos;
            } else {
                final var iterator = generateAll0And2Moves(mu, pi).iterator();
                while (iterator.hasNext()) {
                    final var pair = iterator.next();
                    final var rho = pair.getFirst();

                    final var _mu = PermutationGroups.computeProduct(mu, rho.getInverse());
                    rhos.push(rho);
                    final var solution = searchForSortingSeq(applyTransposition(pi, rho),
                            _mu, rhos, initialNumberOfEvenCycles,
                            minRatio, maxRatio);
                    if (!solution.isEmpty()) {
                        return rhos;
                    }
                    rhos.pop();
                }
            }
        }

        return Collections.emptyList();
    }

    /**
     * Search for a 2-move given by an oriented cycle in \spi.
     */
    public static Cycle searchFor2MoveFromOrientedCycle(final MulticyclePermutation spi, final Cycle pi) {
        for (final var cycle : spi.stream().filter(c -> isOriented(pi, c))
                .collect(Collectors.toList())) {
            final var before = cycle.isEven() ? 1 : 0;
            for (var i = 0; i < cycle.size() - 2; i++) {
                for (var j = i + 1; j < cycle.size() - 1; j++) {
                    for (var k = j + 1; k < cycle.size(); k++) {
                        final var a = cycle.get(i);
                        final var b = cycle.get(j);
                        final var c = cycle.get(k);
                        if (pi.isOriented(a, b, c)) {
                            var after = cycle.getK(a, b) % 2 == 1 ? 1 : 0;
                            after += cycle.getK(b, c) % 2 == 1 ? 1 : 0;
                            after += cycle.getK(c, a) % 2 == 1 ? 1 : 0;
                            if (after - before == 2)
                                return new Cycle(a, b, c);
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Generates a stream containing all 0 and 2-moves applicable on \pi.
     */
    public static Stream<Pair<Cycle, Integer>> generateAll0And2Moves(final MulticyclePermutation spi, final Cycle pi) {
        final var ci = cycleIndex(spi, pi);
        final var numberOfEvenCycles = spi.getNumberOfEvenCycles();
        return IntStream.range(0, pi.size() - 2)
                .boxed().filter(i -> ci[pi.get(i)].size() > 1).flatMap(i -> IntStream.range(i + 1, pi.size() - 1)
                        .boxed().filter(j -> ci[pi.get(j)].size() > 1).flatMap(j -> IntStream.range(j + 1, pi.size())
                                .boxed()
                                .takeWhile(k -> !Thread.currentThread().isInterrupted())
                                .filter(k -> ci[pi.get(k)].size() > 1)
                                .filter(k -> {
                                    byte a = pi.get(i), b = pi.get(j), c = pi.get(k);
                                    final var is_2Move = ci[a] != ci[b] && ci[b] != ci[c] && ci[a] != ci[c];
                                    // skip (-2)-moves
                                    return !is_2Move;
                                }).map(k -> {
                                    final var rho = new Cycle(pi.get(i), pi.get(j), pi.get(k));
                                    final var delta = PermutationGroups.computeProduct(spi, rho.getInverse()).getNumberOfEvenCycles() - numberOfEvenCycles;
                                    if (delta >= 0) {
                                        return new Pair<>(rho, delta);
                                    }
                                    return null;
                                }))).filter(p -> p != null);
    }

    public static <T> Generator<T> combinations(final Collection<T> collection, final int k) {
        return Factory.createSimpleCombinationGenerator(Factory.createVector(collection), k);
    }

    /**
     * Search for a 2-move given first by two odd cycles in \spi. If such move is not found, then search in the oriented cycles of \spi.
     */
    public static Cycle searchFor2MoveOddCycles(final MulticyclePermutation spi, final Cycle pi) {
        final var oddCycles = spi.stream().filter(c -> !c.isEven()).collect(Collectors.toList());
        for (final var c1 : oddCycles)
            for (final var c2 : oddCycles)
                if (c1 != c2) {
                    for (final var a : getSegmentsOfLength2(c1))
                        for (final var b : c2.getSymbols()) {
                            for (final var rho : combinations(Arrays.asList(a.get(0), a.get(1), b), 3)) {
                                final var rho1 = new Cycle(rho.getVector().stream().mapToInt(i -> i).toArray());
                                if (pi.isApplicable(rho1)) {
                                    return rho1;
                                }
                            }
                        }
                }

        return searchFor2MoveFromOrientedCycle(spi, pi);
    }

    public static List<Cycle> getSegmentsOfLength2(final Cycle cycle) {
        final List<Cycle> result = new ArrayList<>();
        for (var i = 0; i < cycle.size(); i++) {
            result.add(new Cycle(cycle.get(i), cycle.image(cycle.get(i))));
        }
        return result;
    }

    /**
     * Tells if a <code>cycle</code> is oriented or not having <code>pi</code> as reference.
     */
    public static boolean isOriented(final Cycle pi, final Cycle cycle) {
        return !areSymbolsInCyclicOrder(pi.getInverse(), cycle.getSymbols());
    }

    @SneakyThrows
    public static List<Cycle> searchFor11_8SeqParallel(final MulticyclePermutation spi, final Cycle pi) {
        final var executorService = Executors.newFixedThreadPool(numberOfCoresToUse);
        final var completionService = new ExecutorCompletionService<List<Cycle>>(executorService);

        final var iterator = generateAll0And2Moves(spi, pi).iterator();

        final var submittedTasks = new ArrayList<Future<List<Cycle>>>();
        while (iterator.hasNext()) {
            final var move = iterator.next();
            final var rho = move.getKey();
            final var _partialSorting = new Stack<Cycle>();
            _partialSorting.push(rho);
            submittedTasks.add(completionService.submit(() ->
                    searchForSortingSeq(CommonOperations.applyTransposition(pi, rho),
                            PermutationGroups.computeProduct(spi, rho.getInverse()), _partialSorting,
                            spi.getNumberOfEvenCycles(), 1.375F)));
        }

        executorService.shutdown();

        List<Cycle> sorting = Collections.emptyList();
        for (int i = 0; i < submittedTasks.size(); i++) {
            final var next = completionService.take();
            if (next.get().size() > 1 || next.get().size() == 1 && is11_8(spi, pi, next.get())) {
                sorting = next.get();
                break;
            }
        }

        executorService.shutdownNow();

        return sorting;
    }
}
