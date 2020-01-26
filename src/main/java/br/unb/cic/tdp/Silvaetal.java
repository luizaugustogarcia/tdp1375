package br.unb.cic.tdp;

import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.proof.Case;
import cern.colt.list.ByteArrayList;
import org.apache.commons.collections.ListUtils;

import java.util.*;
import java.util.stream.Collectors;

import static br.unb.cic.tdp.permutation.PermutationGroups.computeProduct;

public class Silvaetal extends BaseAlgorithm {

    private static final String ORTD_GREATER_THAN_5 = "oriented/oriented_gt_5";
    private static final String ORTD_INTERLEAVING_PAIR = "oriented/(0,4,2)(1,5,3)";
    private static final String ORTD_BAD_SMAL_INTERLEAVING_PAIR = "oriented/(0,4,2)(1,5,3)";

    private List<Case> _11_8OrientedCycle = new ArrayList<>();

    private Map<Integer, List<Case>> _11_8OrientedCases = new HashMap<>();

    public Silvaetal(final String casesFolder) {
        super(casesFolder);
        loadExtraCases(casesFolder);
    }

    public static void main(final String[] args) {
        final var silvaetal = new Silvaetal(args[0]);
        System.out.println(silvaetal.sort(new Cycle(args[1])));
    }

    private void loadExtraCases(final String casesFolder) {
        final var cases = new ArrayList<Case>();

        addCases(casesFolder, cases, ORTD_INTERLEAVING_PAIR, ORTD_BAD_SMAL_INTERLEAVING_PAIR);

        _11_8OrientedCases.putAll(cases.stream().collect(Collectors.groupingBy(Case::getCyclesCount)));

        // Generates the (4,3)-sequences cases to be applied when we have an oriented
        // cycle with length greater than 6
        _11_8OrientedCycle.addAll(loadCasesFromFile(String.format("%s/%s", casesFolder, ORTD_GREATER_THAN_5)));
    }

    @SuppressWarnings({"unchecked"})
    public int sort(Cycle pi) {
        final var n = pi.size();

        final var array = new byte[pi.size()];
        for (var i = 0; i < pi.size(); i++)
            array[i] = (byte) i;

        final var sigma = new Cycle(array);

        var sigmaPiInverse = computeProduct(true, n, sigma, pi.getInverse());

        var distance = 0;

        // O(n^6)
        final var _2_2Seq = searchFor2_2Seq(sigmaPiInverse, pi);
        if (_2_2Seq != null) {
            pi = computeProduct(_2_2Seq.getSecond(), _2_2Seq.getFirst(), pi).asNCycle();
            distance += 2;
            sigmaPiInverse = computeProduct(true, sigma, pi.getInverse());
        }

        // O(n^2)
        while (thereAreOddCycles(sigmaPiInverse)) {
            apply2MoveTwoOddCycles(sigmaPiInverse, pi);
            distance += 1;
            sigmaPiInverse = computeProduct(true, sigma, pi.getInverse());
        }

        final List<Collection<Cycle>> badSmallComponents = new ArrayList<>();

        // O(n^4)
        List<Cycle> sigmaPiInverseWithoOutBadSmallComponents;
        while (!sigmaPiInverse.isIdentity() && !(sigmaPiInverseWithoOutBadSmallComponents = ListUtils.subtract(
                sigmaPiInverse.stream().filter(c -> c.size() > 1).collect(Collectors.toList()),
                badSmallComponents.stream().flatMap(Collection::stream).collect(Collectors.toList()))).isEmpty()) {
            // O(n^3)
            final var _2move = CommonOperations.searchFor2MoveFromOrientedCycle(sigmaPiInverse, pi);
            if (_2move != null) {
                pi = computeProduct(_2move, pi).asNCycle();
                distance += 1;
            } else {
                final var orientedCycle = searchForOrientedCycleBiggerThan5(sigmaPiInverse, pi);
                if (orientedCycle != null) {
                    // O(n^3)
                    distance += apply11_8SeqOrientedCase(orientedCycle, sigmaPiInverse, pi);
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
                            break;
                        }
                    }

                    if (badSmallComponent)
                        badSmallComponents.add(mu);
                }
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

        // O(n)
        while (!sigmaPiInverse.isIdentity()) {
            final var _2move = CommonOperations.searchFor2MoveFromOrientedCycle(sigmaPiInverse, pi);
            if (_2move != null) {
                pi = computeProduct(_2move, pi).asNCycle();
                distance += 1;
            } else {
                apply3_2(sigmaPiInverse, pi);
                distance += 3;
            }
            sigmaPiInverse = computeProduct(true, sigma, pi.getInverse());
        }

        return distance;
    }

