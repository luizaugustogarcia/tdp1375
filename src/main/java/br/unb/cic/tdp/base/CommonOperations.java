package br.unb.cic.tdp.base;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import lombok.val;

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
            final Set<Integer> links,
            final int[] spi,
            final int[] pi,
            final Stack<int[]> stack,
            final double minRate) {
        val movedSymbols = new HashSet<>();
        val fixedSymbols = new HashSet<>();
        for (int i = 0; i < spi.length; i++) {
            if (i == spi[i]) {
                if (!links.contains(i)) {
                    fixedSymbols.add(i);
                }
            } else {
                movedSymbols.add(i);
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

        var movesLeft = movedSymbols.size() / 3.0; // each move can add up to 3 bonds
        var totalMoves = stack.size() + movesLeft;
        var globalRate = (initialConfiguration.getSpi().getNumberOfSymbols() - links.size()) / totalMoves;
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

                                int[] m = { a, b, c };
                                stack.push(m);

                                sorting =
                                        searchForSorting(initialConfiguration, links, times(spi, m[0], m[1], m[2]),
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
        val linkSet = configuration.getSpi().stream()
                .map(cycle -> isOriented(configuration.getPi(), cycle) ? Arrays.stream(cycle.getSymbols()).boxed().collect(Collectors.toSet()) : Set.of(cycle.getMinSymbol()))
                .collect(Collectors.toList());

        val sortings = Sets.cartesianProduct(linkSet).stream()
                .map(symbols -> {
                    val links = new HashSet<>(symbols);
                    if (lookFor2Move(configuration, links).isPresent()) {
                        return Optional.of(List.of(Cycle.of(1, 2, 3)));
                    }
                    return searchForSorting(configuration, links,
                            twoLinesNotation(configuration.getSpi()), configuration.getPi().getSymbols(), new Stack<>(), minRate)
                            .map(moves -> moves.stream().map(Cycle::of).collect(Collectors.toList()));
                });

        boolean anyEmpty = sortings.anyMatch(Optional::isEmpty);
        if (anyEmpty && configuration.isFull()) {
            val sigma = configuration.getSpi().times(configuration.getPi().getInverse());
            if (sigma.size() == 1 && sigma.asNCycle().size() == configuration.getPi().size() && searchForSorting(configuration, Set.of(),
                    twoLinesNotation(configuration.getSpi()), configuration.getPi().getSymbols(), new Stack<>(), minRate)
                    .map(moves -> moves.stream().map(Cycle::of).collect(Collectors.toList())).isEmpty()) {
                System.out.println("bad component -> " + configuration);
            }
        }

        return anyEmpty ? Optional.empty() : Optional.of(List.of(Cycle.of(1, 2, 3)));
    }

    private static Optional<List<Cycle>> lookFor2Move(final Configuration configuration, final Set<Integer> links) {
        val pi = configuration.getPi().getSymbols();

        for (var i = 0; i < pi.length - 2; i++) {
            for (var j = i + 1; j < pi.length - 1; j++) {
                for (var k = j + 1; k < pi.length; k++) {
                    int a = pi[i], b = pi[j], c = pi[k];

                    val move = Cycle.of(a, b, c);
                    val spi = configuration.getSpi().times(move.getInverse());
                    if (spi.stream().filter(cycle -> cycle.size() == 1 && !links.contains(cycle.get(0))).count() == 2) {
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

    public static void main(String[] args) {
        System.out.println(searchForSorting(new Configuration("(0 13 1 11 9)(2 12 8)(3 7 4)(5 6 14 10)"), 1.6));
    }
}