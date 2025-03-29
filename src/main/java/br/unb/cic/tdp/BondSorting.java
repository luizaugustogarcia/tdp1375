package br.unb.cic.tdp;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.proof.ProofStorage;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static br.unb.cic.tdp.base.CommonOperations.*;
import static br.unb.cic.tdp.proof.SortOrExtend.insertAtPosition;

@RequiredArgsConstructor
public class BondSorting {

    private final ProofStorage proofStorage;

    public void sort(Cycle pi) {
        val sigma = CANONICAL_PI[pi.size()];
        var spi = sigma.times(pi.getInverse());

        loop:
        while (!spi.isIdentity()) {
            var simplification = simplify(spi, pi); // remove the inserted symbols and re-enumerate the symbols to restore original permutation

            // TODO 2-cycles

            val _2move = get2Move(spi, pi);
            if (_2move.isPresent()) {
                pi = _2move.get().times(pi).asNCycle();
                spi = spi.times(_2move.get().getInverse());
                continue;
            }

            List<Cycle> configuration = new ArrayList<>();
            val gamma = spi.stream().filter(c -> c.size() > 1).findFirst().get();
            configuration.add(Cycle.of(gamma.get(0), gamma.get(1), gamma.get(2)));
            while (true) {
                val extension = extend(configuration, spi, pi);
                if (extension == configuration) {
                    // TODO should always find a sorting at this point
                    // reached a maximal configuration (i.e. component)
                    val sorting = findBySorting(configuration, pi, proofStorage);
                    System.out.println(sorting);
                }
                configuration = extension;
                val sortings = findBySorting(configuration, pi, proofStorage);
                if (!sortings.isEmpty()) {
                    System.out.println(sortings);
                    break;
                }
            }
        }
    }

    public static void main(String[] args) {
        val pi = Cycle.of(0,30,11,4,19,18,22,6,10,20,2,25,28,21,17,16,13,9,8,23,1,24,29,27,5,26,14,15,3,7,12);
        val spi = CANONICAL_PI[pi.size()].times(pi.getInverse());
        System.out.println(simplify(spi, pi));
    }

    private static Pair<Pair<MulticyclePermutation, Set<Integer>>, Cycle> simplify(final MulticyclePermutation spi, Cycle pi) {
        val newSpi = new ArrayList<Cycle>();
        Set<Integer> insertedSymbols = new HashSet<>();

        val queue = new LinkedList<>(spi);
        while (!queue.isEmpty()) {
            var breakingCycle = startingFromLeftMostSymbol(queue.poll(), pi);
            if (breakingCycle.size() <= 4) {
                newSpi.add(breakingCycle);
            } else {
                val segment = Cycle.of(breakingCycle.get(0), breakingCycle.get(1), breakingCycle.get(2));
                newSpi.add(segment);

                val pivot = segment.get(2) + 1;
                insertedSymbols = insertedSymbols.stream().map(s -> s > pivot? s + 1: s).collect(Collectors.toSet());

                insertedSymbols.add(pivot);

                val newBreakingCycle = new int[breakingCycle.size() - 2];
                newBreakingCycle[0] = pivot;
                for (int i = 1; i < newBreakingCycle.length; i++) {
                    val currentSymbol = breakingCycle.getSymbols()[i + 2];
                    newBreakingCycle[i] = currentSymbol >= pivot ? currentSymbol + 1 : currentSymbol;
                }

                val newPi = insertAtPosition(pi.getSymbols(), -1, pi.indexOf(breakingCycle.get(0)) + 1);
                for (int i = 0; i < newPi.length; i++) {
                    val currentSymbol = newPi[i];
                    newPi[i] = currentSymbol >= pivot ? currentSymbol + 1 : currentSymbol;
                    newPi[i] = currentSymbol == -1 ? pivot : newPi[i];
                }

                pi = Cycle.of(newPi);

                queue.offer(Cycle.of(newBreakingCycle));
            }
        }

        return Pair.of(Pair.of(new MulticyclePermutation(newSpi), insertedSymbols), pi);
    }

    private static Cycle startingFromLeftMostSymbol(final Cycle cycle, Cycle pi) {
        return cycle.startingBy(Arrays.stream(cycle.getSymbols())
                .boxed()
                .map(s -> Pair.of(s, pi.indexOf(s)))
                .min(Comparator.comparing(Pair::getRight)).get().getLeft());
    }

    private Optional<Cycle> get2Move(final MulticyclePermutation spi, final Cycle pi) {
        val piSymbols = pi.getSymbols();

        for (var i = 0; i < piSymbols.length - 2; i++) {
            for (var j = i + 1; j < piSymbols.length - 1; j++) {
                for (var k = j + 1; k < piSymbols.length; k++) {
                    int a = piSymbols[i], b = piSymbols[j], c = piSymbols[k];

                    val move = Cycle.of(a, b, c);
                    val spiPrime = spi.times(move.getInverse());
                    final Predicate<Cycle> trivial = cycle -> cycle.size() == 1;
                    if (spiPrime.stream().filter(trivial).count() >= spi.stream().filter(trivial).count() + 2) {
                        return Optional.of(move);
                    }
                }
            }
        }

        return Optional.empty();
    }

    private List<Pair<Configuration, Pair<Set<Integer>, List<Cycle>>>> findBySorting(final List<Cycle> configuration, final Cycle pi, final ProofStorage proofStorage) {
        val spi = new MulticyclePermutation(configuration);
        val config = new Configuration(spi, removeExtraSymbols(spi.getSymbols(), pi));
        return proofStorage.findBySorting(Configuration.ofSignature(config.getSignature().getContent()).getSpi().toString());
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
        // O(1), since at this point, ||mu|| never exceeds 16
        for (Cycle cycle : config)
            for (int i = 0; i < cycle.getSymbols().length; i++) {
                configSymbols.add(cycle.getSymbols()[i]);
            }

        val configCycleIndex = cycleIndex(config, pi);
        val cycleIndex = cycleIndex(spi, pi);

        val spiPrime = new MulticyclePermutation(config);
        val piPrime = removeExtraSymbols(spiPrime.getSymbols(), pi);
        val openGates = new Configuration(spiPrime, piPrime).getOpenGates().stream().map(piPrime::get).collect(Collectors.toList());

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
