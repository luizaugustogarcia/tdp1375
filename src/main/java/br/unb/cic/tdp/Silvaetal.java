package br.unb.cic.tdp;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.proof.ProofGenerator;
import br.unb.cic.tdp.util.Pair;
import cern.colt.list.IntArrayList;
import com.google.common.collect.Multimap;
import com.google.common.primitives.Ints;
import lombok.SneakyThrows;
import lombok.val;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static br.unb.cic.tdp.base.CommonOperations.*;

public class Silvaetal extends AbstractSbtAlgorithm {

    public Pair<Cycle, List<Cycle>> transform(Cycle pi, Cycle sigma) {
        val n = pi.size();

        val initialPi = pi;

        val sorting = new ArrayList<Cycle>();

        var spi = sigma.times(pi.getInverse());

        val _2_2Seq = searchFor2_2Seq(spi, pi);
        if (_2_2Seq.isPresent()) {
            pi = _2_2Seq.get().getSecond().times(_2_2Seq.get().getFirst()).times(pi).asNCycle();
            spi = sigma.times(pi.getInverse());
            sorting.addAll(Arrays.asList(_2_2Seq.get().getFirst(), _2_2Seq.get().getSecond()));
        }

        while (thereAreOddCycles(spi)) {
            val pair = apply2MoveTwoOddCycles(spi, pi);
            sorting.add(pair.getFirst());
            pi = pair.getSecond();
            spi = sigma.times(pi.getInverse());
        }

        final List<List<Cycle>> badSmallComponents = new ArrayList<>();

        val nonBadSmallComponents = new HashSet<>(spi.getNonTrivialCycles());
        while (!nonBadSmallComponents.isEmpty()) {
            val _2move = searchFor2MoveFromOrientedCycle(nonBadSmallComponents, pi);
            if (_2move.isPresent()) {
                pi = applyTranspositionOptimized(pi, _2move.get());
                spi = sigma.times(pi.getInverse());
                sorting.add(_2move.get());
            } else {
                val orientedCycle = searchForOrientedCycleBiggerThan5(nonBadSmallComponents, pi);
                if (orientedCycle.isPresent()) {
                    val pair = apply4_3SeqOrientedCase(orientedCycle.get(), pi);
                    sorting.addAll(pair.getFirst());
                    pi = pair.getSecond();
                    spi = sigma.times(pi.getInverse());
                } else {
                    List<Cycle> configuration = new ArrayList<>();
                    val gamma = nonBadSmallComponents.stream().findFirst().get();
                    configuration.add(Cycle.of(gamma.get(0), gamma.get(1), gamma.get(2)));

                    var badSmallComponent = false;

                    for (var i = 0; i < 8; i++) {
                        val norm = get3Norm(configuration);

                        configuration = extend(configuration, spi, pi);

                        if (norm == get3Norm(configuration)) {
                            badSmallComponent = true;
                            break;
                        }

                        val seq = searchFor11_8_Seq(configuration, pi);
                        if (seq.isPresent()) {
                            for (val move : seq.get())
                                pi = applyTranspositionOptimized(pi, move);
                            spi = sigma.times(pi.getInverse());
                            sorting.addAll(seq.get());
                            break;
                        }
                    }

                    if (badSmallComponent) {
                        badSmallComponents.add(configuration);
                    }
                }
            }

            val badSmallComponentsCycles = badSmallComponents.stream().flatMap(Collection::stream).collect(Collectors.toList());

            if (get3Norm(badSmallComponentsCycles) >= 8) {
                val _11_8Seq = searchForSeqBadSmallComponents(badSmallComponentsCycles, pi);
                for (val move : _11_8Seq.get()) {
                    pi = applyTranspositionOptimized(pi, move);
                }
                sorting.addAll(_11_8Seq.get());
                spi = sigma.times(pi.getInverse());
                badSmallComponents.clear();
            }

            nonBadSmallComponents.clear();
            nonBadSmallComponents.addAll(spi.getNonTrivialCycles());
            badSmallComponentsCycles.forEach(nonBadSmallComponents::remove);
        }

        // At this point 3-norm of spi is less than 8
        while (!spi.isIdentity()) {
            val _2move = searchFor2MoveFromOrientedCycle(spi, pi);
            if (_2move.isPresent()) {
                pi = applyTranspositionOptimized(pi, _2move.get());
                sorting.add(_2move.get());
            } else {
                val pair = apply3_2(spi, pi);
                pi = pair.getSecond();
                sorting.addAll(pair.getFirst());
            }
            spi = sigma.times(pi.getInverse());
        }

        return new Pair<>(initialPi, sorting);
    }

    public Pair<Cycle, List<Cycle>> doSort(Cycle pi) {
        val n = pi.size();

        val sigmaPrime = new int[n];
        for (int i = 0; i < pi.size(); i++) {
            sigmaPrime[i] = i;
        }
        val sigma = Cycle.of(sigmaPrime);

        return transform(pi, sigma);
    }

    @Override
    protected void load11_8Sortings(final Multimap<Integer, Pair<Configuration, List<Cycle>>> sortings) {
        loadSortingsOriented7Cycles(sortings);
        loadSortings("cases/cases-dfs.txt", sortings);
        loadSortings("cases/cases-comb.txt", sortings);
    }

