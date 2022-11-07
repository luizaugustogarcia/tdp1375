package br.unb.cic.tdp;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.permutation.PermutationGroups;
import br.unb.cic.tdp.proof.ProofGenerator;
import br.unb.cic.tdp.util.Pair;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import lombok.SneakyThrows;
import lombok.val;

import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static br.unb.cic.tdp.base.CommonOperations.*;
import static br.unb.cic.tdp.proof.ProofGenerator.removeExtraSymbols;
import static java.nio.file.Files.lines;

public abstract class AbstractSbtAlgorithm {

    protected Multimap<Integer, Pair<Configuration, List<Cycle>>> _3_2_sortings = Multimaps.synchronizedMultimap(HashMultimap.create());
    protected Multimap<Integer, Pair<Configuration, List<Cycle>>> _11_8_sortings = Multimaps.synchronizedMultimap(HashMultimap.create());

    private final Pattern INPUT_PATTERN = Pattern.compile("^\\d+(,\\d+)*$");

    private final Supplier<Boolean> INITIALIZER = Suppliers.memoize(() -> {
        System.out.print("Loading cases into memory...");
        loadSortings("cases/cases-3,2.txt", _3_2_sortings);
        load11_8Sortings(_11_8_sortings);
        System.out.println("finished loading.");
        return Boolean.TRUE;
    });

    public Pair<Cycle, List<Cycle>> sort(final String input) {
        if (!INPUT_PATTERN.matcher(input).matches()) {
            throw new RuntimeException("Malformed input");
        }

        val pi = Cycle.create(input);
        if (pi.getMaxSymbol() != pi.size() - 1) {
            throw new RuntimeException("Provide a permutation using all (non-zero) the symbols of {1, 2, ..., n}");
        }

        return sort(pi);
    }

    public Pair<Cycle, List<Cycle>> sort(Cycle pi) {
        INITIALIZER.get();
        return doSort(pi);
    }

    protected abstract Pair<Cycle, List<Cycle>> doSort(final Cycle pi);

    protected abstract void load11_8Sortings(Multimap<Integer, Pair<Configuration, List<Cycle>>> sortings);

    public static int get3Norm(final Collection<Cycle> mu) {
        val numberOfEvenCycles = (int) mu.stream().filter(cycle -> cycle.size() % 2 == 1).count();
        val numberOfSymbols = mu.stream().mapToInt(Cycle::size).sum();
        return (numberOfSymbols - numberOfEvenCycles) / 2;
    }

    public static boolean isOutOfInterval(final int pos, final int aPos, final int bPos) {
        return (aPos < bPos && (pos < aPos || pos > bPos)) || (pos < aPos && pos > bPos);
    }

    private static boolean contains(final Set<Integer> muSymbols, final Cycle cycle) {
        for (final Integer symbol : cycle.getSymbols())
            if (muSymbols.contains(symbol))
                return true;
        return false;
    }

