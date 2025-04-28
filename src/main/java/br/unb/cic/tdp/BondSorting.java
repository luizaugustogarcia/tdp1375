package br.unb.cic.tdp;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.permutation.PermutationGroups;
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

@RequiredArgsConstructor
public class BondSorting {

    public static List<Cycle> sort(Cycle pi, final ProofStorage proofStorage) {
        val sigma = CANONICAL_PI[pi.size()];
        var spi = sigma.times(pi.getInverse());

        val sorting = new ArrayList<Cycle>();

        while (!spi.isIdentity()) {
            val _2move = get2Move(spi, pi, getPivots(spi, pi));
            if (_2move.isPresent()) {
                sorting.add(_2move.get());
                pi = _2move.get().times(pi).asNCycle();
            } else {
                List<Cycle> configuration = new ArrayList<>();
                val gamma = spi.stream().filter(c -> c.size() > 1).findFirst().get();
                val pivot = leftMostSymbol(gamma, pi);
                if (gamma.size() == 2) {
                    configuration.add(Cycle.of(pivot, gamma.image(pivot)));
                } else {
                    configuration.add(Cycle.of(pivot, gamma.image(pivot), gamma.image(gamma.image(pivot))));
                }

                while (true) {
                    val extension = extend(configuration, spi, pi);
                    if (extension == configuration) {
                        // reached a maximal configuration (i.e. component)
                        val moves = findCase(configuration, pi, proofStorage::findCompSorting);
                        if (moves.isEmpty()) {
                            throw new RuntimeException("ERROR"); // it should always find a moves at this point
                        }
                        for (val move : moves.get()) {
                            pi = move.times(pi).asNCycle();
                            spi = spi.times(move.getInverse());
                            sorting.add(move);
                        }
                        break;
                    }
                    configuration = extension;
                    val moves = findCase(configuration, pi, proofStorage::findSorting);
                    if (moves.isPresent()) {
                        for (val move : moves.get()) {
                            pi = move.times(pi).asNCycle();
                            spi = spi.times(move.getInverse());
                            sorting.add(move);
                        }
                        break;
                    }
                }
            }
        }

        return sorting;
    }

    private static Set<Integer> getPivots(final MulticyclePermutation spiPrime, final Cycle piPrime) {
        return spiPrime.stream().map(cycle -> leftMostSymbol(cycle, piPrime)).collect(Collectors.toSet());
    }

    public static void main(String[] args) {
        val storage = new MySQLProofStorage("localhost", "luiz", "luiz");
        var pi = Cycle.of(0, 5, 4, 2, 1, 6, 11, 3, 10, 9, 8, 7);
        System.out.println(pi);
        for (val move : sort(pi, storage)) {
            System.out.println(pi = move.times(pi).asNCycle());
        }
    }

