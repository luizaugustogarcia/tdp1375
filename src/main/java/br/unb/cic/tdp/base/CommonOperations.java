package br.unb.cic.tdp.base;

import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import lombok.val;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

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

    public static Cycle applyTranspositionOptimized(final Cycle pi, final Cycle move) {
        val a = move.get(0);
        val b = move.get(1);
        val c = move.get(2);

        val indexes = new int[3];
        for (var i = 0; i < pi.size(); i++) {
            if (pi.get(i) == a)
                indexes[0] = i;
            if (pi.get(i) == b)
                indexes[1] = i;
            if (pi.get(i) == c)
                indexes[2] = i;
        }

        Arrays.sort(indexes);

        val result = new int[pi.size()];
        System.arraycopy(pi.getSymbols(), 0, result, 0, indexes[0]);
        System.arraycopy(pi.getSymbols(), indexes[1], result, indexes[0], indexes[2] - indexes[1]);
        System.arraycopy(pi.getSymbols(), indexes[0], result, indexes[0] + (indexes[2] - indexes[1]), indexes[1] - indexes[0]);
        System.arraycopy(pi.getSymbols(), indexes[2], result, indexes[2], pi.size() - indexes[2]);

        return Cycle.of(result);
    }

    public static int mod(final int a, final int b) {
        var r = a % b;
        if (r < 0)
            r += b;
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

    /**
     * Tells if a <code>cycle</code> is oriented or not having <code>pi</code> as reference.
     */
    public static boolean isOriented(final Cycle pi, final Cycle cycle) {
        return !areSymbolsInCyclicOrder(pi.getInverse(), cycle.getSymbols());
    }

    public static Optional<List<int[]>> searchForSorting(final Configuration initialConfiguration,
                                                         final Set<Integer> notFixableSymbols,
                                                         final int[] spi,
                                                         final int[] pi,
                                                         final Stack<int[]> stack,
                                                         final double minRate) {
        val nonFixedSymbols = new HashSet<>();
        val fixedSymbols = new HashSet<>();
        for (int i = 0; i < spi.length; i++) {
            if (i == spi[i]) {
                if (!notFixableSymbols.contains(i)) {
                    fixedSymbols.add(i);
                }
            } else {
                nonFixedSymbols.add(i);
            }
        }

        val rate = (fixedSymbols.size()) / (double) stack.size();
        if (!fixedSymbols.isEmpty()) {
            if (rate >= minRate) {
                if (rate < bestRate) {
                    System.out.println("Best rate: " + rate);
                    bestRate = rate;
                }
                return Optional.of(stack);
            }
        }

        var movesLeft = Math.floor(nonFixedSymbols.size() / 3.0); // each move can add up to 3 bonds
        var totalMoves = stack.size() + movesLeft;
        var globalRate = (initialConfiguration.getSpi().getNumberOfSymbols() - notFixableSymbols.size()) / totalMoves;
        if (globalRate < minRate) {
            return Optional.empty();
        }

        var sorting = Optional.<List<int[]>>empty();
        for (var i = 0; i < pi.length - 2; i++) {
            if (!fixedSymbols.contains(pi[i]))
                for (var j = i + 1; j < pi.length - 1; j++) {
                    if (!fixedSymbols.contains(pi[j]))
                        for (var k = j + 1; k < pi.length; k++) {
                            if (!fixedSymbols.contains(pi[k])) {
                                int a = pi[i], b = pi[j], c = pi[k];

                                int[] m = {a, b, c};
                                stack.push(m);

                                sorting =
                                        searchForSorting(initialConfiguration, notFixableSymbols, times(spi, m[0], m[1], m[2]),
                                                applyTranspositionOptimized(pi, m), stack, minRate);
                                if (sorting.isPresent()) {
                                    return sorting;
                                }
                                stack.pop();
                            }
                        }
                }
        }

        return sorting;
    }

    private static int[] times(final int[] spi, final int a, final int b, final int c) {
        val result = new int[spi.length];
        for (var i = 0; i < result.length; i++) {
            var newIndex = i;
            if (i == a)
                newIndex = c;
            else if (i == b)
                newIndex = a;
            else if (i == c)
                newIndex = b;
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
            if (pi[i] == a)
                indexes[0] = i;
            if (pi[i] == b)
                indexes[1] = i;
            if (pi[i] == c)
                indexes[2] = i;
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

    public static Optional<List<Cycle>> searchForSorting(final Configuration configuration, final double minRate) {
        // eagerly look for an oriented 3-segment
        for (val cycle : configuration.getSpi()) {
            for (val b : cycle.getSymbols()) {
                val move = Cycle.of(cycle.pow(b, -1), b, cycle.image(b));
                if (isOriented(configuration.getPi(), move)) {
                    val spiPrime = configuration.getSpi().times(move.getInverse());
                    if (spiPrime.stream().filter(c -> c.size() == 1 && c.get(0) != cycle.get(0)).count() == 2) {
                        return Optional.of(List.of(move));
                    }
                }
            }
        }

        val sorting = searchForSorting(configuration, configuration.getSpi().stream().map(Cycle::getMinSymbol).collect(Collectors.toSet()),
                twoLinesNotation(configuration.getSpi()), configuration.getPi().getSymbols(), new Stack<>(), minRate)
                .map(moves -> moves.stream().map(Cycle::of).collect(Collectors.toList()));

        if (sorting.isEmpty() && configuration.isFull()) {
            val sigma = configuration.getSpi().times(configuration.getPi().getInverse());
            if (sigma.size() == 1 && sigma.asNCycle().size() == configuration.getPi().size() && searchForSorting(configuration, Set.of(),
                    twoLinesNotation(configuration.getSpi()), configuration.getPi().getSymbols(), new Stack<>(), minRate)
                    .map(moves -> moves.stream().map(Cycle::of).collect(Collectors.toList())).isEmpty()) {
                System.out.println("bad component -> " + configuration);
            }
        }

        return sorting;
    }

    public static int[] twoLinesNotation(final MulticyclePermutation spi) {
        val result = new int[spi.getNumberOfSymbols()];
        for (var i = 0; i < result.length; i++) {
            result[i] = spi.image(i);
        }
        return result;
    }
}