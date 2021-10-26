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
import static br.unb.cic.tdp.proof.ProofGenerator.removeExtraSymbols;

public abstract class BaseAlgorithm {

    protected Multimap<Integer, Pair<Configuration, List<Cycle>>> _3_2_sortings = HashMultimap.create();
    protected Multimap<Integer, Pair<Configuration, List<Cycle>>> _11_8_sortings = HashMultimap.create();

    public BaseAlgorithm() {
        loadSortings("cases/cases-3,2.txt", _3_2_sortings);
        load11_8Sortings(_11_8_sortings);
    }

    public abstract Pair<Cycle, List<Cycle>> sort(Cycle pi);

    protected abstract void load11_8Sortings(Multimap<Integer, Pair<Configuration, List<Cycle>>> sortings);

    public static int get3Norm(final Collection<Cycle> mu) {
        final var numberOfEvenCycles = (int) mu.stream().filter((cycle) -> cycle.size() % 2 == 1).count();
        final var numberOfSymbols = mu.stream().mapToInt(Cycle::size).sum();
        return (numberOfSymbols - numberOfEvenCycles) / 2;
    }

    public static boolean isOutOfInterval(final int pos, final int aPos, final int bPos) {
        if (aPos < bPos)
            return pos < aPos || pos > bPos;
        return pos < aPos && pos > bPos;
    }

    private static boolean contains(final Set<Integer> muSymbols, final Cycle cycle) {
        for (final Integer symbol : cycle.getSymbols())
            if (muSymbols.contains(symbol))
                return true;
        return false;
    }

    public static List<Cycle> ehExtend(final List<Cycle> config, final MulticyclePermutation spi, final Cycle pi) {
        final var piInverse = pi.getInverse().startingBy(pi.getMinSymbol());

        final var configSymbols = new HashSet<Integer>();
        // O(1), since at this point, ||mu|| never exceeds 16
        for (Cycle cycle : config)
            for (int i = 0; i < cycle.getSymbols().length; i++) {
                configSymbols.add(cycle.getSymbols()[i]);
            }

        final var configCycleIndex = cycleIndex(config, pi);
        final var spiCycleIndex = cycleIndex(spi, pi);

        final var openGates = getOpenGates(config, pi);

        // Type 1 extension
        // These two outer loops are O(1), since at this point, ||mu|| never
        // exceeds 16
        for (final int openGate: openGates) {
            final var symbol = pi.getSymbols()[openGate];
            final var cycle = configCycleIndex[symbol];
            final var aPos = piInverse.indexOf(symbol);
            final var bPos = piInverse.indexOf(cycle.image(symbol));
            final var intersectingCycle = getIntersectingCycle(aPos, bPos, spiCycleIndex, piInverse);
            if (intersectingCycle.isPresent()
                    && !contains(configSymbols, spiCycleIndex[intersectingCycle.get().get(0)])) {

                int a = intersectingCycle.get().get(0), b = intersectingCycle.get().image(a),
                        c = intersectingCycle.get().image(b);
                final var _config = new ArrayList<>(config);
                _config.add(Cycle.create(a, b, c));

                return _config;
            }
        }

        // Type 2 extension
        // O(n)
        if (openGates.isEmpty()) {
            for (Cycle cycle : config) {
                for (int i = 0; i < cycle.size(); i++) {
                    final var aPos = piInverse.indexOf(cycle.get(i));
                    final var bPos = piInverse.indexOf(cycle.image(cycle.get(i)));
                    for (int j = 1; j < (aPos < bPos ? bPos - aPos : piInverse.size() - (aPos - bPos)); j++) {
                        final var index = (j + aPos) % piInverse.size();
                        if (configCycleIndex[piInverse.get(index)] == null) {
                            final var intersectingCycle = spiCycleIndex[piInverse.get(index)];
                            if (intersectingCycle != null && intersectingCycle.size() > 1
                                    && !contains(configSymbols, spiCycleIndex[intersectingCycle.get(0)])) {
                                final var a = piInverse.get(index);
                                final var b = intersectingCycle.image(a);
                                if (isOutOfInterval(piInverse.indexOf(b), aPos, bPos)) {
                                    final var c = intersectingCycle.image(b);
                                    final var _config = new ArrayList<>(config);
                                    _config.add(Cycle.create(a, b, c));
                                    return _config;
                                }
                            }
                        }
                    }
                }
            }
        }

        return config;
    }

