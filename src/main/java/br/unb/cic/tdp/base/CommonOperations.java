package br.unb.cic.tdp.base;

import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.permutation.PermutationGroups;
import br.unb.cic.tdp.util.Triplet;
import cern.colt.list.ByteArrayList;
import cern.colt.list.FloatArrayList;
import com.google.common.primitives.Bytes;
import org.apache.commons.math3.util.Pair;
import org.paukov.combinatorics.Factory;
import org.paukov.combinatorics.Generator;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static br.unb.cic.tdp.util.ByteArrayOperations.removeSymbol;
import static br.unb.cic.tdp.util.ByteArrayOperations.replace;

public class CommonOperations {

    public static final Cycle[] CANONICAL_PI;

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
/*

    public static void main(String[] args) {
        final var conf1 = new Configuration(new MulticyclePermutation("(0,3,1,2,4)"), new Cycle("(0,1,2,3,4)"));
        System.out.println(Arrays.toString(signature(conf1.getSpi(), conf1.getPi())));
        final var conf2 = new Configuration(new MulticyclePermutation("(0,2,4,3,1)"), new Cycle("(0,4,3,2,1)"));
        System.out.println(Arrays.toString(signature(conf2.getSpi(), conf2.getPi())));
        System.out.println(conf1.equals(conf2));
    }
*/

    public static Cycle simplify(Cycle pi) {
        var _pi = new FloatArrayList();
        for (var i = 0; i < pi.getSymbols().length; i++) {
            _pi.add(pi.getSymbols()[i]);
        }

        var sigma = CANONICAL_PI[_pi.size()];

        var spi = PermutationGroups.computeProduct(sigma, pi.getInverse());

        Cycle bigCycle;
        while ((bigCycle = spi.stream().filter(c -> c.size() > 3).findFirst().orElse(null)) != null) {
            final var leftMostSymbol = leftMostSymbol(pi, bigCycle);
            final var newSymbol = _pi.get(_pi.indexOf(leftMostSymbol) - 1) + 0.001F;
            _pi.beforeInsert(_pi.indexOf(bigCycle.pow(leftMostSymbol, -2)), newSymbol);

            final var piCopy = new FloatArrayList(Arrays.copyOf(_pi.elements(), _pi.size()));
            piCopy.sort();

            final var newPi = new ByteArrayList();
            for (var i = 0; i < piCopy.size(); i++) {
                newPi.add((byte) piCopy.indexOf(_pi.get(i)));
            }

            sigma = CANONICAL_PI[newPi.size()];

            spi = PermutationGroups.computeProduct(sigma, new Cycle(newPi).getInverse());

            _pi = new FloatArrayList();
            for (var i = 0; i < newPi.size(); i++) {
                _pi.add(newPi.get(i));
            }
            pi = new Cycle(newPi);
        }

        return pi.getStartingBy((byte) 0);
    }

