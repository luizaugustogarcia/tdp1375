package br.unb.cic.tdp;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.permutation.PermutationGroups;
import br.unb.cic.tdp.proof.DefaultProofStorage;
import br.unb.cic.tdp.proof.MySQLProofStorage;
import br.unb.cic.tdp.proof.ProofStorage;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static br.unb.cic.tdp.base.CommonOperations.*;
import static br.unb.cic.tdp.base.Configuration.ofSignature;
import static br.unb.cic.tdp.proof.SortOrExtend.insertAtPosition;
import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
public class BondSorting {

    public static void sort(Cycle pi, final ProofStorage proofStorage) {
        val sigma = CANONICAL_PI[pi.size()];
        var spi = sigma.times(pi.getInverse());

        while (!spi.isIdentity()) {
            // TODO handle 2-cycles

            var simplification = simplify(spi, pi); // remove the inserted symbols and re-enumerate the symbols to restore original permutation

            var simplifiedPi = simplification.getRight();
            var simplifiedSpi = simplification.getLeft().getLeft();

            var pivots = getPivots(simplifiedSpi, simplifiedPi);

            val _2move = get2Move(simplifiedSpi, simplifiedPi, pivots);
            if (_2move.isPresent()) {
                simplifiedPi = _2move.get().times(simplifiedPi).asNCycle();
            } else {
                List<Cycle> configuration = new ArrayList<>();
                val gamma = simplifiedSpi.stream().filter(c -> c.size() > 1).findFirst().get();
                configuration.add(Cycle.of(gamma.get(0), gamma.get(1), gamma.get(2)));

                while (true) {
                    val extension = extend(configuration, simplifiedSpi, simplifiedPi);
                    if (extension == configuration) {
                        // reached a maximal configuration (i.e. component)
                        val sorting = findBySorting(configuration, simplifiedPi, proofStorage::findCompSorting);
                        if (sorting.isEmpty()) {
                            throw new RuntimeException("ERROR"); // it should always find a sorting at this point
                        }
                        for (val move : sorting.get()) {
                            simplifiedPi = move.times(simplifiedPi).asNCycle();
                            simplifiedSpi = simplifiedSpi.times(move.getInverse());
                        }
                        break;
                    }
                    configuration = extension;
                    val sorting = findBySorting(configuration, simplifiedPi, proofStorage::findSorting);
                    if (sorting.isPresent()) {
                        for (val move : sorting.get()) {
                            simplifiedPi = move.times(simplifiedPi).asNCycle();
                            simplifiedSpi = simplifiedSpi.times(move.getInverse());
                        }
                        break;
                    }
                }
            }

            pi = desimplifyAndNormalize(simplifiedPi, simplification.getLeft().getRight());
        }
    }

    private static Cycle desimplifyAndNormalize(final Cycle simplifiedPi, final Set<Integer> includedSymbols) {
        val list = Arrays.stream(simplifiedPi.getSymbols())
                .boxed()
                .collect(toList());
        list.removeIf(includedSymbols::contains);
        return Cycle.of(normalizeAfterRemovals(list, includedSymbols, simplifiedPi.getMaxSymbol()));
    }

    public static int[] normalizeAfterRemovals(final List<Integer> list, final Set<Integer> removedValues, final int n) {
        int[] shift = new int[n + 2];  // shift[i] = number of removed values < i

        // Build prefix count of removed values
        boolean[] isRemoved = new boolean[n + 2];
        for (int x : removedValues) {
            if (x >= 1 && x <= n) {
                isRemoved[x] = true;
            }
        }
        for (int i = 1; i <= n + 1; i++) {
            shift[i] = shift[i - 1] + (isRemoved[i - 1] ? 1 : 0);
        }

        // Relabel values
        int[] result = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            int val = list.get(i);
            result[i] = val - shift[val];
        }

