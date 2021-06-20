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

    protected abstract Pair<Map<Configuration, List<Cycle>>, Map<Integer, List<Configuration>>> load11_8Cases();

    public static boolean isOpenGate(int left, int right, Cycle[] symbolToMuCycles, Collection<Cycle> mu,
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

    public int get3Norm(final Collection<Cycle> mu) {
        final var numberOfEvenCycles = (int) mu.stream().filter((cycle) -> cycle.size() % 2 == 1).count();
        final var numberOfSymbols = mu.stream().mapToInt(Cycle::size).sum();
        return (numberOfSymbols - numberOfEvenCycles) / 2;
    }

    protected boolean isOutOfInterval(final int x, final int left, final int right) {
        if (left < right)
            return x < left || x > right;
        return false;
    }

    protected boolean contains(final Set<Byte> muSymbols, final Cycle cycle) {
        for (final Byte symbol : cycle.getSymbols())
            if (muSymbols.contains(symbol))
                return true;
        return false;
    }

    protected List<Cycle> extend(final List<Cycle> mu, final MulticyclePermutation spi, final Cycle pi) {
        final var piInverse = pi.getInverse().getStartingBy(pi.getMinSymbol());
        final var muSymbols = new HashSet<Byte>();

        final var symbolToMuCycles = new Cycle[piInverse.size()];
        // O(1), since at this point, ||mu|| never exceeds 16
        for (Cycle muCycle : mu)
            for (int i = 0; i < muCycle.getSymbols().length; i++) {
                symbolToMuCycles[muCycle.getSymbols()[i]] = muCycle;
                muSymbols.add(muCycle.getSymbols()[i]);
            }

        final var symbolToSigmaPiInverseCycles = new Cycle[piInverse.size()];
        for (Cycle cycle : spi)
            for (int i = 0; i < cycle.getSymbols().length; i++)
                symbolToSigmaPiInverseCycles[cycle.getSymbols()[i]] = cycle;

        // Type 1 extension
        // These two outer loops are O(1), since at this point, ||mu|| never
        // exceeds 16
        for (Cycle muCycle : mu) {
            for (int i = 0; i < muCycle.getSymbols().length; i++) {
                final var left = piInverse.indexOf(muCycle.get(i));
                final var right = piInverse.indexOf(muCycle.image(muCycle.get(i)));
                // O(n)
                if (isOpenGate(left, right, symbolToMuCycles, mu, piInverse)) {
                    final var intersectingCycle = getIntersectingCycle(left, right, symbolToSigmaPiInverseCycles, piInverse);
                    if (intersectingCycle.isPresent()
                            && !contains(muSymbols, symbolToSigmaPiInverseCycles[intersectingCycle.get().get(0)])) {
                        byte a = intersectingCycle.get().get(0), b = intersectingCycle.get().image(a),
                                c = intersectingCycle.get().image(b);
                        final var newMu = new ArrayList<>(mu);
                        newMu.add(new Cycle(a, b, c));
                        return newMu;
                    }
                }
            }
        }

        // Type 2 extension
        // O(n)
        for (Cycle muCycle : mu) {
            for (int i = 0; i < muCycle.getSymbols().length; i++) {
                final var left = piInverse.indexOf(muCycle.get(i));
                final var right = piInverse.indexOf(muCycle.image(muCycle.get(i)));
                final var gates = left < right ? right - left : piInverse.size() - (left - right);
                for (int j = 1; j < gates; j++) {
                    final var index = (j + left) % piInverse.size();
                    if (symbolToMuCycles[piInverse.get(index)] == null) {
                        final var intersectingCycle = symbolToSigmaPiInverseCycles[piInverse.get(index)];
                        if (intersectingCycle != null && intersectingCycle.size() > 1
                                && !contains(muSymbols, symbolToSigmaPiInverseCycles[intersectingCycle.get(0)])) {
                            final var a = piInverse.get(index);
                            final var b = intersectingCycle.image(a);
                            if (isOutOfInterval(piInverse.indexOf(b), left, right)) {
                                final var c = intersectingCycle.image(b);
                                final var newMu = new ArrayList<>(mu);
                                newMu.add(new Cycle(a, b, c));
                                return newMu;
                            }
                        }
                    }
                }
            }
        }

        return mu;
    }

    private Optional<Cycle> getIntersectingCycle(int left, int right, Cycle[] symbolToSigmaPiInverseCycles,
                                        Cycle piInverse) {
        final var gates = left < right ? right - left : piInverse.size() - (left - right);
        for (int i = 1; i < gates; i++) {
            final var index = (i + left) % piInverse.size();
            final var intersectingCycle = symbolToSigmaPiInverseCycles[piInverse.get(index)];
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

    public abstract int sort(Cycle pi);

    protected void apply2MoveTwoOddCycles(final MulticyclePermutation spi, final Cycle pi) {
        final var oddCycles = spi.stream().filter(c -> !c.isEven()).limit(2).collect(Collectors.toList());
        byte a = oddCycles.get(0).get(0), b = oddCycles.get(0).get(1), c = oddCycles.get(1).get(0);

        if (areSymbolsInCyclicOrder(pi, a, b, c)) {
            applyMoves(pi, Collections.singletonList(new Cycle(a, b, c)));
        } else {
            applyMoves(pi, Collections.singletonList(new Cycle(a, c, b)));
        }
    }

    protected boolean thereAreOddCycles(final MulticyclePermutation spi) {
        return spi.stream().anyMatch(c -> !c.isEven());
    }

    protected Pair<Cycle, Cycle> searchFor2_2Seq(final MulticyclePermutation spi, final Cycle pi) {
        for (Pair<Cycle, Integer> move : (Iterable<Pair<Cycle, Integer>>) generateAll0And2Moves(spi, pi)
                .filter(r -> r.getSecond() == 2)::iterator) {
            final var _spi = PermutationGroups
                    .computeProduct(spi, move.getFirst().getInverse());
            final var _pi = applyTransposition(pi, move.getFirst());
            final var secondMove = generateAll0And2Moves(_spi, _pi).filter(r -> r.getSecond() == 2).findFirst();
            if (secondMove.isPresent())
                return new Pair<>(move.getFirst(), secondMove.get().getFirst());
        }

        return null;
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

    protected void apply3_2_Unoriented(final MulticyclePermutation spi, final Cycle pi) {
        final var segment = spi.stream().filter(c -> c.size() > 1).findFirst().get();
        List<Cycle> mu = new ArrayList<>();
        mu.add(new Cycle(segment.get(0), segment.get(1), segment.get(2)));
        for (var i = 0; i < 2; i++) {
            mu = extend(mu, spi, pi);
            final var moves = searchForSeq(mu, pi, _3_2cases);
            if (moves.isPresent()) {
                applyMoves(pi, moves.get());
                return;
            }
        }
    }
}
