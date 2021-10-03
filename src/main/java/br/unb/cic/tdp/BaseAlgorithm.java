package br.unb.cic.tdp;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.permutation.PermutationGroups;
import br.unb.cic.tdp.proof.ProofGenerator;
import br.unb.cic.tdp.util.Pair;
import cern.colt.list.IntArrayList;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.primitives.Ints;
import lombok.SneakyThrows;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static br.unb.cic.tdp.base.CommonOperations.*;

public abstract class BaseAlgorithm {

    protected Multimap<Integer, Pair<Configuration, List<Cycle>>> sortings = HashMultimap.create();

    public BaseAlgorithm() {
        loadSortings("cases/cases-3,2.txt", sortings);
        load11_8Sortings(sortings);
    }

    public abstract List<Cycle> sort(Cycle pi);

    protected abstract void load11_8Sortings(Multimap<Integer, Pair<Configuration, List<Cycle>>> sortings);

    protected int get3Norm(final Collection<Cycle> mu) {
        final var numberOfEvenCycles = (int) mu.stream().filter((cycle) -> cycle.size() % 2 == 1).count();
        final var numberOfSymbols = mu.stream().mapToInt(Cycle::size).sum();
        return (numberOfSymbols - numberOfEvenCycles) / 2;
    }

    private boolean isOutOfInterval(final int x, final int left, final int right) {
        if (left < right)
            return x < left || x > right;
        return false;
    }

    private boolean contains(final Set<Integer> muSymbols, final Cycle cycle) {
        for (final Integer symbol : cycle.getSymbols())
            if (muSymbols.contains(symbol))
                return true;
        return false;
    }

    protected List<Cycle> extend(final List<Cycle> bigGamma, final MulticyclePermutation spi, final Cycle pi) {
        final var piInverse = pi.getInverse().startingBy(pi.getMinSymbol());

        final var bigGammaSymbols = new HashSet<Integer>();
        // O(1), since at this point, ||mu|| never exceeds 16
        for (Cycle cycle : bigGamma)
            for (int i = 0; i < cycle.getSymbols().length; i++) {
                bigGammaSymbols.add(cycle.getSymbols()[i]);
            }

        final var bigGammaCycleIndex = cycleIndex(bigGamma, pi);
        final var spiCycleIndex = cycleIndex(spi, pi);

        // Type 1 extension
        // These two outer loops are O(1), since at this point, ||mu|| never
        // exceeds 16
        for (Cycle cycle : bigGamma) {
            for (int i = 0; i < cycle.getSymbols().length; i++) {
                // O(n)
                if (isOpenGate(i, cycle, piInverse, bigGammaCycleIndex)) {
                    final var aPos = piInverse.indexOf(cycle.get(i));
                    final var bPos = piInverse.indexOf(cycle.image(cycle.get(i)));
                    final var intersectingCycle = getIntersectingCycle(aPos, bPos, spiCycleIndex, piInverse);
                    if (intersectingCycle.isPresent()
                            && !contains(bigGammaSymbols, spiCycleIndex[intersectingCycle.get().get(0)])) {
                        int a = intersectingCycle.get().get(0), b = intersectingCycle.get().image(a),
                                c = intersectingCycle.get().image(b);
                        final var _bigGamma = new ArrayList<>(bigGamma);
                        _bigGamma.add(Cycle.create(a, b, c));
                        return _bigGamma;
                    }
                }
            }
        }

        // Type 2 extension
        // O(n)
        if (!hasOpenGates(bigGamma, piInverse, bigGammaCycleIndex)) {
            for (Cycle cycle : bigGamma) {
                for (int i = 0; i < cycle.getSymbols().length; i++) {
                    final var aPos = piInverse.indexOf(cycle.get(i));
                    final var bPos = piInverse.indexOf(cycle.image(cycle.get(i)));
                    for (int j = 1; j < (aPos < bPos ? bPos - aPos : piInverse.size() - (aPos - bPos)); j++) {
                        final var index = (j + aPos) % piInverse.size();
                        if (bigGammaCycleIndex[piInverse.get(index)] == null) {
                            final var intersectingCycle = spiCycleIndex[piInverse.get(index)];
                            if (intersectingCycle != null && intersectingCycle.size() > 1
                                    && !contains(bigGammaSymbols, spiCycleIndex[intersectingCycle.get(0)])) {
                                final var a = piInverse.get(index);
                                final var b = intersectingCycle.image(a);
                                if (isOutOfInterval(piInverse.indexOf(b), aPos, bPos)) {
                                    final var c = intersectingCycle.image(b);
                                    final var _bigGamma = new ArrayList<>(bigGamma);
                                    _bigGamma.add(Cycle.create(a, b, c));
                                    return _bigGamma;
                                }
                            }
                        }
                    }
                }
            }
        }

        return bigGamma;
    }

