package br.unb.cic.tdp;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.permutation.PermutationGroups;
import br.unb.cic.tdp.proof.ProofGenerator;
import br.unb.cic.tdp.util.Pair;
import cern.colt.list.IntArrayList;
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
import static br.unb.cic.tdp.permutation.PermutationGroups.computeProduct;

public class Silvaetal extends BaseAlgorithm {

    public Pair<Cycle, List<Cycle>> transform(Cycle pi, Cycle sigma) {
        final var n = pi.size();

        final var initialPi = pi;

        final var sorting = new ArrayList<Cycle>();

        var spi = computeProduct(true, n, sigma, pi.getInverse());

        final var _2_2Seq = searchFor2_2Seq(spi, pi);
        if (_2_2Seq.isPresent()) {
            pi = computeProduct(_2_2Seq.get().getSecond(), _2_2Seq.get().getFirst(), pi).asNCycle();
            spi = computeProduct(true, pi.size(), sigma, pi.getInverse());
            sorting.addAll(Arrays.asList(_2_2Seq.get().getFirst(), _2_2Seq.get().getSecond()));
        }

        while (thereAreOddCycles(spi)) {
            final var pair = apply2MoveTwoOddCycles(spi, pi);
            sorting.add(pair.getFirst());
            pi = pair.getSecond();
            spi = computeProduct(true, pi.size(), sigma, pi.getInverse());
        }

        final List<List<Cycle>> badSmallComponents = new ArrayList<>();

        final var nonBadSmallComponents = new HashSet<>(spi.getNonTrivialCycles());
        while (!nonBadSmallComponents.isEmpty()) {
            final var _2move = searchFor2MoveFromOrientedCycle(nonBadSmallComponents, pi);
            if (_2move.isPresent()) {
                pi = computeProduct(_2move.get(), pi).asNCycle();
                spi = computeProduct(true, pi.size(), sigma, pi.getInverse());
                sorting.add(_2move.get());
            } else {
                final var orientedCycle = searchForOrientedCycleBiggerThan5(nonBadSmallComponents, pi);
                if (orientedCycle != null) {
                    final var pair = apply4_3SeqOrientedCase(orientedCycle, pi);
                    sorting.addAll(pair.getFirst());
                    pi = pair.getSecond();
                    spi = computeProduct(true, pi.size(), sigma, pi.getInverse());
                } else {
                    List<Cycle> component = new ArrayList<>();
                    final var gamma = nonBadSmallComponents.stream().findFirst().get();
                    component.add(Cycle.create(gamma.get(0), gamma.get(1), gamma.get(2)));

                    var badSmallComponent = false;

                    for (var i = 0; i < 8; i++) {
                        final var norm = get3Norm(component);

                        component = extend(component, spi, pi);

                        if (norm == get3Norm(component)) {
                            badSmallComponent = true;
                            break;
                        }

                        final var seq = searchFor11_8_Seq(component, pi);
                        if (seq.isPresent()) {
                            for (final var move : seq.get())
                                pi = applyTransposition(pi, move);
                            spi = computeProduct(true, pi.size(), sigma, pi.getInverse());
                            sorting.addAll(seq.get());
                            break;
                        }
                    }

                    if (badSmallComponent) {
                        badSmallComponents.add(component);
                    }
                }
            }

            final var badSmallComponentsCycles = badSmallComponents.stream().flatMap(Collection::stream).collect(Collectors.toList());

            if (get3Norm(badSmallComponentsCycles) >= 8) {
                final var _11_8Seq = searchForSeqBadSmallComponents(badSmallComponentsCycles, pi);
                for (final var move : _11_8Seq.get()) {
                    pi = computeProduct(move, pi).asNCycle();
                }
                sorting.addAll(_11_8Seq.get());
                spi = computeProduct(true, pi.size(), sigma, pi.getInverse());
                badSmallComponents.clear();
            }

            nonBadSmallComponents.clear();
            nonBadSmallComponents.addAll(spi.getNonTrivialCycles());
            nonBadSmallComponents.removeAll(badSmallComponentsCycles);
        }

        // At this point 3-norm of spi is less than 8
        while (!spi.isIdentity()) {
            final var _2move = searchFor2MoveFromOrientedCycle(spi, pi);
            if (_2move.isPresent()) {
                pi = computeProduct(_2move.get(), pi).asNCycle();
                sorting.add(_2move.get());
            } else {
                final var pair = apply3_2(spi, pi);
                pi = pair.getSecond();
                sorting.addAll(pair.getFirst());
            }
            spi = computeProduct(true, pi.size(), sigma, pi.getInverse());
        }

        return new Pair<>(initialPi, sorting);
    }

    @SuppressWarnings({"unchecked"})
    public Pair<Cycle, List<Cycle>> sort(Cycle pi) {
        final var n = pi.size();

        final var _sigma = new int[n];
        for (int i = 0; i < pi.size(); i++) {
            _sigma[i] = i;
        }
        final var sigma = Cycle.create(_sigma);

        return transform(pi, sigma);
    }

    @Override
    protected void load11_8Sortings(final Multimap<Integer, Pair<Configuration, List<Cycle>>> sortings) {
        loadSortingsOriented7Cycles("cases/cases-oriented-7cycle.txt", sortings);
        loadSortings("cases/cases-dfs.txt", sortings);
        loadSortings("cases/cases-comb.txt", sortings);
    }

