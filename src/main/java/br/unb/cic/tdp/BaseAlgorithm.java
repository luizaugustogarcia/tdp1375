package br.unb.cic.tdp;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.permutation.PermutationGroups;
import br.unb.cic.tdp.proof.ProofGenerator;
import br.unb.cic.tdp.util.Pair;
import cern.colt.list.ByteArrayList;
import com.google.common.primitives.Bytes;
import lombok.SneakyThrows;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static br.unb.cic.tdp.base.CommonOperations.*;

public abstract class BaseAlgorithm {

    protected Pair<Map<Configuration, List<Cycle>>, Map<Integer, List<Configuration>>> _11_8cases;
    protected Pair<Map<Configuration, List<Cycle>>, Map<Integer, List<Configuration>>> _3_2cases;

    public BaseAlgorithm() {
        final var _3_2sortings = new HashMap<Configuration, List<Cycle>>();
        loadSortings("cases/cases-3,2.txt").forEach(_3_2sortings::put);
        _3_2cases = new Pair<>(_3_2sortings, _3_2sortings.keySet().stream()
                .collect(Collectors.groupingBy(Configuration::hashCode)));
        _11_8cases = load11_8Cases();
    }

    public abstract List<Cycle> sort(Cycle pi);

    protected abstract Pair<Map<Configuration, List<Cycle>>, Map<Integer, List<Configuration>>> load11_8Cases();

    private static boolean isOpenGate(int left, int right, Cycle[] symbolToMuCycles, Collection<Cycle> mu,
                                     Cycle piInverse) {
        int gates = left < right ? right - left : piInverse.size() - (left - right);
        for (int i = 1; i < gates; i++) {
            int index = (i + left) % piInverse.size();
            Cycle cycle = symbolToMuCycles[piInverse.get(index)];
            if (cycle != null && mu.contains(cycle))
                return false;
        }
        return true;
    }

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

    private boolean contains(final Set<Byte> muSymbols, final Cycle cycle) {
        for (final Byte symbol : cycle.getSymbols())
            if (muSymbols.contains(symbol))
                return true;
        return false;
    }