    private static byte leftMostSymbol(final Cycle pi, final Cycle bigCycle) {
        for (var i = 1; i < pi.getSymbols().length; i++)
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
    public static Cycle[] createCycleIndex(final List<Cycle> spi, final Cycle pi) {
        final var index = new Cycle[pi.size()];
        for (final var muCycle : spi) {
            for (final int symbol : muCycle.getSymbols()) {
                index[symbol] = muCycle;
            }
        }
        return index;
    }
/*
    public static float[] signature(final List<Cycle> spi, final Cycle pi) {
        final var labelByCycle = new HashMap<Cycle, Float>();
        final var cycleIndex = createCycleIndex(spi, pi);
        final var orientedCycles = spi.stream().filter(c -> !areSymbolsInCyclicOrder(c.getSymbols(), pi.getInverse().getSymbols()))
                .collect(Collectors.toSet());
        final var symbolIndexByOrientedCycle = new HashMap<Cycle, byte[]>();

        final var signature = new float[pi.size()];

        for (var i = 0; i < signature.length; i++) {
            final int symbol = pi.get(i);
            final var cycle = cycleIndex[symbol];
            if (orientedCycles.contains(cycle)) {
                symbolIndexByOrientedCycle.computeIfAbsent(cycle, c -> {
                    final var symbolIndex = new byte[pi.size()];
                    for (int j = 0; j < c.size(); j++) {
                        symbolIndex[c.get(j)] = (byte) (j + 1);
                    }
                    return symbolIndex;
                });
            }
            labelByCycle.computeIfAbsent(cycle, c -> (float) (labelByCycle.size() + 1));
            signature[i] = orientedCycles.contains(cycle) ?
                    labelByCycle.get(cycle) + (float) symbolIndexByOrientedCycle.get(cycle)[symbol] / 10 : labelByCycle.get(cycle);
        }

        return signature;
    }*/

    /**
     * Performs a join operation, producing new \spi, \pi and \rhos.
     */
    public static Triplet<MulticyclePermutation, Cycle, List<Cycle>> join(final MulticyclePermutation spi,
                                                                          final Cycle pi, final List<Cycle> rhos,
                                                                          final byte[] joinPair) {
        final var cycleIndex = createCycleIndex(spi, pi);

        final var _spi = new MulticyclePermutation(spi);

        var a = cycleIndex[joinPair[0]];
        var b = cycleIndex[joinPair[1]];

        a = a.getInverse().getStartingBy(a.getInverse().image(joinPair[0]));
        b = b.getInverse().getStartingBy(joinPair[1]);

        final var cSymbols = new byte[a.size() + b.size() - 1];
        System.arraycopy(a.getSymbols(), 0, cSymbols, 0, a.size());
        System.arraycopy(b.getSymbols(), 1, cSymbols, a.size(), b.size() - 1);

        final var c = new Cycle(cSymbols);
        _spi.add(c.getInverse());
        _spi.remove(cycleIndex[joinPair[0]]);
        _spi.remove(cycleIndex[joinPair[1]]);

        final var _rhos = new ArrayList<Cycle>();
        var _pi = pi.getSymbols();
        for (final var rho : rhos) {
            final var __pi = applyTransposition(new Cycle(_pi), rho).getSymbols();
            final var _rho = PermutationGroups.computeProduct(false,
                    new Cycle(replace(removeSymbol(__pi, joinPair[0]), joinPair[1], joinPair[0])),
                    new Cycle(replace(removeSymbol(_pi, joinPair[0]), joinPair[1], joinPair[0])).getInverse());

            _pi = __pi;
            // sometimes _rho = (),
            if (_rho.size() != 0)
                _rhos.add(_rho.asNCycle());
        }

        _pi = removeSymbol(pi.getSymbols(), joinPair[1]);

        return new Triplet<>(_spi, new Cycle(_pi), _rhos);
    }


    public static List<byte[]> getJoinPairs(final List<Cycle> cycles, final Cycle pi) {
        final var symbols = new HashSet<>(Bytes.asList(pi.getSymbols()));

        final var symbolToLabel = new HashMap<Byte, Byte>();

        for (var i = 0; i < cycles.size(); i++) {
            for (var j = 0; j < cycles.get(i).size(); j++) {
                final var symbol = cycles.get(i).getSymbols()[j];
                symbolToLabel.put(symbol, (byte) i);
                symbols.remove(symbol);
            }
        }

        final var _pi = new ByteArrayList(Arrays.copyOf(pi.getSymbols(), pi.size()));
        _pi.removeAll(new ByteArrayList(Bytes.toArray(symbols)));

        final var results = new ArrayList<byte[]>();
        for (var i = 0; i < _pi.size(); i++) {
            final var currentLabel = symbolToLabel.get(_pi.get(i));
            final var nextLabel = symbolToLabel.get(_pi.get((i + 1) % _pi.size()));
            if (!currentLabel.equals(nextLabel) && (_pi.get(i) + 1) % pi.size() == _pi.get((i + 1) % _pi.size()))
                results.add(new byte[]{_pi.get(i), _pi.get((i + 1) % _pi.size())});
        }

        return results;
    }

    public static boolean areNotIntersecting(final List<Cycle> cycles, final Cycle pi) {
        final var cycleIndex = createCycleIndex(cycles, pi);
        for (int i = 0; i < pi.size(); i++) {
            int j = i;
            Cycle a = null;
            while (a == null) {
                a = cycleIndex[pi.get((j++) % pi.size())];
            }
            Cycle b = null;
            while (b == null) {
                b = cycleIndex[pi.get((j++) % pi.size())];
            }
            Cycle c = null;
            while (c == null) {
                c = cycleIndex[pi.get((j++) % pi.size())];
            }
            if (a != b && a == c) {
                return false;
            }
        }
        return true;
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
        final var cycleIndex = createCycleIndex(cycles, piInverse);

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

    public static boolean areSymbolsInCyclicOrder(final byte[] symbols, final byte[] target) {
        final var symbolIndexes = new byte[target.length];

        for (var i = 0; i < target.length; i++) {
            symbolIndexes[target[i]] = (byte) i;
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
                                                                                  final Cycle pi) {
        return canonicalize(spi, pi, null);
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

    /**
     * Find a sorting sequence whose approximation ratio lies between 1 and
     * <code>maxRatio</code>.
     */
    public static List<Cycle> searchForSortingSeq(final Cycle pi, final MulticyclePermutation mu, final Stack<Cycle> rhos,
                                                  final int initialNumberOfEvenCycles, final float maxRatio) {
        return searchForSortingSeq(pi, mu, rhos, initialNumberOfEvenCycles, 1, maxRatio, -1);
    }

    /**
     * Find a sorting sequence whose approximation ratio lies between
     * <code>minRatio</code> and <code>maxRatio</code>.
     */
    private static List<Cycle> searchForSortingSeq(final Cycle pi, final MulticyclePermutation mu, final Stack<Cycle> rhos,
                                                   final int initialNumberOfEvenCycles, final float minRatio,
                                                   final float maxRatio, final int lastMove) {
        final var n = pi.size();

        final var lowerBound = (n - mu.getNumberOfEvenCycles()) / 2;
        final var minAchievableRatio = (float) (rhos.size() + lowerBound) / ((n - initialNumberOfEvenCycles) / 2);

        final var spiCycleIndex = createCycleIndex(mu, pi);

        // Do not allow it to exceed the max ratio
        if (minAchievableRatio <= maxRatio) {
            final var delta = (mu.getNumberOfEvenCycles() - initialNumberOfEvenCycles);
            final var instantRatio = delta > 0
                    ? (float) (rhos.size() * 2) / (mu.getNumberOfEvenCycles() - initialNumberOfEvenCycles)
                    : 0;
            if (0 < instantRatio && minRatio <= instantRatio && instantRatio <= maxRatio) {
                return rhos;
            } else {
                final var iterator = generateAll0_2Moves(pi, spiCycleIndex).iterator();
                while (iterator.hasNext()) {
                    final var pair = iterator.next();
                    final var rho = pair.getFirst();

                    if (pair.getSecond() == 0 && lastMove == 0) {
                        continue;
                    }
                    final var _mu = PermutationGroups.computeProduct(mu, rho.getInverse());
                    rhos.push(rho);
                    final var solution = searchForSortingSeq(applyTransposition(pi, rho),
                            _mu, rhos, initialNumberOfEvenCycles,
                            minRatio, maxRatio, pair.getSecond());
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
    public static Stream<Pair<Cycle, Integer>> generateAll0_2Moves(final Cycle pi, final Cycle[] spiCycleIndex) {
        return IntStream.range(0, pi.size() - 2)
                .boxed().flatMap(i -> IntStream.range(i + 1, pi.size() - 1)
                        .boxed().flatMap(j -> IntStream.range(j + 1, pi.size())
                                .boxed()
                                .takeWhile(k -> !Thread.currentThread().isInterrupted())
                                .map(k -> {
                                    final var rho = new Cycle(pi.get(i), pi.get(j), pi.get(k));
                                    byte a = rho.get(0), b = rho.get(1), c = rho.get(2);

                                    final var is_2Move = spiCycleIndex[a] != spiCycleIndex[b] &&
                                            spiCycleIndex[b] != spiCycleIndex[c] &&
                                            spiCycleIndex[a] != spiCycleIndex[c];

                                    if (is_2Move) {
                                        return new Pair<>(rho, -2);
                                    }

                                    final var is2Move = spiCycleIndex[a] == spiCycleIndex[b] &&
                                            spiCycleIndex[a] == spiCycleIndex[c] &&
                                            spiCycleIndex[a].isOriented(rho.getSymbols()) &&
                                            spiCycleIndex[a].getK(a, b) % 2 == 1 && spiCycleIndex[a].getK(b, c) % 2 == 1 && spiCycleIndex[a].getK(c, a) % 2 == 1;

                                    if (is2Move) {
                                        return new Pair<>(rho, 2);
                                    }

                                    final var is0Move = ((spiCycleIndex[a] == spiCycleIndex[b] &&
                                            spiCycleIndex[a] == spiCycleIndex[c] &&
                                            !spiCycleIndex[a].isOriented(rho.getSymbols()))) ||
                                            ((spiCycleIndex[a] == spiCycleIndex[b] && spiCycleIndex[a] != spiCycleIndex[c] && spiCycleIndex[a].getK(a, b) % 2 != 0) ||
                                                    (spiCycleIndex[b] == spiCycleIndex[c] && spiCycleIndex[b] != spiCycleIndex[a] && spiCycleIndex[b].getK(b, c) % 2 != 0) ||
                                                    (spiCycleIndex[c] == spiCycleIndex[a] && spiCycleIndex[c] != spiCycleIndex[b] && spiCycleIndex[c].getK(c, a) % 2 != 0));

                                    if (is0Move) {
                                        return new Pair<>(rho, 0);
                                    }

                                    return new Pair<Cycle, Integer>(rho, null);
                                }).filter(move -> move.getValue() != null && move.getValue() >= 0)));
    }

    public static <T> Generator<T> combinations(final Collection<T> collection, final int k) {
        return Factory.createSimpleCombinationGenerator(Factory.createVector(collection), k);
    }

    /**
     * Search for a 2-move given first by two odd cycles in \spi. If such move is not found, then search in the oriented cycles of \spi.
     */
    public static Cycle searchFor2Move(final MulticyclePermutation spi, final Cycle pi) {
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
        return !areSymbolsInCyclicOrder(cycle.getSymbols(), pi.getInverse().getSymbols());
    }
}
