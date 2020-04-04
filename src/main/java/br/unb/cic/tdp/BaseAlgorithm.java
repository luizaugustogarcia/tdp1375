package br.unb.cic.tdp;

import br.unb.cic.tdp.base.CommonOperations;
import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.permutation.PermutationGroups;
import br.unb.cic.tdp.proof.ProofGenerator;
import cern.colt.list.ByteArrayList;
import com.google.common.base.Throwables;
import com.google.common.primitives.Bytes;
import org.apache.commons.math3.util.Pair;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static br.unb.cic.tdp.base.CommonOperations.*;
import static br.unb.cic.tdp.permutation.PermutationGroups.computeProduct;

public abstract class BaseAlgorithm {

    protected Pair<Map<Configuration, List<Cycle>>, Map<Integer, List<Configuration>>> _11_8cases;
    protected Pair<Map<Configuration, List<Cycle>>, Map<Integer, List<Configuration>>> _3_2cases;

    public BaseAlgorithm() {
        final var _11_8sortings = new HashMap<Configuration, List<Cycle>>();
        loadSortings("cases/cases-oriented-7cycle.txt").forEach(_11_8sortings::put);
        loadSortings("cases/cases-dfs.txt").forEach(_11_8sortings::put);
        loadSortings("cases/cases-comb.txt").forEach(_11_8sortings::put);
        _11_8cases = new Pair<>(_11_8sortings, _11_8sortings.keySet().stream()
                .collect(Collectors.groupingBy(Configuration::hashCode)));

        final var _3_2sortings = new HashMap<Configuration, List<Cycle>>();
        loadSortings("cases/cases-3,2.txt").forEach(_3_2sortings::put);
        _3_2cases = new Pair<>(_3_2sortings, _3_2sortings.keySet().stream()
                .collect(Collectors.groupingBy(Configuration::hashCode)));
    }

