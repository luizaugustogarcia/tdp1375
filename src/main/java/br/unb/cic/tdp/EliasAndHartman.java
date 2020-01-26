package br.unb.cic.tdp;

import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.proof.Case;
import cern.colt.list.ByteArrayList;
import org.apache.commons.collections.ListUtils;

import java.util.*;
import java.util.stream.Collectors;

import static br.unb.cic.tdp.permutation.PermutationGroups.computeProduct;

public class EliasAndHartman extends BaseAlgorithm {

    public EliasAndHartman(final String casesFolder) {
        super(casesFolder);
    }

    @SuppressWarnings({"unchecked"})
    public int sort(Cycle pi) {
        pi = CommonOperations.simplify(pi);

        final var n = pi.size();

        final var array = new byte[pi.size()];
        for (var i = 0; i < pi.size(); i++)
            array[i] = (byte) i;

        final var sigma = new Cycle(array);

        var sigmaPiInverse = computeProduct(true, n, sigma, pi.getInverse());

        var distance = 0;

        final var _2_2Seq = searchFor2_2Seq(sigmaPiInverse, pi);
        if (_2_2Seq != null) {
            pi = computeProduct(_2_2Seq.getSecond(), _2_2Seq.getFirst(), pi).asNCycle();
            distance += 2;
            sigmaPiInverse = computeProduct(true, sigma, pi.getInverse());
        }

        while (thereAreOddCycles(sigmaPiInverse)) {
            apply2MoveTwoOddCycles(sigmaPiInverse, pi);
            distance += 1;
            sigmaPiInverse = computeProduct(true, sigma, pi.getInverse());
        }

        final List<Collection<Cycle>> badSmallComponents = new ArrayList<>();

        List<Cycle> sigmaPiInverseWithoOutBadSmallComponents;
        while (!sigmaPiInverse.isIdentity() && !(sigmaPiInverseWithoOutBadSmallComponents = ListUtils.subtract(
                sigmaPiInverse.stream().filter(c -> c.size() > 1).collect(Collectors.toList()),
                badSmallComponents.stream().flatMap(Collection::stream).collect(Collectors.toList()))).isEmpty()) {
            final var _2move = CommonOperations.searchFor2MoveFromOrientedCycle(sigmaPiInverse, pi);
            if (_2move != null) {
                pi = computeProduct(_2move, pi).asNCycle();
                distance += 1;
            } else {
                Set<Cycle> mu = new HashSet<>();
                final var initialFactor = sigmaPiInverseWithoOutBadSmallComponents.stream().filter(c -> c.size() > 1)
                        .findFirst().get();
                mu.add(new Cycle(initialFactor.get(0), initialFactor.get(1), initialFactor.get(2)));

                var badSmallComponent = false;
                // O(n)
                for (var i = 0; i < 8; i++) {
                    final var norm = getNorm(mu);

                    mu = extend(mu, sigmaPiInverse, pi);

                    if (norm == getNorm(mu)) {
                        badSmallComponent = true;
                        break;
                    }

                    final var _11_8Seq = searchFor11_8Seq(mu, sigmaPiInverse, pi);
                    if (_11_8Seq != null) {
                        for (final var rho : _11_8Seq)
                            pi = computeProduct(new Cycle(rho), pi).asNCycle();
                        distance += _11_8Seq.length;
                        badSmallComponent = false;
                        break;
                    }
                }

                if (badSmallComponent)
                    badSmallComponents.add(mu);
            }

            sigmaPiInverse = computeProduct(true, sigma, pi.getInverse());
        }

        // Bad small components
        final Collection<Cycle> mu = new ArrayList<>();
        final var iterator = badSmallComponents.iterator();
        while (iterator.hasNext()) {
            mu.addAll(iterator.next());
            iterator.remove();
            if (getNorm(mu) >= 16) {
                final var _11_8Seq = searchFor11_8Seq(mu, sigmaPiInverse, pi);
                for (final var rho : _11_8Seq) {
                    pi = computeProduct(new Cycle(rho), pi).asNCycle();
                }
                distance += _11_8Seq.length;
                sigmaPiInverse = computeProduct(true, sigma, pi.getInverse());
            }
        }

        while (!sigmaPiInverse.isIdentity()) {
            final var _2move = CommonOperations.searchFor2MoveFromOrientedCycle(sigmaPiInverse, pi);
            if (_2move != null) {
                pi = computeProduct(_2move, pi).asNCycle();
                distance += 1;
            } else {
                apply3_2_Unoriented(sigmaPiInverse, pi);
                distance += 3;
            }
            sigmaPiInverse = computeProduct(true, sigma, pi.getInverse());
        }

        return distance;
    }