    private byte[][] searchFor11_8Seq(final Collection<Cycle> mu, final MulticyclePermutation sigmaPiInverse, final Cycle pi) {
        byte[][] rhos = null;

        if (mu.stream().anyMatch(c -> c.size() == 5 && !pi.getInverse().areSymbolsInCyclicOrder(c.getSymbols())))
            rhos = searchForSeq(mu, sigmaPiInverse, pi, _11_8OrientedCases.get(mu.size()));

        if (rhos == null)
            rhos = searchForSeq(mu, sigmaPiInverse, pi, _11_8UnorientedCases.get(mu.size()));

        return rhos;
    }

    Set<Cycle> extend(final Set<Cycle> mu, final MulticyclePermutation sigmaPiInverse, final Cycle pi) {
        final var piInverse = pi.getInverse().getStartingBy(pi.getMinSymbol());
        final Set<Byte> muSymbols = new HashSet<>();

        final var symbolToMuCycles = CommonOperations.mapSymbolsToCycles(mu, pi);

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

        // Type 3 extension
        // The outer loop is O(1) since, at this point, ||mu|| never exceeds 16
        for (var muCycle : mu) {
            if (muCycle.size() < symbolToSigmaPiInverseCycles[muCycle.get(0)].size()) {
                final var sigmaPiInverseCycle = align(symbolToSigmaPiInverseCycles[muCycle.get(0)], muCycle);
                muCycle = muCycle.getStartingBy(sigmaPiInverseCycle.get(0));
                final var newSymbols = Arrays.copyOf(muCycle.getSymbols(), muCycle.getSymbols().length + 2);
                newSymbols[muCycle.getSymbols().length] = sigmaPiInverseCycle
                        .image(muCycle.get(muCycle.getSymbols().length - 1));
                newSymbols[muCycle.getSymbols().length + 1] = sigmaPiInverseCycle
                        .image(newSymbols[muCycle.getSymbols().length]);

                final Set<Cycle> newMu = new HashSet<>(mu);
                newMu.remove(muCycle);
                newMu.add(new Cycle(newSymbols));

                final var openGates = CommonOperations.openGatesPerCycle(newMu, piInverse);
                if (openGates.values().stream().mapToInt(j -> j).sum() <= 2)
                    return newMu;
            }
        }

        return mu;
    }

    // O(n)
    private Cycle align(final Cycle sigmaPiInverseCycle, final Cycle segment) {
        for (var i = 0; i < segment.size(); i++) {
            var symbol = segment.get(i);
            final var index = sigmaPiInverseCycle.indexOf(symbol);
            var match = true;
            for (var j = 1; j < segment.size(); j++) {
                if (segment.get((i + j) % segment.size()) != sigmaPiInverseCycle
                        .get((index + j) % sigmaPiInverseCycle.size())) {
                    match = false;
                    break;
                }
                symbol = segment.image(symbol);
            }
            if (match)
                return sigmaPiInverseCycle.getStartingBy(segment.get(i));
        }
        return null;
    }

    private Cycle searchForOrientedCycleBiggerThan5(final MulticyclePermutation sigmaPiInverse, final Cycle pi) {
        return sigmaPiInverse.stream().filter(c -> c.size() > 5 && !pi.getInverse().areSymbolsInCyclicOrder(c.getSymbols())).findFirst()
                .orElse(null);
    }