    public int getNorm(final Collection<Cycle> mu) {
        return mu.stream().mapToInt(Cycle::getNorm).sum();
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

    protected List<Cycle> extend(final List<Cycle> mu, final MulticyclePermutation spi, Cycle pi) {
        final var piInverse = pi.getInverse().getStartingBy(pi.getMinSymbol());
        final Set<Byte> muSymbols = mu.stream().flatMap(c -> Bytes.asList(c.getSymbols()).stream())
                .collect(Collectors.toSet());

        final var muCycleIndex = cycleIndex(mu, pi);
        final var spiCycleIndex = cycleIndex(spi, pi);

        // Type 1 extension
        // These two outer loops are O(1), since at this point, ||mu|| never
        // exceeds 16
        for (final var muCycle : mu) {
            for (var i = 0; i < muCycle.getSymbols().length; i++) {
                final var a = piInverse.indexOf(muCycle.get(i));
                final var b = piInverse.indexOf(muCycle.image(muCycle.get(i)));

                for (int j = 0; j < piInverse.size(); j++) {
                    final var intersecting = spiCycleIndex[piInverse.get(j)];
                    if (!intersecting.equals(muCycle) && !contains(muSymbols, intersecting)) {
                        if (a < j && j < b) {
                            for (final var symbol : intersecting.getStartingBy(piInverse.get(j)).getSymbols()) {
                                final var nextSymbol = intersecting.image(symbol);
                                final var index = piInverse.indexOf(nextSymbol);
                                if (index > b || index < a) {
                                    final List<Cycle> newMu = new ArrayList<>(mu);
                                    newMu.add(new Cycle(symbol, nextSymbol, intersecting.image(nextSymbol)));
                                    return newMu;
                                }
                            }
                        } else if (a > j && j > b) {
                            for (final var symbol : intersecting.getSymbols()) {
                                final var nextSymbol = intersecting.image(symbol);
                                final var index = piInverse.indexOf(nextSymbol);
                                if (a < index || index < b) {
                                    final List<Cycle> newMu = new ArrayList<>(mu);
                                    newMu.add(new Cycle(symbol, nextSymbol, intersecting.image(nextSymbol)));
                                    return newMu;
                                }
                            }
                        }
                    }
                }
            }
        }

        // Type 2 extension
        // O(n)
        for (final var muCycle : mu) {
            for (var i = 0; i < muCycle.getSymbols().length; i++) {
                final var left = piInverse.indexOf(muCycle.get(i));
                final var right = piInverse.indexOf(muCycle.image(muCycle.get(i)));
                final var gates = left < right ? right - left : piInverse.size() - (left - right);
                for (var j = 1; j < gates; j++) {
                    final var index = (j + left) % piInverse.size();
                    if (muCycleIndex[piInverse.get(index)] == null) {
                        final var intersectingCycle = spiCycleIndex[piInverse.get(index)];
                        if (intersectingCycle != null && intersectingCycle.size() > 1
                                && !contains(muSymbols, spiCycleIndex[intersectingCycle.get(0)])) {
                            final var a = piInverse.get(index);
                            final var b = intersectingCycle.image(a);
                            if (isOutOfInterval(piInverse.indexOf(b), left, right)) {
                                final var c = intersectingCycle.image(b);
                                final List<Cycle> newMu = new ArrayList<>(mu);
                                newMu.add(new Cycle(a, b, c));
                                return newMu;
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    private Map<Configuration, List<Cycle>> loadSortings(final String resource) {
        try {
            final Path file = Paths.get(ProofGenerator.class.getClassLoader().getResource(resource).toURI());
            final var result = new HashMap<Configuration, List<Cycle>>();
            final var br = new BufferedReader(new FileReader(file.toFile()), 1024 * 1024);

            String line;
            while ((line = br.readLine()) != null) {
                final var lineSplit = line.trim().split("->");
                final var spi = new MulticyclePermutation(lineSplit[0].replace(" ", ","));
                final var sorting = Arrays.stream(lineSplit[1].substring(1, lineSplit[1].length() - 1)
                        .split(", ")).map(c -> c.replace(" ", ",")).map(Cycle::new)
                        .collect(Collectors.toList());
                final var config = new Configuration(spi, CANONICAL_PI[spi.getNumberOfSymbols()]);
                result.put(config, sorting);
            }

            return result;
        } catch (IOException | URISyntaxException e) {
            Throwables.propagate(e);
        }
        return null;
    }

    abstract int sort(Cycle pi);

    protected void apply2MoveTwoOddCycles(final MulticyclePermutation spi, final Cycle pi) {
        final var evenCycles = spi.getNumberOfEvenCycles();
        final var oddCycles = spi.stream().filter(c -> !c.isEven()).collect(Collectors.toList());
        for (final var c1 : oddCycles)
            for (final var c2 : oddCycles)
                if (c1 != c2) {
                    for (final var a : getSegmentsOfLength2(c1))
                        for (final var b : getSegmentsOfLength2(c2)) {
                            for (final var rho : CommonOperations
                                    .combinations(Arrays.asList(a.get(0), a.get(1), b.get(0), b.get(1)), 3)) {
                                final var rho1 = new Cycle(rho.getVector().stream().mapToInt(i -> i).toArray());
                                if (pi.isApplicable(rho1)
                                        && (computeProduct(spi, rho1.getInverse())).getNumberOfEvenCycles()
                                        - evenCycles == 2) {
                                    applyMoves(pi, Collections.singletonList(rho1));
                                }
                            }
                        }
                }
    }

    protected boolean thereAreOddCycles(final MulticyclePermutation spi) {
        return spi.stream().anyMatch(c -> !c.isEven());
    }

    protected Pair<Cycle, Cycle> searchFor2_2Seq(final MulticyclePermutation spi, final Cycle pi) {
        final var oddCycles = spi.stream().filter(c -> !c.isEven()).collect(Collectors.toList());

        for /* O(n) */ (final var c1 : oddCycles)
            for /* O(n) */ (final var c2 : oddCycles)
                if (c1 != c2) {
                    for /* O(n) */ (final var a : getSegmentsOfLength2(c1))
                        for (final Byte b : c2.getSymbols()) {
                            for (final var rho : CommonOperations
                                    .combinations(Arrays.asList(a.get(0), a.get(1), b), 3)) {
                                final var rho1 = new Cycle(rho.getVector().stream().mapToInt(i -> i).toArray());
                                if (pi.isApplicable(rho1)) {
                                    final var _spi = PermutationGroups
                                            .computeProduct(spi, rho1.getInverse());
                                    final var _pi = applyTransposition(pi, rho1);
                                    final var rho2 = searchFor2MoveOddCycles(_spi, _pi);
                                    if (rho2 != null)
                                        return new Pair<>(rho1, rho2);
                                }
                            }
                        }
                }

        for (final var cycle : spi.stream().filter(c -> isOriented(pi, c))
                .collect(Collectors.toList())) {
            final var before = cycle.isEven() ? 1 : 0;
            for (var i = 0; i < cycle.size() - 2; i++) {
                for (var j = i + 1; j < cycle.size() - 1; j++) {
                    for (var k = j + 1; k < cycle.size(); k++) {
                        final var a = cycle.get(i);
                        final var b = cycle.get(j);
                        final var c = cycle.get(k);
                        if (pi.isOriented(a, b, c)) {
                            var after = cycle.getK(a, b) % 2 == 1 ? 1 : 0;
                            after += cycle.getK(b, c) % 2 == 1 ? 1 : 0;
                            after += cycle.getK(c, a) % 2 == 1 ? 1 : 0;
                            if (after - before == 2) {
                                final var rho1 = new Cycle(a, b, c);
                                final var _spi = PermutationGroups.computeProduct(spi,
                                        rho1.getInverse());
                                final var _pi = applyTransposition(pi, rho1);
                                final var rho2 = searchFor2MoveOddCycles(_spi, _pi);
                                if (rho2 != null)
                                    return new Pair<>(rho1, rho2);
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    protected void applyMoves(final Cycle pi, final List<Cycle> rhos) {
        var _pi = pi;
        for (final var rho : rhos) {
            _pi = applyTransposition(_pi, rho);
        }
        pi.redefine(_pi.getSymbols());
    }

    protected List<Cycle> searchForSeq(final List<Cycle> mu, final Cycle pi,
                                       final Pair<Map<Configuration, List<Cycle>>, Map<Integer, List<Configuration>>> _cases) {
        final var allSymbols = mu.stream().flatMap(c -> Bytes.asList(c.getSymbols()).stream()).collect(Collectors.toSet());
        final var _pi = new ByteArrayList(7);
        for (final var symbol : pi.getSymbols()) {
            if (allSymbols.contains(symbol)) {
                _pi.add(symbol);
            }
        }

        final var config = new Configuration(new MulticyclePermutation(mu), new Cycle(_pi));
        if (_cases.getFirst().containsKey(config)) {
            return config.translatedSorting(_cases.getSecond().get(config.hashCode()).stream()
                    .filter(_c -> _c.equals(config)).findFirst().get(), _cases.getFirst().get(config));
        }

        return null;
    }

    protected void apply3_2_Unoriented(final MulticyclePermutation spi, final Cycle pi) {
        final var initialFactor = spi.stream().filter(c -> c.size() > 1).findFirst().get();
        List<Cycle> mu = new ArrayList<>();
        mu.add(new Cycle(initialFactor.get(0), initialFactor.get(1), initialFactor.get(2)));
        for (var i = 0; i < 2; i++) {
            mu = extend(mu, spi, pi);
            final var rhos = searchForSeq(mu, pi, _3_2cases);
            if (rhos != null) {
                applyMoves(pi, rhos);
                return;
            }
        }
    }
}
