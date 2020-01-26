package br.unb.cic.tdp;

import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.permutation.PermutationGroups;
import cern.colt.list.ByteArrayList;
import cern.colt.list.FloatArrayList;
import org.javatuples.Triplet;
import org.paukov.combinatorics.Factory;
import org.paukov.combinatorics.Generator;

import java.util.*;
import java.util.stream.Collectors;

public class CommonOperations {

    static Cycle simplify(Cycle pi) {
        var _pi = new FloatArrayList();
        for (var i = 0; i < pi.getSymbols().length; i++) {
            _pi.add(pi.getSymbols()[i]);
        }

        var sigma = new ByteArrayList();
        for (var i = 0; i < _pi.size(); i++) {
            sigma.add((byte) i);
        }

        var sigmaPiInverse = PermutationGroups.computeProduct(new Cycle(sigma), pi.getInverse());

        Cycle bigCycle;
        while ((bigCycle = sigmaPiInverse.stream().filter(c -> c.size() > 3).findFirst().orElse(null)) != null) {
            final var leftMostSymbol = leftMostSymbol(bigCycle, pi);
            final var newSymbol = _pi.get(_pi.indexOf(leftMostSymbol) - 1) + 0.001F;
            _pi.beforeInsert(_pi.indexOf(bigCycle.pow(leftMostSymbol, -2)), newSymbol);

            final var piCopy = new FloatArrayList(Arrays.copyOf(_pi.elements(), _pi.size()));
            piCopy.sort();

            final var newPi = new ByteArrayList();
            for (var i = 0; i < piCopy.size(); i++) {
                newPi.add((byte) piCopy.indexOf(_pi.get(i)));
            }

            sigma = new ByteArrayList();
            for (var i = 0; i < newPi.size(); i++) {
                sigma.add((byte) i);
            }

            sigmaPiInverse = PermutationGroups.computeProduct(new Cycle(sigma), new Cycle(newPi).getInverse());

            _pi = new FloatArrayList();
            for (var i = 0; i < newPi.size(); i++) {
                _pi.add(newPi.get(i));
            }
            pi = new Cycle(newPi);
        }

        return pi.getStartingBy((byte) 0);
    }

    private static byte leftMostSymbol(final Cycle bigCycle, final Cycle pi) {
        for (var i = 1; i < pi.getSymbols().length; i++)
            if (bigCycle.contains(pi.get(i)))
                return pi.get(i);
        return -1;
    }

    public static byte[] applyTransposition(final byte[] rho, final byte[] pi) {
        final var a = rho[0];
        final var b = rho[1];
        final var c = rho[2];
        final var indexes = new int[3];
        for (var i = 0; i < pi.length; i++) {
            if (pi[i] == a)
                indexes[0] = i;
            if (pi[i] == b)
                indexes[1] = i;
            if (pi[i] == c)
                indexes[2] = i;
        }
        Arrays.sort(indexes);
        final var result = new byte[pi.length];

        System.arraycopy(pi, 0, result, 0, indexes[0]);
        System.arraycopy(pi, indexes[1], result, indexes[0], indexes[2] - indexes[1]);
        System.arraycopy(pi, indexes[0], result, indexes[0] + (indexes[2] - indexes[1]), indexes[1] - indexes[0]);
        System.arraycopy(pi, indexes[2], result, indexes[2], pi.length - indexes[2]);

        return result;
    }

    public static Cycle[] mapSymbolsToCycles(final Collection<Cycle> spi, final Cycle pi) {
        return mapSymbolsToCycles(spi, pi.getSymbols());
    }

    public static Cycle[] mapSymbolsToCycles(final Collection<Cycle> spi, final byte[] pi) {
        final var symbolToMuCycle = new Cycle[pi.length];
        for (final var muCycle : spi) {
            for (final int symbol : muCycle.getSymbols()) {
                symbolToMuCycle[symbol] = muCycle;
            }
        }
        return symbolToMuCycle;
    }

    public static byte[] signature(final byte[] pi, final List<Cycle> mu) {
        for (final var cycle : mu) {
            cycle.setLabel(-1);
        }

        final var symbolToMuCycle = mapSymbolsToCycles(mu, pi);

        final var signature = new byte[pi.length];
        var lastLabel = 0;
        for (var i = 0; i < signature.length; i++) {
            final int symbol = pi[i];
            final var cycle = symbolToMuCycle[symbol];
            if (cycle.getLabel() == -1) {
                lastLabel++;
                cycle.setLabel(lastLabel);
            }
            signature[i] = (byte) cycle.getLabel();
        }

        return signature;
    }

    public static boolean isOpenGate(final int left, final int right, final Cycle[] symbolToMuCycles, final Collection<Cycle> mu,
                                     final Cycle piInverse) {
        final var gates = left < right ? right - left : piInverse.size() - (left - right);
        for (var i = 1; i < gates; i++) {
            final var index = (i + left) % piInverse.size();
            final var cycle = symbolToMuCycles[piInverse.get(index)];
            if (cycle != null && mu.contains(cycle))
                return false;
        }
        return true;
    }