    @SneakyThrows
    private void loadSortingsOriented7Cycles(final String resource, final Multimap<Integer, Pair<Configuration, List<Cycle>>> sortings) {
        final Path file = Paths.get(ProofGenerator.class.getClassLoader().getResource(resource).toURI());
        final var br = new BufferedReader(new FileReader(file.toFile()), 10 * 1024 * 1024);

        String line;
        while ((line = br.readLine()) != null) {
            final var lineSplit = line.trim().split("->");

            final var spi = new MulticyclePermutation(new MulticyclePermutation("(0,3,4,1,5,2,6)"));
            final var sorting = Arrays.stream(lineSplit[1].substring(1, lineSplit[1].length() - 1)
                    .split(", ")).map(c -> c.replace(" ", ",")).map(s -> Cycle.create(s))
                    .collect(Collectors.toList());
            final var config = new Configuration(spi, Cycle.create(lineSplit[0].replace(" ", ",")));
            sortings.put(config.hashCode(), new Pair<>(config, sorting));
        }
    }

    public static List<Cycle> extend(final List<Cycle> config, final MulticyclePermutation spi, final Cycle pi) {
        final var extension = ehExtend(config, spi, pi);

        if (extension != config) {
            return extension;
        }

        final var cycleIndex = cycleIndex(spi, pi);

        // Type 3 extension
        // O(1) since, at this point, ||mu||_3 never exceeds 8
        for (var cycle : config) {
            if (cycle.size() < cycleIndex[cycle.get(0)].size()) {
                final var spiCycle = align(cycleIndex[cycle.get(0)], cycle);
                cycle = cycle.startingBy(spiCycle.get(0));
                final var newSymbols = Arrays.copyOf(cycle.getSymbols(), cycle.size() + 2);
                newSymbols[cycle.size()] = spiCycle
                        .image(cycle.get(cycle.size() - 1));
                newSymbols[cycle.size() + 1] = spiCycle
                        .image(newSymbols[cycle.size()]);

                final List<Cycle> _config = new ArrayList<>(config);
                _config.remove(cycle);
                _config.add(Cycle.create(newSymbols));

                final var openGates = getOpenGates(_config, pi);
                if (openGates.size() <= 2)
                    return _config;
            }
        }

        return config;
    }

    private static Cycle align(final Cycle spiCycle, final Cycle segment) {
        for (var i = 0; i < segment.size(); i++) {
            var symbol = segment.get(i);
            final var index = spiCycle.indexOf(symbol);
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

    private Cycle searchForOrientedCycleBiggerThan5(final Collection<Cycle> spi, final Cycle pi) {
        return spi.stream().filter(c -> c.size() > 5 && isOriented(pi, c)).findFirst()
                .orElse(null);
    }

    private Pair<List<Cycle>, Cycle> apply4_3SeqOrientedCase(final Cycle orientedCycle, final Cycle pi) {
        for (var j = 0; j < orientedCycle.size(); j++) {
            final var a = orientedCycle.get(j);
            final var d = orientedCycle.image(a);
            final var e = orientedCycle.image(d);

            for (var i = 3; i <= orientedCycle.size() - 4; i++) {
                final var b = orientedCycle.get((i + j) % orientedCycle.size());
                final var f = orientedCycle.image(b);

                for (final var l = 5; l <= orientedCycle.size() - 2; i++) {
                    final var c = orientedCycle.get((j + l) % orientedCycle.size());
                    final var g = orientedCycle.image(c);

                    final var _7Cycle = Cycle.create(a, d, e, b, f, c, g);
                    final var allSymbols = new HashSet<>(Ints.asList(_7Cycle.getSymbols()));

                    final var _pi = new IntArrayList(7);
                    for (final var symbol : pi.getSymbols()) {
                        if (allSymbols.contains(symbol)) {
                            _pi.add(symbol);
                        }
                    }

                    final var config = new Configuration(new MulticyclePermutation(_7Cycle), Cycle.create(_pi));
                    if (_11_8_sortings.containsKey(config.hashCode())) {
                        final var pair = _11_8_sortings.get(config.hashCode()).stream()
                                .filter(p -> p.getFirst().equals(config)).findFirst().get();
                        final var moves = config.translatedSorting(pair.getFirst(), pair.getSecond());
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

        if (orientedCycle.isPresent()) {
            return apply3_2BadOriented5Cycle(orientedCycle.get(), pi);
        } else {
            return apply3_2_Unoriented(spi, pi);
        }
    }

    private Pair<List<Cycle>, Cycle> apply3_2BadOriented5Cycle(final Cycle orientedCycle, final Cycle pi) {
        final var a = orientedCycle.get(0);
        final var d = orientedCycle.image(a);
        final var b = orientedCycle.image(d);
        final var e = orientedCycle.image(b);
        final var c = orientedCycle.image(e);

        final var moves = Arrays.asList(Cycle.create(a, b, c), Cycle.create(b, c, d), Cycle.create(c, d, e));

        return new Pair<>(moves, applyMoves(pi, moves));
    }

    public static void main(String[] args) {
        System.out.println("Loading cases into memory...");
        final var silvaetal = new Silvaetal();
        System.out.println("Finished loading...");
        var pi = Cycle.create(args[0]);
        final var moves = silvaetal.sort(pi);
        System.out.println(pi);
        for (Cycle move : moves.getSecond()) {
            pi = PermutationGroups.computeProduct(move, pi).asNCycle();
            System.out.println(pi);
        }
    }
}