    private static Optional<Cycle> getIntersectingCycle(final int aPos, final int bPos,
                                                        final Cycle[] spiCycleIndex, final Cycle piInverse) {
        var nextPos = (aPos + 1) % piInverse.size();

        while (nextPos != bPos) {
            final var intersectingCycle = spiCycleIndex[piInverse.get(nextPos)];
            if (intersectingCycle != null && intersectingCycle.size() > 1) {
                final var a = piInverse.get(nextPos);
                final var b = intersectingCycle.image(a);
                if (isOutOfInterval(piInverse.indexOf(b), aPos, bPos)) {
                    return Optional.of(intersectingCycle.startingBy(a));
                }
            }
            nextPos = (nextPos + 1) % piInverse.size();
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

    protected Optional<List<Cycle>> searchFor3_2_Seq(final List<Cycle> mu, final Cycle pi) {
        return searchForSeq(mu, pi, _3_2_sortings);
    }

    protected Optional<List<Cycle>> searchFor11_8_Seq(final List<Cycle> mu, final Cycle pi) {
        return searchForSeq(mu, pi, _11_8_sortings);
    }

    private Optional<List<Cycle>> searchForSeq(final List<Cycle> mu, final Cycle pi,
                                               final Multimap<Integer, Pair<Configuration, List<Cycle>>> sortings) {
        final var allSymbols = mu.stream().flatMap(c -> Ints.asList(c.getSymbols()).stream()).collect(Collectors.toSet());
        final var _pi = new IntArrayList(allSymbols.size());
        for (final var symbol : pi.getSymbols()) {
            if (allSymbols.contains(symbol)) {
                _pi.add(symbol);
            }
        }

        final var config = new Configuration(new MulticyclePermutation(mu), Cycle.create(_pi));

        var sorting= sortings.get(config.hashCode());
        if (sorting != null) {
            final var pair = sortings.get(config.hashCode()).stream()
                    .filter(p -> p.getFirst().equals(config)).findFirst();

            if (pair.isPresent()) {
                return Optional.of(config.translatedSorting(pair.get().getFirst(), pair.get().getSecond()));
            }
        }

        return Optional.empty();
    }

    protected Optional<List<Cycle>> searchForSeqBadSmallComponents(final List<Cycle> badSmallComponents,
                                                                   final Cycle pi) {

        final var spi = new MulticyclePermutation(badSmallComponents);
        final var subConfig = new Configuration(spi, removeExtraSymbols(spi.getSymbols(), pi));
        var pair = _11_8_sortings.get(subConfig.hashCode())
                .stream().filter(p -> p.getFirst().equals(subConfig)).findFirst();
        if (pair.isPresent()) {
            final var sorting = subConfig.translatedSorting(pair.get().getFirst(), pair.get().getSecond());
            return Optional.of(sorting);
        }

        return Optional.empty();
    }


    protected Pair<List<Cycle>, Cycle> apply3_2_Unoriented(final MulticyclePermutation spi, final Cycle pi) {
        final var segment = spi.getNonTrivialCycles().stream().findFirst().get();
        List<Cycle> mu = new ArrayList<>();
        mu.add(Cycle.create(segment.get(0), segment.get(1), segment.get(2)));
        for (var i = 0; i < 2; i++) {
            mu = ehExtend(mu, spi, pi);
            final var moves = searchForSeq(mu, pi, _3_2_sortings);
            if (moves.isPresent()) {
                return new Pair<>(moves.get(), applyMoves(pi, moves.get()));
            }
        }

        // the article contains the proof that there will always be a (3,2)-sequence at this point
        throw new RuntimeException("ERROR");
    }
}