    public static Map<Cycle, Integer> openGatesPerCycle(final Collection<Cycle> mu, final Cycle piInverse) {
        final var symbolToMuCycles = CommonOperations.mapSymbolsToCycles(mu, piInverse);

        final Map<Cycle, Integer> result = new HashMap<>();
        for (final var muCycle : mu) {
            for (var i = 0; i < muCycle.getSymbols().length; i++) {
                final var left = piInverse.indexOf(muCycle.get(i));
                final var right = piInverse.indexOf(muCycle.image(muCycle.get(i)));
                // O(n)
                if (isOpenGate(left, right, symbolToMuCycles, mu, piInverse)) {
                    if (!result.containsKey(muCycle))
                        result.put(muCycle, 0);
                    result.put(muCycle, result.get(muCycle) + 1);
                }
            }
        }
        return result;
    }

    public static byte[] replace(final byte[] array, final byte a, final byte b) {
        final var replaced = Arrays.copyOf(array, array.length);
        for (var i = 0; i < replaced.length; i++) {
            if (replaced[i] == a)
                replaced[i] = b;
        }
        return replaced;
    }

    static void replace(final byte[] array, final byte[] substitutionMatrix) {
        for (var i = 0; i < array.length; i++) {
            array[i] = substitutionMatrix[array[i]];
        }
    }

    /**
     * Checks whether or not a given sequence of \rho's is a 11/8-sequence.
     */
    public static boolean is11_8(MulticyclePermutation spi, byte[] pi, final List<byte[]> rhos) {
        final var before = spi.getNumberOfEvenCycles();
        for (final var rho : rhos) {
            if (areSymbolsInCyclicOrder(rho, pi)) {
                pi = CommonOperations.applyTransposition(rho, pi);
                spi = PermutationGroups.computeProduct(spi, new Cycle(rho).getInverse());
            } else {
                return false;
            }
        }
        final var after = spi.getNumberOfEvenCycles();
        return after > before && (float) rhos.size() / ((after - before) / 2) <= ((float) 11 / 8);
    }

    public static boolean areSymbolsInCyclicOrder(final byte[] symbols, final byte[] target) {
        final var symbolIndexes = new byte[target.length];

        Arrays.fill(symbolIndexes, (byte) -1);

        for (var i = 0; i < target.length; i++) {
            symbolIndexes[target[i]] = (byte) i;
        }

        int firstIndex = -1;
        int lastIndex = -1;
        int state = 0;

        for (int symbol : symbols) {
            if (state == 0 && symbolIndexes[symbol] < lastIndex) {
                state = 1;
            }
            if (state == 1 || state == 2) {
                if (symbolIndexes[symbol] > firstIndex || (state == 2 && symbolIndexes[symbol] < lastIndex)) {
                    return false;
                }
                if (state == 1) {
                    state = 2;
                }
            }
            lastIndex = symbolIndexes[symbol];
            if (firstIndex == -1) {
                firstIndex = lastIndex;
            }
        }

        return true;
    }

    public static Triplet<MulticyclePermutation, byte[], List<byte[]>> canonicalize(final MulticyclePermutation spi, final byte[] pi) {
        return canonicalize(spi, pi, null);
    }

    public static Triplet<MulticyclePermutation, byte[], List<byte[]>> canonicalize(final MulticyclePermutation spi, final byte[] pi,
                                                                                    final List<byte[]> rhos) {
        var maxSymbol = 0;
        for (byte b : pi)
            if (b > maxSymbol)
                maxSymbol = b;

        final var substitutionMatrix = new byte[maxSymbol + 1];

        for (var i = 0; i < pi.length; i++) {
            substitutionMatrix[pi[i]] = (byte) i;
        }

        final var _pi = Arrays.copyOf(pi, pi.length);

        replace(_pi, substitutionMatrix);

        final var _rhos = new ArrayList<byte[]>();
        if (rhos != null) {
            for (final var rho : rhos) {
                final var _rho = Arrays.copyOf(rho, rho.length);
                replace(_rho, substitutionMatrix);
                _rhos.add(_rho);
            }
        }

        final var _spi = new MulticyclePermutation();

        for (final var cycle : spi) {
            final var _cycle = Arrays.copyOf(cycle.getSymbols(), cycle.size());
            replace(_cycle, substitutionMatrix);
            _spi.add(new Cycle(_cycle));
        }

        return new Triplet<>(_spi, _pi, _rhos);
    }

    /**
     * Find a sorting sequence whose approximation ratio lies between 1 and
     * <code>maxRatio</code>.
     */
    public static List<byte[]> findSortingSequence(final byte[] pi, final MulticyclePermutation mu, final Stack<byte[]> rhos,
                                                   final int initialNumberOfEvenCycles, final float maxRatio) {
        return findSortingSequence(pi, mu, rhos, initialNumberOfEvenCycles, 1, maxRatio);
    }

