package br.unb.cic.tdp;

import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.proof.Case;
import cern.colt.list.ByteArrayList;
import org.apache.commons.collections.ListUtils;

import java.util.*;
import java.util.stream.Collectors;

import static br.unb.cic.tdp.base.CommonOperations.*;
import static br.unb.cic.tdp.permutation.PermutationGroups.computeProduct;
import static br.unb.cic.tdp.util.ByteArrayOperations.replace;

public class EliasAndHartman extends BaseAlgorithm {

    public EliasAndHartman(final String casesFolder) {
        super(casesFolder);
    }

    @SuppressWarnings({"unchecked"})
    public int sort(Cycle pi) {
        pi = simplify(pi);

        final var n = pi.size();

        final var array = new byte[pi.size()];
        for (var i = 0; i < pi.size(); i++)
            array[i] = (byte) i;

        final var sigma = new Cycle(array);

        var spi = computeProduct(true, n, sigma, pi.getInverse());

        var distance = 0;

        final var _2_2Seq = searchFor2_2Seq(spi, pi);
        if (_2_2Seq != null) {
            pi = computeProduct(_2_2Seq.getSecond(), _2_2Seq.getFirst(), pi).asNCycle();
            distance += 2;
            spi = computeProduct(true, sigma, pi.getInverse());
        }

        while (thereAreOddCycles(spi)) {
            apply2MoveTwoOddCycles(spi, pi);
            distance += 1;
            spi = computeProduct(true, sigma, pi.getInverse());
        }

        final List<Collection<Cycle>> badSmallComponents = new ArrayList<>();

        List<Cycle> spiWithOutBadSmallComponents;
        while (!spi.isIdentity() && !(spiWithOutBadSmallComponents = ListUtils.subtract(
                spi.stream().filter(c -> c.size() > 1).collect(Collectors.toList()),
                badSmallComponents.stream().flatMap(Collection::stream).collect(Collectors.toList()))).isEmpty()) {
            final var _2move = searchFor2MoveFromOrientedCycle(spi, pi);
            if (_2move != null) {
                pi = computeProduct(_2move, pi).asNCycle();
                distance += 1;
            } else {
                List<Cycle> mu = new ArrayList<>();
                final var initialFactor = spiWithOutBadSmallComponents.stream().filter(c -> c.size() > 1)
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

                    final var _11_8Seq = searchFor11_8Seq(mu, spi, pi);
                    if (_11_8Seq != null) {
                        for (final var rho : _11_8Seq)
                            pi = computeProduct(rho, pi).asNCycle();
                        distance += _11_8Seq.length;
                        badSmallComponent = false;
                        break;
                    }
                }

                if (badSmallComponent)
                    badSmallComponents.add(mu);
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
                final var _11_8Seq = searchFor11_8Seq(mu, spi, pi);
                for (final var rho : _11_8Seq) {
                    pi = computeProduct(rho, pi).asNCycle();
                }
                distance += _11_8Seq.length;
                spi = computeProduct(true, sigma, pi.getInverse());
            }
        }

        while (!spi.isIdentity()) {
            final var _2move = searchFor2MoveFromOrientedCycle(spi, pi);
            if (_2move != null) {
                pi = computeProduct(_2move, pi).asNCycle();
                distance += 1;
            } else {
                apply3_2_Unoriented(spi, pi);
                distance += 3;
            }
            spi = computeProduct(true, sigma, pi.getInverse());
        }

        return distance;
    }

    private Cycle[] searchFor11_8Seq(final List<Cycle> mu, final MulticyclePermutation spi, final Cycle pi) {
        return searchForSeq(mu, spi, pi, _11_8UnorientedCases.get(mu.size()));
    }

    public static List<Cycle> extend(final List<Cycle> mu, final MulticyclePermutation spi, final Cycle pi) {
        final var piInverse = pi.getInverse().getStartingBy(pi.getMinSymbol());
        final Set<Byte> muSymbols = new HashSet<>();

        final var muCycleIndex = cycleIndex(mu, pi);
        final var spiCycleIndex = cycleIndex(spi, pi);

        // Type 1 extension
        // These two outer loops are O(1), since at this point, ||mu|| never
        // exceeds 16
        for (final var muCycle : mu) {
            for (var i = 0; i < muCycle.getSymbols().length; i++) {
                final var left = piInverse.indexOf(muCycle.get(i));
                final var right = piInverse.indexOf(muCycle.image(muCycle.get(i)));
                // O(n)
                if (isOpenGate(mu, piInverse, muCycleIndex, right, left)) {
                    final var intersectingCycle = getIntersectingCycle(left, right, spiCycleIndex,
                            piInverse);
                    if (intersectingCycle != null
                            && !contains(muSymbols, spiCycleIndex[intersectingCycle.get(0)])) {
                        final var a = intersectingCycle.get(0);
                        final var b = intersectingCycle.image(a);
                        final var c = intersectingCycle.image(b);
                        final List<Cycle> newMu = new ArrayList<>(mu);
                        newMu.add(new Cycle(a, b, c));
                        return newMu;
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

        return mu;
    }

    // O(n)
    public static Cycle[] searchForSeq(final List<Cycle> mu, final MulticyclePermutation spi, final Cycle pi,
                                       final List<Case> cases) {
        if (cases != null) {
            final var cycleIndex = cycleIndex(mu, pi);

            final var symbolsCount = mu.stream().mapToInt(Cycle::size).sum();

            final var _piArrayList = new ByteArrayList(symbolsCount);
            // O(n)
            for (var i = 0; i < pi.getSymbols().length; i++) {
                final var muCycle = cycleIndex[pi.getSymbols()[i]];
                if (muCycle != null)
                    _piArrayList.add(pi.getSymbols()[i]);
            }

            // |_pi| is constant, since ||mu|| is constant
            final var piSymbols = _piArrayList.elements();

            for /* O(1) */ (final var _case : cases)
                if (symbolsCount == _case.getSignature().length) {
                    rotation:
                    for /* O(1) */ (var i = 0; i < piSymbols.length; i++) {
                        final var _piSymbols = getStartingBy(piSymbols, i);

                        final Map<Cycle, Integer> labels = new HashMap<>();
                        /* O(1) */
                        for (byte piSymbol : _piSymbols) {
                            final var muCycle = cycleIndex[piSymbol];
                            if (muCycle != null && !labels.containsKey(muCycle))
                                labels.put(muCycle, labels.size() + 1);
                        }

                        for (final var entry : labels.entrySet())
                            if (entry.getKey().size() != _case.getSpi().get(entry.getValue() - 1).size())
                                continue rotation;

                        for (var j = 0; j < _piSymbols.length; j++) {
                            if (_case.getSignature()[j] != labels.get(cycleIndex[_piSymbols[j]]))
                                continue rotation;
                            else {
                                if (j == _case.getSignature().length - 1) {
                                    final var rhos = new Cycle[_case.getRhos().size()];
                                    for (var k = 0; k < rhos.length; k++) {
                                        final var _rho = Arrays.copyOf(_case.getRhos().get(k).getSymbols(), 3);
                                        replace(_rho, _piSymbols);
                                        rhos[k] = new Cycle(_rho);
                                    }

                                    return rhos;
                                }
                            }
                        }
                    }
                }
        }

        return null;
    }
}
