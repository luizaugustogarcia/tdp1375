package br.unb.cic.tdp;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.util.Pair;
import cern.colt.list.ByteArrayList;
import com.google.common.primitives.Bytes;
import org.apache.commons.collections.ListUtils;

import java.util.*;
import java.util.stream.Collectors;

import static br.unb.cic.tdp.base.CommonOperations.*;
import static br.unb.cic.tdp.permutation.PermutationGroups.computeProduct;

public class Silvaetal extends BaseAlgorithm {

    public Silvaetal() {
        final var _11_8sortings = new HashMap<Configuration, List<Cycle>>();
        loadSortings("cases/cases-oriented-7cycle.txt").forEach(_11_8sortings::put);
        loadSortings("cases/cases-dfs.txt").forEach(_11_8sortings::put);
        loadSortings("cases/cases-comb.txt").forEach(_11_8sortings::put);
        _11_8cases = new Pair<>(_11_8sortings, _11_8sortings.keySet().stream()
                .collect(Collectors.groupingBy(Configuration::hashCode)));
    }

    public static void main(String[] args) {
        final var silvaetal = new Silvaetal();
        System.out.println(silvaetal.sort(new Cycle(args[0])));
    }

    @SuppressWarnings({"unchecked"})
    public int sort(Cycle pi) {
        final var n = pi.size();

        final var _sigma = new byte[pi.size()];
        for (int i = 0; i < pi.size(); i++) {
            _sigma[i] = (byte)i;
        }

        final var sigma = new Cycle(_sigma);

        var spi = computeProduct(true, n, sigma, pi.getInverse());

        var distance = 0;

        // O(n^6)
        final var _2_2Seq = searchFor2_2Seq(spi, pi);
        if (_2_2Seq != null) {
            pi = computeProduct(_2_2Seq.getSecond(), _2_2Seq.getFirst(), pi).asNCycle();
            distance += 2;
            spi = computeProduct(true, sigma, pi.getInverse());
        }

        // O(n^2)
        while (thereAreOddCycles(spi)) {
            apply2MoveTwoOddCycles(spi, pi);
            distance += 1;
            spi = computeProduct(true, sigma, pi.getInverse());
        }

        final List<Collection<Cycle>> badSmallComponents = new ArrayList<>();

        // O(n^4)
        List<Cycle> spiMinusBadSmallComponents;
        while (!spi.isIdentity() && !(spiMinusBadSmallComponents = ListUtils.subtract(
                spi.stream().filter(c -> c.size() > 1).collect(Collectors.toList()),
                badSmallComponents.stream().flatMap(Collection::stream).collect(Collectors.toList()))).isEmpty()) {
            // O(n^3)
            final var _2move = searchFor2MoveFromOrientedCycle(spi, pi);
            if (_2move != null) {
                pi = computeProduct(_2move, pi).asNCycle();
                distance += 1;
            } else {
                final var orientedCycle = searchForOrientedCycleBiggerThan5(spi, pi);
                if (orientedCycle != null) {
                    // O(n^3)
                    distance += apply11_8SeqOrientedCase(orientedCycle, pi);
                } else {
                    List<Cycle> mu = new ArrayList<>();
                    final var initialFactor = spiMinusBadSmallComponents.stream().filter(c -> c.size() > 1)
                            .findFirst().get();
                    mu.add(new Cycle(initialFactor.get(0), initialFactor.get(1), initialFactor.get(2)));

                    var badSmallComponent = false;
                    // O(n)
                    for (var i = 0; i < 8; i++) {
                        final var norm = getNorm(mu);

                        mu = extend(mu, spi, pi);

                        if (norm == getNorm(mu)) {
                            badSmallComponent = true;
                            break;
                        }

                        final var _11_8Seq = searchForSeq(mu, pi, _11_8cases);
                        if (_11_8Seq != null) {
                            for (final var rho : _11_8Seq)
                                pi = computeProduct(rho, pi).asNCycle();
                            distance += _11_8Seq.size();
                            break;
                        }
                    }

                    if (badSmallComponent)
                        badSmallComponents.add(mu);
                }
            }
            spi = computeProduct(true, sigma, pi.getInverse());
        }

        // Bad small components
        final List<Cycle> mu = new ArrayList<>();
        final var iterator = badSmallComponents.iterator();
        while (iterator.hasNext()) {
            mu.addAll(iterator.next());
            iterator.remove();
            if (getNorm(mu) >= 16) {
                final var _11_8Seq = searchForSeq(mu, pi, _3_2cases);
                for (final var rho : _11_8Seq) {
                    pi = computeProduct(rho, pi).asNCycle();
                }
                distance += _11_8Seq.size();
                spi = computeProduct(true, sigma, pi.getInverse());
            }
        }

        // O(1)
        while (!spi.isIdentity()) {
            final var _2move = searchFor2MoveFromOrientedCycle(spi, pi);
            if (_2move != null) {
                pi = computeProduct(_2move, pi).asNCycle();
                distance += 1;
            } else {
                apply3_2(spi, pi);
                distance += 3;
            }
            spi = computeProduct(true, sigma, pi.getInverse());
        }

        return distance;
    }