    public static Integer leftMostSymbol(final Cycle cycle, final Cycle pi) {
        return Arrays.stream(cycle.getSymbols())
                .boxed()
                .map(s -> Pair.of(s, pi.startingBy(0).indexOf(s))) // zero is the leftmost
                .min(Comparator.comparing(Pair::getRight)).get().getLeft();
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

    private static Optional<List<Cycle>> findCase(final List<Cycle> configuration, final Cycle pi, final Function<String, Optional<List<Cycle>>> findFn) {
        var spiPrime = new MulticyclePermutation(configuration);
        var piPrime = removeExtraSymbols(spiPrime.getSymbols(), pi);

        val signature = new Configuration(spiPrime, piPrime).getSignature();
        val canonicalConfig = ofSignature(signature.getContent());

        val moves = findFn.apply(canonicalConfig.getSpi().toString());
        if (moves.isPresent()) {
            // translate the moves by simulating its application
            val result = new ArrayList<Cycle>();
            var piPrimePrime = canonicalConfig.getPi();
            for (val move : moves.get()) {
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
            if (symbols.contains(symbol)) {
                newPi.add(symbol);
            }
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

        val cycleIndex = cycleIndex(config, pi);
        val spiCycleIndex = cycleIndex(spi, pi);

        final Set<Integer> openGates = getOpenGates(config, pi);

        // Type 1 extension
        for (final int openGate : openGates) {
            val cycle = cycleIndex[openGate];
            val aPos = pi.indexOf(openGate);
            val bPos = pi.indexOf(cycle.pow(openGate, -1));
            val intersectingCycle = getIntersectingCycle(aPos, bPos, spiCycleIndex, pi);
            if (intersectingCycle.isPresent() && !contains(configSymbols, spiCycleIndex[intersectingCycle.get().get(0)])) {
                val pivot = leftMostSymbol(intersectingCycle.get(), pi);
                if (!configSymbols.contains(pivot)) {
                    int a = pivot;
                    int b = intersectingCycle.get().image(a);

                    val configPrime = new ArrayList<>(config);
                    if (intersectingCycle.get().size() == 2) {
                        configPrime.add(Cycle.of(a, b));
                    } else {
                        int c = intersectingCycle.get().image(b);
                        configPrime.add(Cycle.of(a, b, c));
                    }

                    return configPrime;
                }
            }
        }

        // Type 2 extension
        if (openGates.isEmpty()) {
            for (val cycle : config) {
                for (int i = 0; i < cycle.size(); i++) {
                    val aPos = pi.indexOf(cycle.get(i));
                    val bPos = pi.indexOf(cycle.pow(cycle.get(i), -1));
                    val intersectingCycle = getIntersectingCycle(aPos, bPos, spiCycleIndex, pi);
                    if (intersectingCycle.isPresent() && !contains(configSymbols, spiCycleIndex[intersectingCycle.get().get(0)])) {
                        val pivot = leftMostSymbol(intersectingCycle.get(), pi);
                        if (!configSymbols.contains(pivot)) {
                            int a = pivot;
                            int b = intersectingCycle.get().image(a);

                            val configPrime = new ArrayList<>(config);
                            if (intersectingCycle.get().size() == 2) {
                                configPrime.add(Cycle.of(a, b));
                            } else {
                                int c = intersectingCycle.get().image(b);
                                configPrime.add(Cycle.of(a, b, c));
                            }

                            return configPrime;
                        }
                    }
                }
            }
        }

        // Type 3 extension
        for (var cycle : config) {
            if (cycle.size() < spiCycleIndex[cycle.get(0)].size()) {
                val spiCycle = spiCycleIndex[cycle.get(0)];
                val pivot = leftMostSymbol(cycle, pi);
                cycle = cycle.startingBy(pivot);
                val newSymbols = Arrays.copyOf(cycle.getSymbols(), cycle.size() + 1);
                newSymbols[cycle.size()] = spiCycle.pow(pivot, cycle.size());

                final List<Cycle> configPrime = new ArrayList<>(config);
                configPrime.remove(cycle);
                configPrime.add(Cycle.of(newSymbols));

                val spiPrimePrime = new MulticyclePermutation(configPrime);
                val piPrimePrime = removeExtraSymbols(spiPrimePrime.getSymbols(), pi);
                if (openGates.isEmpty() || new Configuration(spiPrimePrime, piPrimePrime).getOpenGates().size() <= openGates.size())
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

    private static Optional<Cycle> getIntersectingCycle(final int aPos, final int bPos, final Cycle[] spiCycleIndex, final Cycle pi) {
        var nextPos = (aPos + 1) % pi.size();

        while (nextPos != bPos) {
            val intersectingCycle = spiCycleIndex[pi.get(nextPos)];
            if (intersectingCycle != null && intersectingCycle.size() > 1) {
                val pivot = leftMostSymbol(intersectingCycle, pi);
                if (pi.get(nextPos) == pivot) {
                    val b = intersectingCycle.image(pivot);
                    if (isOutOfInterval(pi.indexOf(b), aPos, bPos)) {
                        return Optional.of(intersectingCycle);
                    }
                }
            }
            nextPos = (nextPos + 1) % pi.size();
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