    // O(n^3)
    private int apply11_8SeqOrientedCase(final Cycle orientedCycle, final MulticyclePermutation sigmaPiInverse, final Cycle pi) {
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

                    final var factor = new Cycle(a, d, e, b, f, c, g);

                    for (final var _case : _11_8OrientedCycle) {
                        final var _orientedCycleCase = _case.getSigmaPiInverse().get(0);

                        final var substitution = new byte[7];
                        for (var k = 0; k < substitution.length; k++)
                            substitution[_orientedCycleCase.getSymbols()[k]] = factor.get(k);

                        final var rhos = new byte[_case.getRhos().size()][];
                        for (var k = 0; k < rhos.length; k++) {
                            final var _rho = Arrays.copyOf(_case.getRhos().get(k), 3);
                            CommonOperations.replace(_rho, substitution);
                            rhos[k] = _rho;
                        }

                        if (CommonOperations.is11_8(sigmaPiInverse, pi.getSymbols(), Arrays.asList(rhos))) {
                            applyMoves(pi, rhos);
                            return rhos.length;
                        }
                    }
                }
            }
        }

        throw new RuntimeException("ERROR");
    }

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
                        for /* O(1) */ (var j = 0; j < _piSymbols.length; j++) {
                            final var muCycle = symbolToMuCycle[_piSymbols[j]];
                            if (muCycle != null && !labels.containsKey(muCycle))
                                labels.put(muCycle, labels.size() + 1);
                        }

                        for (final var entry : labels.entrySet())
                            if (entry.getKey().size() != _case.getSigmaPiInverse().get(entry.getValue() - 1).size()
                                    || isOriented(pi, entry.getKey()) != isOriented(_case.getPi(),
                                    _case.getSigmaPiInverse().get(entry.getValue() - 1)))
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

    private boolean isOriented(final Cycle pi, final Cycle cycle) {
        return !pi.getInverse().areSymbolsInCyclicOrder(cycle.getSymbols());
    }

    private void apply3_2(final MulticyclePermutation sigmaPiInverse, final Cycle pi) {
        final var orientedCycle = sigmaPiInverse.stream().filter(c -> c.size() == 5 && !pi.getInverse().areSymbolsInCyclicOrder(c.getSymbols()))
                .findFirst().orElse(null);

        if (orientedCycle != null) {
            apply3_2OrientedCase(orientedCycle, sigmaPiInverse, pi);
        } else {
            final var initialFactor = sigmaPiInverse.stream().filter(c -> c.size() > 1).findFirst().get();
            Set<Cycle> mu = new HashSet<>();
            mu.add(new Cycle(initialFactor.get(0), initialFactor.get(1), initialFactor.get(2)));
            for (var i = 0; i < 2; i++) {
                mu = extend(mu, sigmaPiInverse, pi);
                final var rhos = searchForSeq(mu, sigmaPiInverse, pi, _3_2Cases);
                if (rhos != null) {
                    applyMoves(pi, rhos);
                    return;
                }
            }
        }
    }

    private void apply3_2OrientedCase(final Cycle orientedCycle, final MulticyclePermutation sigmaPiInverse, final Cycle pi) {
        final var even = sigmaPiInverse.getNumberOfEvenCycles();

        for (var i = 0; i <= orientedCycle.size() - 5; i++) {
            final var _0 = orientedCycle.get(0);
            final var _3 = orientedCycle.image(_0);
            final var _1 = orientedCycle.image(_3);

            final var _4 = orientedCycle.get(i + 3);
            final var _2 = orientedCycle.image(_4);

            final var rho1 = new byte[]{_0, _1, _2};
            final var rho2 = new byte[]{_1, _2, _3};
            final var rho3 = new byte[]{_2, _3, _4};

            if (computeProduct(true, sigmaPiInverse, new Cycle(rho1).getInverse(), new Cycle(rho2).getInverse(),
                    new Cycle(rho3).getInverse()).getNumberOfEvenCycles() - even >= 4) {
                applyMoves(pi, rho1, rho2, rho3);
                return;
            }
        }
    }
}