    private boolean hasOpenGates(List<Cycle> bigGamma, Cycle piInverse, Cycle[] bigGammaCycleIndex) {
        for (Cycle cycle : bigGamma) {
            for (int i = 0; i < cycle.getSymbols().length; i++) {
                final var aPos = piInverse.indexOf(cycle.get(i));
                final var bPos = piInverse.indexOf(cycle.image(cycle.get(i)));
                if (isOpenGate(i, cycle, piInverse, bigGammaCycleIndex)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Optional<Cycle> getIntersectingCycle(int aPos, int bPos, Cycle[] spiCycleIndex,
                                        Cycle piInverse) {
        for (int i = 1; i < (aPos < bPos ? bPos - aPos : piInverse.size() - (aPos - bPos)); i++) {
            final var index = (i + aPos) % piInverse.size();
            final var intersectingCycle = spiCycleIndex[piInverse.get(index)];
            if (intersectingCycle != null && intersectingCycle.size() > 1) {
                final var a = piInverse.get(index);
                final var b = intersectingCycle.image(a);
                if (isOutOfInterval(piInverse.indexOf(b), aPos, bPos)) {
                    return Optional.of(intersectingCycle.startingBy(a));
                }
            }
        }
        return Optional.empty();
    }

    @SneakyThrows
    protected void loadSortings(final String resource, final Multimap<Integer, Pair<Configuration, List<Cycle>>> sortings) {
        final Path file = Paths.get(ProofGenerator.class.getClassLoader().getResource(resource).toURI());
        final var br = new BufferedReader(new FileReader(file.toFile()), 10 * 1024 * 1024);

        String line;
        while ((line = br.readLine()) != null) {
            final var lineSplit = line.trim().split("->");

            final var spi = new MulticyclePermutation(lineSplit[0].replace(" ", ","));
            final var sorting = Arrays.stream(lineSplit[1].substring(1, lineSplit[1].length() - 1)
                    .split(", ")).map(c -> c.replace(" ", ",")).map(s -> Cycle.create(s))
                    .collect(Collectors.toList());
            final var config = new Configuration(spi, CANONICAL_PI[spi.getNumberOfSymbols()]);
            sortings.put(config.hashCode(), new Pair<>(config, sorting));
        }
    }

    protected Pair<Cycle, Cycle> apply2MoveTwoOddCycles(final MulticyclePermutation spi, final Cycle pi) {
        final var oddCycles = spi.stream().filter(c -> !c.isEven()).limit(2).collect(Collectors.toList());
        int a = oddCycles.get(0).get(0), b = oddCycles.get(0).get(1), c = oddCycles.get(1).get(0);

        final Cycle _2Move;
        if (areSymbolsInCyclicOrder(pi, a, b, c)) {
            _2Move = Cycle.create(a, b, c);
        } else {
            _2Move = Cycle.create(a, c, b);
        }

        return new Pair<>(_2Move, applyMoves(pi, Collections.singletonList(_2Move)));
    }

    protected boolean thereAreOddCycles(final MulticyclePermutation spi) {
        return spi.stream().anyMatch(c -> !c.isEven());
    }

    protected Optional<Pair<Cycle, Cycle>> searchFor2_2Seq(final MulticyclePermutation spi, final Cycle pi) {
        for (Pair<Cycle, Integer> move : (Iterable<Pair<Cycle, Integer>>) generateAll0And2Moves(spi, pi)
                .filter(r -> r.getSecond() == 2)::iterator) {
            final var _spi = PermutationGroups
                    .computeProduct(spi, move.getFirst().getInverse());
            final var _pi = applyTransposition(pi, move.getFirst());
            final var secondMove = generateAll0And2Moves(_spi, _pi).filter(r -> r.getSecond() == 2).findFirst();
            if (secondMove.isPresent())
                return Optional.of(new Pair<>(move.getFirst(), secondMove.get().getFirst()));
        }

        return Optional.empty();
    }

    protected Cycle applyMoves(final Cycle pi, final List<Cycle> moves) {
        var _pi = pi;
        for (final var move : moves) {
            _pi = applyTransposition(_pi, move);
        }
        return _pi;
    }

    protected Optional<List<Cycle>> searchForSeq(final List<Cycle> mu, final Cycle pi) {
        final var allSymbols = mu.stream().flatMap(c -> Ints.asList(c.getSymbols()).stream()).collect(Collectors.toSet());
        final var _pi = new IntArrayList(allSymbols.size());
        for (final var symbol : pi.getSymbols()) {
            if (allSymbols.contains(symbol)) {
                _pi.add(symbol);
            }
        }

        final var config = new Configuration(new MulticyclePermutation(mu), Cycle.create(_pi));
        if (sortings.containsKey(config.hashCode())) {
            final var pair = sortings.get(config.hashCode()).stream()
                    .filter(p -> p.getFirst().equals(config)).findFirst().get();
            return Optional.of(config.translatedSorting(pair.getFirst(), pair.getSecond()));
        }

        return Optional.empty();
    }

    protected Pair<List<Cycle>, Cycle> apply3_2_Unoriented(final MulticyclePermutation spi, final Cycle pi) {
        final var segment = spi.stream().filter(c -> c.size() > 1).findFirst().get();
        List<Cycle> mu = new ArrayList<>();
        mu.add(Cycle.create(segment.get(0), segment.get(1), segment.get(2)));
        for (var i = 0; i < 2; i++) {
            mu = extend(mu, spi, pi);
            final var moves = searchForSeq(mu, pi);
            if (moves.isPresent()) {
                return new Pair<>(moves.get(), applyMoves(pi, moves.get()));
            }
        }

        // the article contains the proof that there will always be a (3,2)-sequence at this point
        throw new RuntimeException("ERROR");
    }
}