    /**
     * Find a sorting sequence whose approximation ratio lies between
     * <code>minRatio</code> and <code>maxRatio</code>.
     */
    private static List<byte[]> findSortingSequence(final byte[] pi, final MulticyclePermutation mu, final Stack<byte[]> rhos,
                                                    final int initialNumberOfEvenCycles, final float minRatio, final float maxRatio) {
        final var n = pi.length;

        final var lowerBound = (n - mu.getNumberOfEvenCycles()) / 2;
        final var minAchievableRatio = (float) (rhos.size() + lowerBound) / ((n - initialNumberOfEvenCycles) / 2);

        // Do not allow it to exceed the upper bound
        if (minAchievableRatio <= maxRatio) {
            final var delta = (mu.getNumberOfEvenCycles() - initialNumberOfEvenCycles);
            final var instantRatio = delta > 0
                    ? (float) (rhos.size() * 2) / (mu.getNumberOfEvenCycles() - initialNumberOfEvenCycles)
                    : 0;
            if (0 < instantRatio && minRatio <= instantRatio && instantRatio <= maxRatio) {
                return rhos;
            } else {
                final Set<Byte> fixedSymbols = new HashSet<>();
                for (final var c : mu) {
                    if (c.size() == 1)
                        fixedSymbols.add(c.get(0));
                }

                final var newOmega = new ByteArrayList(n - fixedSymbols.size());
                for (final var symbol : pi) {
                    if (!fixedSymbols.contains(symbol)) {
                        newOmega.add(symbol);
                    }
                }

                for (final var rho : searchAllApp3Cycles(Arrays.copyOfRange(newOmega.elements(), 0, newOmega.size()))) {
                    if (is0Or2Move(rho, mu)) {
                        rhos.push(rho);
                        final var solution = findSortingSequence(CommonOperations.applyTransposition(rho, pi),
                                PermutationGroups.computeProduct(mu, new Cycle(rho).getInverse()), rhos, initialNumberOfEvenCycles,
                                minRatio, maxRatio);
                        if (!solution.isEmpty()) {
                            return rhos;
                        }
                        rhos.pop();
                    }
                }
            }
        }

        return Collections.emptyList();
    }

    /**
     * Search for a 2-move given by an oriented cycle in \spi.
     */
    public static Cycle searchFor2MoveFromOrientedCycle(final MulticyclePermutation spi, final Cycle pi) {
        for (final var cycle : spi.stream().filter(c -> !pi.getInverse().areSymbolsInCyclicOrder(c.getSymbols()))
                .collect(Collectors.toList())) {
            final var before = cycle.isEven() ? 1 : 0;
            for (var i = 0; i < cycle.size() - 2; i++) {
                for (var j = i + 1; j < cycle.size() - 1; j++) {
                    for (var k = j + 1; k < cycle.size(); k++) {
                        final var a = cycle.get(i);
                        final var b = cycle.get(j);
                        final var c = cycle.get(k);
                        if (pi.areSymbolsInCyclicOrder(a, b, c)) {
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
     * Checks whether or not a given \rho is either a 0-move or a 2-move.
     */
    private static boolean is0Or2Move(final byte[] rho, final MulticyclePermutation spi) {
        return PermutationGroups.computeProduct(spi, new Cycle(rho).getInverse()).getNumberOfEvenCycles() >= spi
                .getNumberOfEvenCycles();
    }

    /**
     * Search for all applicable 3-cycles on \pi.
     */
    public static List<byte[]> searchAllApp3Cycles(final byte[] pi) {
        final List<byte[]> result = new ArrayList<>();

        for (var i = 0; i < pi.length - 2; i++) {
            for (var j = i + 1; j < pi.length - 1; j++) {
                for (var k = j + 1; k < pi.length; k++) {
                    result.add(new byte[]{pi[i], pi[j], pi[k]});
                }
            }
        }

        return result;
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
                    for (final var a : get2CyclesSegments(c1))
                        for (final var b : c2.getSymbols()) {
                            for (final var rho : CommonOperations.combinations(Arrays.asList(a.get(0), a.get(1), b), 3)) {
                                final var rho1 = new Cycle(rho.getVector().stream().mapToInt(i -> i).toArray());
                                if (pi.areSymbolsInCyclicOrder(rho1.getSymbols())) {
                                    return rho1;
                                }
                            }
                        }
                }

        return searchFor2MoveFromOrientedCycle(spi, pi);
    }

    static List<Cycle> get2CyclesSegments(final Cycle cycle) {
        final List<Cycle> result = new ArrayList<>();
        for (var i = 0; i < cycle.size(); i++) {
            result.add(new Cycle(cycle.get(i), cycle.image(cycle.get(i))));
        }
        return result;
    }
}