    protected static List<Cycle> ehExtend(final List<Cycle> config, final MulticyclePermutation spi, final Cycle pi) {
        val piInverse = pi.getInverse().startingBy(pi.getMinSymbol());

        val configSymbols = new HashSet<Integer>();
        // O(1), since at this point, ||mu|| never exceeds 16
        for (Cycle cycle : config)
            for (int i = 0; i < cycle.getSymbols().length; i++) {
                configSymbols.add(cycle.getSymbols()[i]);
            }

        val configCycleIndex = cycleIndex(config, pi);
        val spiCycleIndex = cycleIndex(spi, pi);

        final Set<Integer> openGates = getOpenGates(config, pi);

        // Type 1 extension
        // These two outer loops are O(1), since at this point, ||mu|| never
        // exceeds 16
        for (final int openGate : openGates) {
            val cycle = configCycleIndex[openGate];
            val aPos = piInverse.indexOf(openGate);
            val bPos = piInverse.indexOf(cycle.image(openGate));
            val intersectingCycle = getIntersectingCycle(aPos, bPos, spiCycleIndex, piInverse);
            if (intersectingCycle.isPresent()
                    && !contains(configSymbols, spiCycleIndex[intersectingCycle.get().get(0)])) {

                int a = intersectingCycle.get().get(0);
                int b = intersectingCycle.get().image(a);
                int c = intersectingCycle.get().image(b);
                val _config = new ArrayList<>(config);
                _config.add(Cycle.create(a, b, c));

                return _config;
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
                            if (intersectingCycle != null && intersectingCycle.size() > 1
                                    && !contains(configSymbols, spiCycleIndex[intersectingCycle.get(0)])) {
                                val a = piInverse.get(index);
                                val b = intersectingCycle.image(a);
                                if (isOutOfInterval(piInverse.indexOf(b), aPos, bPos)) {
                                    val c = intersectingCycle.image(b);
                                    val _config = new ArrayList<>(config);
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

    @SneakyThrows
    protected void loadSortings(final String resource, final Multimap<Integer, Pair<Configuration, List<Cycle>>> sortings) {
        val file = Paths.get(ProofGenerator.class.getClassLoader().getResource(resource).toURI());

        lines(file).parallel().forEach(line -> {
            val lineSplit = line.trim().split("->");

            val spi = new MulticyclePermutation(lineSplit[0].replace(" ", ","));
            val sorting = Arrays.stream(lineSplit[1].substring(1, lineSplit[1].length() - 1)
                            .split(", ")).map(c -> c.replace(" ", ",")).map(Cycle::create)
                    .collect(Collectors.toList());
            val config = new Configuration(spi, CANONICAL_PI[spi.getNumberOfSymbols()]);
            sortings.put(config.hashCode(), new Pair<>(config, sorting));
        });
    }

    protected Pair<Cycle, Cycle> apply2MoveTwoOddCycles(final MulticyclePermutation spi, final Cycle pi) {
        val oddCycles = spi.stream().filter(c -> !c.isEven()).limit(2).collect(Collectors.toList());
        int a = oddCycles.get(0).get(0);
        int b = oddCycles.get(0).get(1);
        int c = oddCycles.get(1).get(0);

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
            val _spi = PermutationGroups
                    .computeProduct(spi, move.getFirst().getInverse());
            val _pi = applyTranspositionOptimized(pi, move.getFirst());
            val secondMove = generateAll0And2Moves(_spi, _pi).filter(r -> r.getSecond() == 2).findFirst();
            if (secondMove.isPresent())
                return Optional.of(new Pair<>(move.getFirst(), secondMove.get().getFirst()));
        }

        return Optional.empty();
    }

    protected Cycle applyMoves(final Cycle pi, final List<Cycle> moves) {
        var _pi = pi;
        for (val move : moves) {
            _pi = applyTranspositionOptimized(_pi, move);
        }
        return _pi;
    }

    protected Optional<List<Cycle>> searchFor11_8_Seq(final List<Cycle> mu, final Cycle pi) {
        return searchForSeq(mu, pi, _11_8_sortings);
    }

    protected Optional<List<Cycle>> searchForSeq(final Collection<Cycle> mu, final Cycle pi,
                                                 final Multimap<Integer, Pair<Configuration, List<Cycle>>> sortings) {
        val _mu = new MulticyclePermutation(mu);
        val config = new Configuration(_mu, removeExtraSymbols(_mu.getSymbols(), pi));

        val pair = sortings.get(config.hashCode()).stream()
                .filter(p -> p.getFirst().equals(config)).findFirst();

        return pair.map(configurationListPair -> config.translatedSorting(configurationListPair.getFirst(), configurationListPair.getSecond()));
    }

    protected Optional<List<Cycle>> searchForSeqBadSmallComponents(final List<Cycle> badSmallComponents,
                                                                   final Cycle pi) {

        while (!badSmallComponents.isEmpty()) {
            val spi = new MulticyclePermutation(badSmallComponents);
            val subConfig = new Configuration(spi, removeExtraSymbols(spi.getSymbols(), pi));

            var pair = _11_8_sortings.get(subConfig.hashCode())
                    .stream().filter(p -> p.getFirst().equals(subConfig)).findFirst();
            if (pair.isPresent()) {
                val sorting = subConfig.translatedSorting(pair.get().getFirst(), pair.get().getSecond());
                return Optional.of(sorting);
            }

            badSmallComponents.remove(0);
        }

        return Optional.empty();
    }

    protected Pair<List<Cycle>, Cycle> apply3_2_Unoriented(final MulticyclePermutation spi, final Cycle pi) {
        val segment = spi.getNonTrivialCycles().stream().findFirst().get();
        List<Cycle> mu = new ArrayList<>();
        mu.add(Cycle.create(segment.get(0), segment.get(1), segment.get(2)));
        for (var i = 0; i < 2; i++) {
            mu = ehExtend(mu, spi, pi);
            val moves =
                    searchForSeq(mu, removeExtraSymbols(new MulticyclePermutation(mu).getSymbols(), pi), _3_2_sortings);
            if (moves.isPresent()) {
                return new Pair<>(moves.get(), applyMoves(pi, moves.get()));
            }
        }

        // the article contains the proof that there will always be a (3,2)-sequence at this point
        throw new RuntimeException("ERROR");
    }
}