        return result;
    }

    private static Set<Integer> getPivots(final MulticyclePermutation spiPrime, final Cycle piPrime) {
        return spiPrime.stream().map(cycle -> leftMostSymbol(cycle, piPrime)).collect(Collectors.toSet());
    }

    public static void main(String[] args) {
        val storage = new MySQLProofStorage("192.168.68.114");
        sort(Cycle.of(0, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1), storage);
    }

    private static Pair<Pair<MulticyclePermutation, Set<Integer>>, Cycle> simplify(MulticyclePermutation spi, Cycle pi) {
        val segments = new ArrayList<Cycle>();
        Set<Integer> insertedSymbols = new HashSet<>();

        val queue = new LinkedList<>(spi);
        while (!queue.isEmpty()) {
            var breakingCycle = startingFromLeftMostSymbol(queue.poll(), pi);
            if (breakingCycle.size() <= 4) {
                segments.add(breakingCycle);
            } else {
                val segment = Cycle.of(breakingCycle.get(0), breakingCycle.get(1), breakingCycle.get(2));
                segments.add(segment);

                val pivot = segment.get(2) + 1;
                // relabel inserted symbols
                insertedSymbols = insertedSymbols.stream().map(s -> s > pivot ? s + 1 : s).collect(Collectors.toSet());
                // relabel segments symbols
                segments.forEach(s -> {
                    val symbols = s.getSymbols();
                    for (int i = 0; i < symbols.length; i++) {
                        val currentSymbol = symbols[i];
                        symbols[i] = currentSymbol >= pivot ? currentSymbol + 1 : currentSymbol;
                    }
                    s.update(symbols);
                });

                insertedSymbols.add(pivot);

                val newBreakingCycle = new int[breakingCycle.size() - 2];
                newBreakingCycle[0] = pivot;
                for (int i = 1; i < newBreakingCycle.length; i++) {
                    val currentSymbol = breakingCycle.getSymbols()[i + 2];
                    newBreakingCycle[i] = currentSymbol >= pivot ? currentSymbol + 1 : currentSymbol;
                }

                val newPi = insertAtPosition(pi.getSymbols(), -1, pi.indexOf(breakingCycle.get(0)) + 1);
                // relabel pi symbols
                for (int i = 0; i < newPi.length; i++) {
                    val currentSymbol = newPi[i];
                    newPi[i] = currentSymbol >= pivot ? currentSymbol + 1 : currentSymbol;
                    newPi[i] = currentSymbol == -1 ? pivot : newPi[i];
                }

                pi = Cycle.of(newPi);

                // relabel symbols
                queue.forEach(cycle -> {
                    val symbols = cycle.getSymbols();
                    for (int i = 0; i < symbols.length; i++) {
                        val currentSymbol = symbols[i];
                        symbols[i] = currentSymbol >= pivot ? currentSymbol + 1 : currentSymbol;
                    }
                    cycle.toString();
                    cycle.update(symbols);
                });

                queue.offerFirst(Cycle.of(newBreakingCycle));
            }
        }

        return Pair.of(Pair.of(new MulticyclePermutation(segments).conjugateBy(CANONICAL_PI[pi.size()]), insertedSymbols), pi);
    }

    private static Integer leftMostSymbol(final Cycle cycle, Cycle pi) {
        return Arrays.stream(cycle.getSymbols())
                .boxed()
                .map(s -> Pair.of(s, s == 0 ? pi.size() - 1 : pi.indexOf(s))) // zero is the rightmost symbol
                .min(Comparator.comparing(Pair::getRight)).get().getLeft();
    }

    private static Cycle startingFromLeftMostSymbol(final Cycle cycle, Cycle pi) {
        return cycle.startingBy(Arrays.stream(cycle.getSymbols())
                .boxed()
                .map(s -> Pair.of(s, pi.indexOf(s)))
                .min(Comparator.comparing(Pair::getRight)).get().getLeft());
    }

    private static Optional<Cycle> get2Move(final MulticyclePermutation spi, final Cycle pi, final Set<Integer> pivots) {
        val piSymbols = pi.getSymbols();

        for (var i = 0; i < piSymbols.length - 2; i++) {
            for (var j = i + 1; j < piSymbols.length - 1; j++) {
                for (var k = j + 1; k < piSymbols.length; k++) {
                    int a = piSymbols[i], b = piSymbols[j], c = piSymbols[k];

                    val move = Cycle.of(a, b, c);
                    val spiPrime = spi.times(move.getInverse());
                    final Predicate<Cycle> trivialNonPivot = cycle -> cycle.size() == 1 && !pivots.contains(cycle.get(0));
                    if (spiPrime.stream().filter(trivialNonPivot).count() >= spi.stream().filter(trivialNonPivot).count() + 2) {
                        return Optional.of(move);
                    }
                }
            }
        }

        return Optional.empty();
    }

    private static Optional<List<Cycle>> findBySorting(final List<Cycle> configuration, final Cycle pi, final Function<String, Optional<List<Cycle>>> findFn) {
        var spiPrime = new MulticyclePermutation(configuration);
        var piPrime = removeExtraSymbols(spiPrime.getSymbols(), pi);

        val signature = new Configuration(spiPrime, piPrime).getSignature();
        val canonicalConfig = ofSignature(signature.getContent());

        val sorting = findFn.apply(canonicalConfig.getSpi().toString());
        if (sorting.isPresent()) {
            // translate the sorting by simulating its application
            val result = new ArrayList<Cycle>();
            var piPrimePrime = canonicalConfig.getPi();
            for (val move : sorting.get()) {
                val a = move.get(0);
                val b = move.get(1);
                val c = move.get(2);

                val i = piPrimePrime.indexOf(a);
                val j = piPrimePrime.indexOf(b);
                val k = piPrimePrime.indexOf(c);

                val movePrime = Cycle.of(piPrime.get(i), piPrime.get(j), piPrime.get(k));
                result.add(movePrime);

                piPrime = PermutationGroups.computeProduct(false, movePrime.times(piPrime)).asNCycle();
                piPrimePrime = move.times(piPrimePrime).asNCycle();
                spiPrime = spiPrime.times(movePrime.getInverse());
            }
            return Optional.of(result);
        }

        return Optional.empty();
    }

    public static Cycle removeExtraSymbols(final Set<Integer> symbols, final Cycle pi) {
        val newPi = new ArrayList<Integer>(symbols.size());
        for (val symbol : pi.getSymbols()) {
            if (symbols.contains(symbol))
                newPi.add(symbol);
        }
        return Cycle.of(newPi.stream().map(Object::toString).collect(Collectors.joining(" ")));
    }

    public static List<Cycle> extend(final List<Cycle> config, final MulticyclePermutation spi, final Cycle pi) {
        val piInverse = pi.getInverse().startingBy(pi.getMinSymbol());

        val configSymbols = new HashSet<Integer>();
        for (Cycle cycle : config)
            for (int i = 0; i < cycle.getSymbols().length; i++) {
                configSymbols.add(cycle.getSymbols()[i]);
            }

        val configCycleIndex = cycleIndex(config, pi);
        val cycleIndex = cycleIndex(spi, pi);

        val spiPrime = new MulticyclePermutation(config);
        val piPrime = removeExtraSymbols(spiPrime.getSymbols(), pi);
        val openGates = new Configuration(spiPrime, piPrime).getOpenGates().stream().map(piPrime::get).collect(toList());

        // Type 1 extension
        for (final int openGate : openGates) {
            val cycle = configCycleIndex[openGate];
            val aPos = piInverse.indexOf(openGate);
            val bPos = piInverse.indexOf(cycle.image(openGate));
            val intersectingCycle = getIntersectingCycle(aPos, bPos, cycleIndex, piInverse);
            if (intersectingCycle.isPresent()
                    && !contains(configSymbols, cycleIndex[intersectingCycle.get().get(0)])) {

                int a = intersectingCycle.get().get(0);
                int b = intersectingCycle.get().image(a);
                int c = intersectingCycle.get().image(b);
                val configPrime = new ArrayList<>(config);
                configPrime.add(Cycle.of(a, b, c));

                return configPrime;
            }
        }

        // Type 2 extension
        if (openGates.isEmpty()) {
            for (Cycle cycle : config) {
                for (int i = 0; i < cycle.size(); i++) {
                    val aPos = piInverse.indexOf(cycle.get(i));
                    val bPos = piInverse.indexOf(cycle.image(cycle.get(i)));
                    for (int j = 1; j < (aPos < bPos ? bPos - aPos : piInverse.size() - (aPos - bPos)); j++) {
                        val index = (j + aPos) % piInverse.size();
                        if (configCycleIndex[piInverse.get(index)] == null) {
                            val intersectingCycle = cycleIndex[piInverse.get(index)];
                            if (intersectingCycle != null && intersectingCycle.size() > 1
                                    && !contains(configSymbols, cycleIndex[intersectingCycle.get(0)])) {
                                val a = piInverse.get(index);
                                val b = intersectingCycle.image(a);
                                if (isOutOfInterval(piInverse.indexOf(b), aPos, bPos)) {
                                    val c = intersectingCycle.image(b);
                                    val configPrime = new ArrayList<>(config);
                                    configPrime.add(Cycle.of(a, b, c));
                                    return configPrime;
                                }
                            }
                        }
                    }
                }
            }
        }

        // Type 3 extension
        for (var cycle : config) {
            if (cycle.size() < cycleIndex[cycle.get(0)].size()) {
                val spiCycle = align(cycleIndex[cycle.get(0)], cycle);
                cycle = cycle.startingBy(spiCycle.get(0));
                val newSymbols = Arrays.copyOf(cycle.getSymbols(), cycle.size() + 1);
                newSymbols[cycle.size()] = spiCycle
                        .image(cycle.get(cycle.size() - 1));

                final List<Cycle> configPrime = new ArrayList<>(config);
                configPrime.remove(cycle);
                configPrime.add(Cycle.of(newSymbols));

                val spiPrimePrime = new MulticyclePermutation(configPrime);
                val piPrimePrime = removeExtraSymbols(spiPrimePrime.getSymbols(), pi);
                if (new Configuration(spiPrimePrime, piPrimePrime).getOpenGates().size() <= 2)
                    return configPrime;
            }
        }

        return config;
    }

    public static Set<Integer> getOpenGates(final Collection<Cycle> spi, final Cycle pi) {
        val openGates = new HashSet<Integer>();

        for (val epsilon : spi) {
            if (epsilon.size() == 1) continue;

            for (int i = 0; i < epsilon.size(); i++) {
                int a = epsilon.get(i);
                int b = epsilon.image(a);

                var intersecting = false;
                var orientedTriple = false;

                openGate:
                for (Cycle gamma : spi) {
                    if (gamma.size() == 1) continue;

                    if (epsilon != gamma) {
                        for (int j = 0; j < gamma.size(); j++) {
                            int c = gamma.get(j);
                            int d = gamma.image(c);

                            if (areSymbolsInCyclicOrder(pi.getInverse(), a, c, b, d)) {
                                intersecting = true;
                                break openGate;
                            }
                        }
                    } else {
                        for (int j = 0; j < epsilon.size(); j++) {
                            int c = epsilon.get(j);
                            if (c != a && c != b && areSymbolsInCyclicOrder(pi, a, b, c)) {
                                orientedTriple = true;
                                break openGate;
                            }
                        }
                    }
                }

                if (!intersecting && !orientedTriple) {
                    openGates.add(a);
                }
            }
        }

        return openGates;
    }

    private static Cycle align(final Cycle spiCycle, final Cycle segment) {
        for (var i = 0; i < segment.size(); i++) {
            var symbol = segment.get(i);
            val index = spiCycle.indexOf(symbol);
            var match = true;
            for (var j = 1; j < segment.size(); j++) {
                if (segment.get((i + j) % segment.size()) != spiCycle.get((index + j) % spiCycle.size())) {
                    match = false;
                    break;
                }
                symbol = segment.image(symbol);
            }
            if (match) return spiCycle.startingBy(segment.get(i));
        }
        return null;
    }

    protected static List<Cycle> ehExtend(final List<Cycle> config, final MulticyclePermutation spi, final Cycle pi) {
        val piInverse = pi.getInverse().startingBy(pi.getMinSymbol());

        val configSymbols = new HashSet<Integer>();
        for (Cycle cycle : config)
            for (int i = 0; i < cycle.getSymbols().length; i++) {
                configSymbols.add(cycle.getSymbols()[i]);
            }

        val configCycleIndex = cycleIndex(config, pi);
        val spiCycleIndex = cycleIndex(spi, pi);

        final Set<Integer> openGates = new Configuration(new MulticyclePermutation(config), pi).getOpenGates();

        // Type 1 extension
        for (final int openGate : openGates) {
            val cycle = configCycleIndex[openGate];
            val aPos = piInverse.indexOf(openGate);
            val bPos = piInverse.indexOf(cycle.image(openGate));
            val intersectingCycle = getIntersectingCycle(aPos, bPos, spiCycleIndex, piInverse);
            if (intersectingCycle.isPresent() && !contains(configSymbols, spiCycleIndex[intersectingCycle.get().get(0)])) {

                int a = intersectingCycle.get().get(0);
                int b = intersectingCycle.get().image(a);
                int c = intersectingCycle.get().image(b);
                val configPrime = new ArrayList<>(config);
                configPrime.add(Cycle.of(a, b, c));

                return configPrime;
            }
        }

        // Type 2 extension
        if (openGates.isEmpty()) {
            for (Cycle cycle : config) {
                for (int i = 0; i < cycle.size(); i++) {
                    val aPos = piInverse.indexOf(cycle.get(i));
                    val bPos = piInverse.indexOf(cycle.image(cycle.get(i)));
                    for (int j = 1; j < (aPos < bPos ? bPos - aPos : piInverse.size() - (aPos - bPos)); j++) {
                        val index = (j + aPos) % piInverse.size();
                        if (configCycleIndex[piInverse.get(index)] == null) {
                            val intersectingCycle = spiCycleIndex[piInverse.get(index)];
                            if (intersectingCycle != null && intersectingCycle.size() > 1 && !contains(configSymbols, spiCycleIndex[intersectingCycle.get(0)])) {
                                val a = piInverse.get(index);
                                val b = intersectingCycle.image(a);
                                if (isOutOfInterval(piInverse.indexOf(b), aPos, bPos)) {
                                    val c = intersectingCycle.image(b);
                                    val configPrime = new ArrayList<>(config);
                                    configPrime.add(Cycle.of(a, b, c));
                                    return configPrime;
                                }
                            }
                        }
                    }
                }
            }
        }

        return config;
    }

    private static Optional<Cycle> getIntersectingCycle(final int aPos, final int bPos, final Cycle[] spiCycleIndex, final Cycle piInverse) {
        var nextPos = (aPos + 1) % piInverse.size();

        while (nextPos != bPos) {
            val intersectingCycle = spiCycleIndex[piInverse.get(nextPos)];
            if (intersectingCycle != null && intersectingCycle.size() > 1) {
                val a = piInverse.get(nextPos);
                val b = intersectingCycle.image(a);
                if (isOutOfInterval(piInverse.indexOf(b), aPos, bPos)) {
                    return Optional.of(intersectingCycle.startingBy(a));
                }
            }
            nextPos = (nextPos + 1) % piInverse.size();
        }

        return Optional.empty();
    }

    public static boolean isOutOfInterval(final int pos, final int aPos, final int bPos) {
        return (aPos < bPos && (pos < aPos || pos > bPos)) || (pos < aPos && pos > bPos);
    }

    private static boolean contains(final Set<Integer> muSymbols, final Cycle cycle) {
        for (final Integer symbol : cycle.getSymbols())
            if (muSymbols.contains(symbol)) return true;
        return false;
    }
}