    protected List<Cycle> extend(final List<Cycle> bigGamma, final MulticyclePermutation spi, final Cycle pi) {
        final var piInverse = pi.getInverse().getStartingBy(pi.getMinSymbol());

        final var bigGammaSymbols = new HashSet<Byte>();
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
                final var left = piInverse.indexOf(cycle.get(i));
                final var right = piInverse.indexOf(cycle.image(cycle.get(i)));
                // O(n)
                if (isOpenGate(left, right, bigGammaCycleIndex, bigGamma, piInverse)) {
                    final var intersectingCycle = getIntersectingCycle(left, right, spiCycleIndex, piInverse);
                    if (intersectingCycle.isPresent()
                            && !contains(bigGammaSymbols, spiCycleIndex[intersectingCycle.get().get(0)])) {
                        byte a = intersectingCycle.get().get(0), b = intersectingCycle.get().image(a),
                                c = intersectingCycle.get().image(b);
                        final var _bigGamma = new ArrayList<>(bigGamma);
                        _bigGamma.add(new Cycle(a, b, c));
                        return _bigGamma;
                    }
                }
            }
        }

        // Type 2 extension
        // O(n)
        for (Cycle cycle : bigGamma) {
            for (int i = 0; i < cycle.getSymbols().length; i++) {
                final var left = piInverse.indexOf(cycle.get(i));
                final var right = piInverse.indexOf(cycle.image(cycle.get(i)));
                final var gates = left < right ? right - left : piInverse.size() - (left - right);
                for (int j = 1; j < gates; j++) {
                    final var index = (j + left) % piInverse.size();
                    if (bigGammaCycleIndex[piInverse.get(index)] == null) {
                        final var intersectingCycle = spiCycleIndex[piInverse.get(index)];
                        if (intersectingCycle != null && intersectingCycle.size() > 1
                                && !contains(bigGammaSymbols, spiCycleIndex[intersectingCycle.get(0)])) {
                            final var a = piInverse.get(index);
                            final var b = intersectingCycle.image(a);
                            if (isOutOfInterval(piInverse.indexOf(b), left, right)) {
                                final var c = intersectingCycle.image(b);
                                final var _bigGamma = new ArrayList<>(bigGamma);
                                _bigGamma.add(new Cycle(a, b, c));
                                return _bigGamma;
                            }
                        }
                    }
                }
            }
        }

        return bigGamma;
    }

    private Optional<Cycle> getIntersectingCycle(int left, int right, Cycle[] spiCycleIndex,
                                        Cycle piInverse) {
        final var gates = left < right ? right - left : piInverse.size() - (left - right);
        for (int i = 1; i < gates; i++) {
            final var index = (i + left) % piInverse.size();
            final var intersectingCycle = spiCycleIndex[piInverse.get(index)];
            if (intersectingCycle != null && intersectingCycle.size() > 1) {
                final var a = piInverse.get(index);
                final var b = intersectingCycle.image(a);
                if (isOutOfInterval(piInverse.indexOf(b), left, right)) {
                    return Optional.of(intersectingCycle.getStartingBy(a));
                }
            }
        }
        return Optional.empty();
    }

    @SneakyThrows
    protected Map<Configuration, List<Cycle>> loadSortings(final String resource) {
        final var threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        final Path file = Paths.get(ProofGenerator.class.getClassLoader().getResource(resource).toURI());
        final var result = new ConcurrentHashMap<Configuration, List<Cycle>>();
        final var br = new BufferedReader(new FileReader(file.toFile()), 10 * 1024 * 1024);

        String line;
        while ((line = br.readLine()) != null) {
            final var lineSplit = line.trim().split("->");
            threadPool.submit(() -> {
                final var spi = new MulticyclePermutation(lineSplit[0].replace(" ", ","));
                final var sorting = Arrays.stream(lineSplit[1].substring(1, lineSplit[1].length() - 1)
                        .split(", ")).map(c -> c.replace(" ", ",")).map(Cycle::new)
                        .collect(Collectors.toList());
                final var config = new Configuration(spi, CANONICAL_PI[spi.getNumberOfSymbols()]);
                result.computeIfAbsent(config, k -> sorting);
            });
        }

        threadPool.shutdown();

        threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);

        return result;
    }

    protected Cycle apply2MoveTwoOddCycles(final MulticyclePermutation spi, final Cycle pi) {
        final var oddCycles = spi.stream().filter(c -> !c.isEven()).limit(2).collect(Collectors.toList());
        byte a = oddCycles.get(0).get(0), b = oddCycles.get(0).get(1), c = oddCycles.get(1).get(0);

        final Cycle _2Move;
        if (areSymbolsInCyclicOrder(pi, a, b, c)) {
            _2Move = new Cycle(a, b, c);
        } else {
            _2Move = new Cycle(a, c, b);
        }

        applyMoves(pi, Collections.singletonList(_2Move));

        return _2Move;
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

    protected void applyMoves(final Cycle pi, final List<Cycle> moves) {
        var _pi = pi;
        for (final var move : moves) {
            _pi = applyTransposition(_pi, move);
        }
        pi.redefine(_pi.getSymbols());
    }

    protected Optional<List<Cycle>> searchForSeq(final List<Cycle> mu, final Cycle pi,
                                       final Pair<Map<Configuration, List<Cycle>>, Map<Integer, List<Configuration>>> cases) {
        final var allSymbols = mu.stream().flatMap(c -> Bytes.asList(c.getSymbols()).stream()).collect(Collectors.toSet());
        final var _pi = new ByteArrayList(allSymbols.size());
        for (final var symbol : pi.getSymbols()) {
            if (allSymbols.contains(symbol)) {
                _pi.add(symbol);
            }
        }

        final var config = new Configuration(new MulticyclePermutation(mu), new Cycle(_pi));
        if (cases.getFirst().containsKey(config)) {
            return Optional.of(config.translatedSorting(cases.getSecond().get(config.hashCode()).stream()
                    .filter(_c -> _c.equals(config)).findFirst().get(), cases.getFirst().get(config)));
        }

        return Optional.empty();
    }

    protected List<Cycle> apply3_2_Unoriented(final MulticyclePermutation spi, final Cycle pi) {
        final var segment = spi.stream().filter(c -> c.size() > 1).findFirst().get();
        List<Cycle> mu = new ArrayList<>();
        mu.add(new Cycle(segment.get(0), segment.get(1), segment.get(2)));
        for (var i = 0; i < 2; i++) {
            mu = extend(mu, spi, pi);
            final var moves = searchForSeq(mu, pi, _3_2cases);
            if (moves.isPresent()) {
                applyMoves(pi, moves.get());
                return moves.get();
            }
        }

        // the article contains the proof that there will always be a (3,2)-sequence at this point
        throw new RuntimeException("ERROR");
    }
}