    private byte[][] searchFor11_8Seq(final Collection<Cycle> mu, final MulticyclePermutation sigmaPiInverse, final Cycle pi) {
        return searchForSeq(mu, sigmaPiInverse, pi, _11_8UnorientedCases.get(mu.size()));
    }

    Set<Cycle> extend(final Set<Cycle> mu, final MulticyclePermutation sigmaPiInverse, final Cycle pi) {
        final var piInverse = pi.getInverse().getStartingBy(pi.getMinSymbol());
        final Set<Byte> muSymbols = new HashSet<>();

        final var symbolToMuCycles = CommonOperations.mapSymbolsToCycles(sigmaPiInverse, pi);

        final var symbolToSigmaPiInverseCycles = new Cycle[piInverse.size()];
        for (final var cycle : sigmaPiInverse)
            for (var i = 0; i < cycle.getSymbols().length; i++)
                symbolToSigmaPiInverseCycles[cycle.getSymbols()[i]] = cycle;

        // Type 1 extension
        // These two outer loops are O(1), since at this point, ||mu|| never
        // exceeds 16
        for (final var muCycle : mu) {
            for (var i = 0; i < muCycle.getSymbols().length; i++) {
                final var left = piInverse.indexOf(muCycle.get(i));
                final var right = piInverse.indexOf(muCycle.image(muCycle.get(i)));
                // O(n)
                if (CommonOperations.isOpenGate(left, right, symbolToMuCycles, mu, piInverse)) {
                    final var intersectingCycle = getIntersectingCycle(left, right, symbolToSigmaPiInverseCycles,
                            piInverse);
                    if (intersectingCycle != null
                            && !contains(muSymbols, symbolToSigmaPiInverseCycles[intersectingCycle.get(0)])) {
                        final var a = intersectingCycle.get(0);
                        final var b = intersectingCycle.image(a);
                        final var c = intersectingCycle.image(b);
                        final Set<Cycle> newMu = new HashSet<>(mu);
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
                    if (symbolToMuCycles[piInverse.get(index)] == null) {
                        final var intersectingCycle = symbolToSigmaPiInverseCycles[piInverse.get(index)];
                        if (intersectingCycle != null && intersectingCycle.size() > 1
                                && !contains(muSymbols, symbolToSigmaPiInverseCycles[intersectingCycle.get(0)])) {
                            final var a = piInverse.get(index);
                            final var b = intersectingCycle.image(a);
                            if (isOutOfInterval(piInverse.indexOf(b), left, right)) {
                                final var c = intersectingCycle.image(b);
                                final Set<Cycle> newMu = new HashSet<>(mu);
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
    byte[][] searchForSeq(final Collection<Cycle> mu, final MulticyclePermutation sigmaPiInverse, final Cycle pi,
                          final List<Case> cases) {
        if (cases != null) {
            final var symbolToMuCycle = CommonOperations.mapSymbolsToCycles(mu, pi);

            final var symbolsCount = mu.stream().mapToInt(Cycle::size).sum();

            final var _piArrayList = new ByteArrayList(symbolsCount);
            // O(n)
            for (var i = 0; i < pi.getSymbols().length; i++) {
                final var muCycle = symbolToMuCycle[pi.getSymbols()[i]];
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
                            final var muCycle = symbolToMuCycle[piSymbol];
                            if (muCycle != null && !labels.containsKey(muCycle))
                                labels.put(muCycle, labels.size() + 1);
                        }

                        for (final var entry : labels.entrySet())
                            if (entry.getKey().size() != _case.getSigmaPiInverse().get(entry.getValue() - 1).size())
                                continue rotation;

                        for (var j = 0; j < _piSymbols.length; j++) {
                            if (_case.getSignature()[j] != labels.get(symbolToMuCycle[_piSymbols[j]]))
                                continue rotation;
                            else {
                                if (j == _case.getSignature().length - 1) {
                                    final var rhos = new byte[_case.getRhos().size()][];
                                    for (var k = 0; k < rhos.length; k++) {
                                        final var _rho = Arrays.copyOf(_case.getRhos().get(k), 3);
                                        CommonOperations.replace(_rho, _piSymbols);
                                        rhos[k] = _rho;
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