    @SneakyThrows
    private void loadSortingsOriented7Cycles(final Multimap<Integer, Pair<Configuration, List<Cycle>>> sortings) {
        final Path file = Paths.get(ProofGenerator.class.getClassLoader().getResource("cases/cases-oriented-7cycle.txt").toURI());
        val br = new BufferedReader(new FileReader(file.toFile()));

        String line;
        while ((line = br.readLine()) != null) {
            val lineSplit = line.trim().split("->");

            val spi = new MulticyclePermutation(new MulticyclePermutation("(0,3,4,1,5,2,6)"));
            val sorting = Arrays.stream(lineSplit[1].substring(1, lineSplit[1].length() - 1)
                            .split(", ")).map(c -> c.replace(" ", ",")).map(Cycle::of)
                    .collect(Collectors.toList());
            val config = new Configuration(spi, Cycle.of(lineSplit[0].replace(" ", ",")));
            sortings.put(config.hashCode(), new Pair<>(config, sorting));
        }
    }

    public static List<Cycle> extend(final List<Cycle> config, final MulticyclePermutation spi, final Cycle pi) {
        val extension = ehExtend(config, spi, pi);

        if (extension != config) {
            return extension;
        }

        val cycleIndex = cycleIndex(spi, pi);

        // Type 3 extension
        // O(1) since, at this point, ||mu||_3 never exceeds 8
        for (var cycle : config) {
            if (cycle.size() < cycleIndex[cycle.get(0)].size()) {
                val spiCycle = align(cycleIndex[cycle.get(0)], cycle);
                cycle = cycle.startingBy(spiCycle.get(0));
                val newSymbols = Arrays.copyOf(cycle.getSymbols(), cycle.size() + 2);
                newSymbols[cycle.size()] = spiCycle
                        .image(cycle.get(cycle.size() - 1));
                newSymbols[cycle.size() + 1] = spiCycle
                        .image(newSymbols[cycle.size()]);

                final List<Cycle> configPrime = new ArrayList<>(config);
                configPrime.remove(cycle);
                configPrime.add(Cycle.of(newSymbols));

                val openGates = getOpenGates(configPrime, pi);
                if (openGates.size() <= 2)
                    return configPrime;
            }
        }

        return config;
    }

    private static Cycle align(final Cycle spiCycle, final Cycle segment) {
        for (var i = 0; i < segment.size(); i++) {
            var symbol = segment.get(i);
            val index = spiCycle.indexOf(symbol);
            var match = true;
            for (var j = 1; j < segment.size(); j++) {
                if (segment.get((i + j) % segment.size()) != spiCycle
                        .get((index + j) % spiCycle.size())) {
                    match = false;
                    break;
                }
                symbol = segment.image(symbol);
            }
            if (match)
                return spiCycle.startingBy(segment.get(i));
        }
        return null;
    }

    private Optional<Cycle> searchForOrientedCycleBiggerThan5(final Collection<Cycle> spi, final Cycle pi) {
        return spi.stream().filter(c -> c.size() > 5 && isOriented(pi, c)).findFirst();
    }

    private Pair<List<Cycle>, Cycle> apply4_3SeqOrientedCase(final Cycle orientedCycle, final Cycle pi) {
        for (var j = 0; j < orientedCycle.size(); j++) {
            val a = orientedCycle.get(j);
            val d = orientedCycle.image(a);
            val e = orientedCycle.image(d);

            for (var i = 3; i <= orientedCycle.size() - 4; i++) {
                val b = orientedCycle.get((i + j) % orientedCycle.size());
                val f = orientedCycle.image(b);

                for (var l = 5; l <= orientedCycle.size() - 2; l++) {
                    val c = orientedCycle.get((j + l) % orientedCycle.size());
                    val g = orientedCycle.image(c);

                    val _7Cycle = Cycle.of(a, d, e, b, f, c, g);
                    val allSymbols = new HashSet<>(Ints.asList(_7Cycle.getSymbols()));

                    val piPrime = new IntArrayList(7);
                    for (val symbol : pi.getSymbols()) {
                        if (allSymbols.contains(symbol)) {
                            piPrime.add(symbol);
                        }
                    }

                    val config = new Configuration(new MulticyclePermutation(_7Cycle), Cycle.of(piPrime));
                    if (_11_8_sortings.containsKey(config.hashCode())) {
                        val pair = _11_8_sortings.get(config.hashCode()).stream()
                                .filter(p -> p.getFirst().equals(config)).findFirst().get();
                        val moves = config.translatedSorting(pair.getFirst(), pair.getSecond());
                        return new Pair<>(moves, applyMoves(pi, moves));
                    }
                }
            }
        }

        // the article contains the proof that there will always be a (4,3)-sequence
        throw new RuntimeException("ERROR");
    }

    private Pair<List<Cycle>, Cycle> apply3_2(final MulticyclePermutation spi, final Cycle pi) {
        var orientedCycle = spi.stream().filter(c -> c.size() == 5 && isOriented(pi, c))
                .findFirst();

        return orientedCycle.map(cycle -> apply3_2BadOriented5Cycle(cycle, pi)).orElseGet(() -> apply3_2_Unoriented(spi, pi));
    }

    private Pair<List<Cycle>, Cycle> apply3_2BadOriented5Cycle(final Cycle orientedCycle, final Cycle pi) {
        val a = orientedCycle.get(0);
        val d = orientedCycle.image(a);
        val b = orientedCycle.image(d);
        val e = orientedCycle.image(b);
        val c = orientedCycle.image(e);

        val moves = Arrays.asList(Cycle.of(a, b, c), Cycle.of(b, c, d), Cycle.of(c, d, e));

        return new Pair<>(moves, applyMoves(pi, moves));
    }

    public static void main(final String[] args) {
        val silvaetal = new Silvaetal();
        val permutation = "0,11,10,9,8,7,6,5,4,3,2,1";
        val moves = silvaetal.sort(permutation);
        var pi = Cycle.of(permutation);
        System.out.println(pi);
        for (final Cycle move : moves.getSecond()) {
            pi = move.times(pi).asNCycle();
            System.out.println(pi);
        }
    }
}
