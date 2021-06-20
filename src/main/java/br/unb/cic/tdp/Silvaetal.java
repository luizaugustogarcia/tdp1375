package br.unb.cic.tdp;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.permutation.PermutationGroups;
import br.unb.cic.tdp.util.Pair;
import cern.colt.list.ByteArrayList;
import com.google.common.primitives.Bytes;
import org.apache.commons.collections.ListUtils;

import java.util.*;
import java.util.stream.Collectors;

import static br.unb.cic.tdp.base.CommonOperations.*;
import static br.unb.cic.tdp.permutation.PermutationGroups.computeProduct;

public class Silvaetal extends BaseAlgorithm {

    @SuppressWarnings({"unchecked"})
    public int sort(Cycle pi) {
        final var n = pi.size();

        final var sigma = CANONICAL_PI[n];

        var spi = computeProduct(true, n, sigma, pi.getInverse());

        var distance = 0;

        final var _2_2Seq = searchFor2_2Seq(spi, pi);
        if (_2_2Seq != null) {
            pi = computeProduct(_2_2Seq.getSecond(), _2_2Seq.getFirst(), pi).asNCycle();
            spi = computeProduct(true, sigma, pi.getInverse());
            distance += 2;
        }

        while (thereAreOddCycles(spi)) {
            apply2MoveTwoOddCycles(spi, pi);
            spi = computeProduct(true, sigma, pi.getInverse());
            distance += 1;
        }

        final List<Cycle> bigLambda = new ArrayList<>(); // bad small components

        List<Cycle> bigTheta; // unmarked cycles
        while (!(bigTheta = ListUtils.subtract(spi.stream().filter(c -> c.size() > 1).collect(Collectors.toList()), bigLambda)).isEmpty()) {
            final var _2move = searchFor2MoveFromOrientedCycle(bigTheta, pi);
            if (_2move.isPresent()) {
                pi = computeProduct(_2move.get(), pi).asNCycle();
                spi = computeProduct(true, sigma, pi.getInverse());
                distance += 1;
            } else {
                final var orientedCycle = searchForOrientedCycleBiggerThan5(bigTheta, pi);
                if (orientedCycle != null) {
                    distance += apply4_3SeqOrientedCase(orientedCycle, pi);
                    spi = computeProduct(true, sigma, pi.getInverse());
                } else {
                    List<Cycle> bigGamma = new ArrayList<>();
                    final var gamma = bigTheta.stream().filter(c -> c.size() > 1).findFirst().get();
                    bigGamma.add(new Cycle(gamma.get(0), gamma.get(1), gamma.get(2)));

                    var badSmallComponent = false;

                    for (var i = 0; i < 8; i++) {
                        final var norm = get3Norm(bigGamma);

                        bigGamma = extend(bigGamma, spi, pi);

                        if (norm == get3Norm(bigGamma)) {
                            badSmallComponent = true;
                            break;
                        }

                        final var seq = searchForSeq(bigGamma, pi, _11_8cases);
                        if (seq.isPresent()) {
                            for (final var move : seq.get())
                                pi = computeProduct(move, pi).asNCycle();
                            spi = computeProduct(true, sigma, pi.getInverse());
                            distance += seq.get().size();
                            break;
                        }
                    }

                    if (badSmallComponent) {
                        bigLambda.addAll(bigGamma);
                    }
                }
            }

            if (get3Norm(bigLambda) >= 8) {
                final var _11_8Seq = searchForSeq(bigLambda, pi, _11_8cases);
                for (final var move : _11_8Seq.get()) {
                    pi = computeProduct(move, pi).asNCycle();
                }
                distance += _11_8Seq.get().size();
                spi = computeProduct(true, sigma, pi.getInverse());
                bigLambda.clear();
            }
        }

        // At this point 3-norm of spi is less than 8
        while (!spi.isIdentity()) {
            final var _2move = searchFor2MoveFromOrientedCycle(spi, pi);
            if (_2move.isPresent()) {
                pi = computeProduct(_2move.get(), pi).asNCycle();
                distance += 1;
            } else {
                apply3_2(spi, pi);
                distance += 3;
            }
            spi = computeProduct(true, sigma, pi.getInverse());
        }

        return distance;
    }

    @Override
    protected Pair<Map<Configuration, List<Cycle>>, Map<Integer, List<Configuration>>> load11_8Cases() {
        final var _11_8sortings = new HashMap<Configuration, List<Cycle>>();
        loadSortings("cases/cases-oriented-7cycle.txt").forEach(_11_8sortings::put);
        loadSortings("cases/cases-dfs.txt").forEach(_11_8sortings::put);
        loadSortings("cases/cases-comb.txt").forEach(_11_8sortings::put);
        return new Pair<>(_11_8sortings, _11_8sortings.keySet().stream()
                .collect(Collectors.groupingBy(Configuration::hashCode)));
    }

    public List<Cycle> extend(final List<Cycle> mu, final MulticyclePermutation spi, final Cycle pi) {
        final var extension = super.extend(mu, spi, pi);
        if (extension != mu) {
            return extension;
        }

        final var piInverse = pi.getInverse().getStartingBy(pi.getMinSymbol());
        final var cycleIndex = cycleIndex(spi, pi);

        // Type 3 extension
        // O(1) since, at this point, ||mu||_3 never exceeds 8
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

    private Cycle searchForOrientedCycleBiggerThan5(final List<Cycle> mu, final Cycle pi) {
        return mu.stream().filter(c -> c.size() > 5 && isOriented(pi, c)).findFirst()
                .orElse(null);
    }

    private int apply4_3SeqOrientedCase(final Cycle orientedCycle, final Cycle pi) {
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
                        final var moves = config.translatedSorting(_11_8cases.getSecond().get(config.hashCode()).stream()
                                .filter(_c -> _c.equals(config)).findFirst().get(), _11_8cases.getFirst().get(config));
                        applyMoves(pi, moves);
                        return 4;
                    }
                }
            }
        }

        throw new RuntimeException("ERROR");
    }

    private void apply3_2(final MulticyclePermutation spi, final Cycle pi) {
        var orientedCycle = spi.stream().filter(c -> c.size() == 5 && isOriented(pi, c))
                .findFirst();

        if (orientedCycle.isPresent()) {
            apply3_2BadOriented5Cycle(orientedCycle.get(), pi);
        } else {
            apply3_2_Unoriented(spi, pi);
        }
    }

    private void apply3_2BadOriented5Cycle(final Cycle orientedCycle, final Cycle pi) {
        final var a = orientedCycle.get(0);
        final var d = orientedCycle.image(a);
        final var b = orientedCycle.image(d);
        final var e = orientedCycle.image(b);
        final var c = orientedCycle.image(e);

        final var move1 = new Cycle(a, b, c);
        final var move2 = new Cycle(b, c, d);
        final var move3 = new Cycle(c, d, e);

        applyMoves(pi, Arrays.asList(move1, move2, move3));
    }
}