    public List<Cycle> extend(final List<Cycle> mu, final MulticyclePermutation spi, final Cycle pi) {
        final var extension = super.extend(mu, spi, pi);
        if (extension != null) {
            return extension;
        }

        final var piInverse = pi.getInverse().getStartingBy(pi.getMinSymbol());
        final var cycleIndex = cycleIndex(spi, pi);

        // Type 3 extension
        // The outer loop is O(1) since, at this point, ||mu|| never exceeds 16
        for (var muCycle : mu) {
            if (muCycle.size() < cycleIndex[muCycle.get(0)].size()) {
                final var spiCycle = align(cycleIndex[muCycle.get(0)], muCycle);
                muCycle = muCycle.getStartingBy(spiCycle.get(0));
                final var newSymbols = Arrays.copyOf(muCycle.getSymbols(), muCycle.getSymbols().length + 2);
                newSymbols[muCycle.getSymbols().length] = spiCycle
                        .image(muCycle.get(muCycle.getSymbols().length - 1));
                newSymbols[muCycle.getSymbols().length + 1] = spiCycle
                        .image(newSymbols[muCycle.getSymbols().length]);

                final List<Cycle> newMu = new ArrayList<>(mu);
                newMu.remove(muCycle);
                newMu.add(new Cycle(newSymbols));

                final var openGates = openGatesPerCycle(newMu, piInverse);
                if (openGates.values().stream().mapToInt(j -> j).sum() <= 2)
                    return newMu;
            }
        }

        return mu;
    }

    // O(n)
    public Cycle align(final Cycle spiCycle, final Cycle segment) {
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
                return spiCycle.getStartingBy(segment.get(i));
        }
        return null;
    }

    private Cycle searchForOrientedCycleBiggerThan5(final MulticyclePermutation spi, final Cycle pi) {
        return spi.stream().filter(c -> c.size() > 5 && isOriented(pi, c)).findFirst()
                .orElse(null);
    }

    // O(n^3)
    private int apply11_8SeqOrientedCase(final Cycle orientedCycle, final Cycle pi) {
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

                    final var _7Cycle = new Cycle(a, d, e, b, f, c, g);
                    final var allSymbols = new HashSet<>(Bytes.asList(_7Cycle.getSymbols()));
                    final var _pi = new ByteArrayList(7);
                    for (final var symbol : pi.getSymbols()) {
                        if (allSymbols.contains(symbol)) {
                            _pi.add(symbol);
                        }
                    }

                    final var config = new Configuration(new MulticyclePermutation(_7Cycle), new Cycle(_pi));
                    if (_11_8cases.getFirst().containsKey(config)) {
                        final var rhos = config.translatedSorting(_11_8cases.getSecond().get(config.hashCode()).stream()
                                .filter(_c -> _c.equals(config)).findFirst().get(), _11_8cases.getFirst().get(config));
                        applyMoves(pi, rhos);
                        return 4;
                    }
                }
            }
        }

        throw new RuntimeException("ERROR");
    }

    private void apply3_2(final MulticyclePermutation spi, final Cycle pi) {
        final var orientedCycle = spi.stream().filter(c -> c.size() == 5 && isOriented(pi, c))
                .findFirst().orElse(null);

        if (orientedCycle != null) {
            apply3_2OrientedCase(orientedCycle, spi, pi);
        } else {
            apply3_2_Unoriented(spi, pi);
        }
    }

    private void apply3_2OrientedCase(final Cycle orientedCycle, final MulticyclePermutation spi, final Cycle pi) {
        final var even = spi.getNumberOfEvenCycles();

        for (var i = 0; i <= orientedCycle.size() - 5; i++) {
            final var _0 = orientedCycle.get(0);
            final var _3 = orientedCycle.image(_0);
            final var _1 = orientedCycle.image(_3);

            final var _4 = orientedCycle.get(i + 3);
            final var _2 = orientedCycle.image(_4);

            final var rho1 = new Cycle(_0, _1, _2);
            final var rho2 = new Cycle(_1, _2, _3);
            final var rho3 = new Cycle(_2, _3, _4);

            if (computeProduct(true, spi, rho1.getInverse(), rho2.getInverse(),
                    rho3.getInverse()).getNumberOfEvenCycles() - even >= 4) {
                applyMoves(pi, Arrays.asList(rho1, rho2, rho3));
                return;
            }
        }
    }
}
