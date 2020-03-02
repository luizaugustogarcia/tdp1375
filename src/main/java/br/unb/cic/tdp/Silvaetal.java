package br.unb.cic.tdp;

import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.proof.Case;
import cern.colt.list.ByteArrayList;
import com.google.common.primitives.Bytes;
import org.apache.commons.collections.ListUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static br.unb.cic.tdp.CommonOperations.*;
import static br.unb.cic.tdp.permutation.PermutationGroups.computeProduct;
import static br.unb.cic.tdp.util.ByteArrayOperations.replace;

public class Silvaetal extends BaseAlgorithm {

    private static final String ORTD_GREATER_THAN_5 = "oriented/oriented_gt_5";
    private static final String ORTD_INTERLEAVING_PAIR = "oriented/(0,4,2)(1,5,3)";
    private static final String ORTD_BAD_SMAL_INTERLEAVING_PAIR = "oriented/(0,4,2)(1,5,3)";

    private List<Case> _11_8OrientedCycle = new ArrayList<>();

    private Map<Integer, List<Case>> _11_8OrientedCases = new HashMap<>();

    public Silvaetal(final String casesFolder) {
        super(casesFolder);

        final var cases = new ArrayList<Case>();
        try {
            Files.lines(Paths.get("C:\\Users\\USER-Admin\\workspace\\tdp1375\\proof\\" + "lemma-24.txt")).forEach(line -> {
                final var lineSplit = line.split("-");
                final var spi = new MulticyclePermutation(lineSplit[0]);
                final var pi = CANONICAL_PI[spi.getNumberOfSymbols()];
                final var rhos = Arrays.stream(lineSplit[1].split(";")).map(i -> new Cycle(i)).collect(Collectors.toList());
                cases.add(new Case(spi, pi, rhos));
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        _11_8UnorientedCases = cases.stream().collect(Collectors.groupingBy(Case::getCyclesCount));
    }

    public static void main(final String[] args) {
        final var silvaetal = new Silvaetal("");
        System.out.println(silvaetal.sort(new Cycle("0,9,8,7,6,5,4,3,2,1")));
    }

    public static List<Cycle> extend(final List<Cycle> mu, final MulticyclePermutation spi, final Cycle pi) {
        final var piInverse = pi.getInverse().getStartingBy(pi.getMinSymbol());
        final Set<Byte> muSymbols = mu.stream().flatMap(c -> Bytes.asList(c.getSymbols()).stream())
                .collect(Collectors.toSet());

        final var muCycleIndex = createCycleIndex(mu, pi);
        final var spiCycleIndex = createCycleIndex(spi, pi);

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
                        if (a < b && (a < j && j < b)) {
                            for (final var symbol : intersecting.getStartingBy(piInverse.get(j)).getSymbols()) {
                                final var nextSymbol = intersecting.image(symbol);
                                final var index = piInverse.indexOf(nextSymbol);
                                if (index > b || index < a) {
                                    final List<Cycle> newMu = new ArrayList<>(mu);
                                    newMu.add(new Cycle(symbol, nextSymbol, intersecting.image(nextSymbol)));
                                    return newMu;
                                }
                            }
                        } else if (a > b && (j > b && j < a)) {
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

        // Type 3 extension
        // The outer loop is O(1) since, at this point, ||mu|| never exceeds 16
        for (var muCycle : mu) {
            if (muCycle.size() < spiCycleIndex[muCycle.get(0)].size()) {
                final var spiCycle = align(spiCycleIndex[muCycle.get(0)], muCycle);
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
    public static Cycle align(final Cycle spiCycle, final Cycle segment) {
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

    public static Cycle[] searchForSeq(final List<Cycle> mu, final MulticyclePermutation spi, final Cycle pi,
                                       final List<Case> cases) {
        if (cases != null) {
            final var cycleIndex = createCycleIndex(mu, pi);

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
                        for /* O(1) */ (var j = 0; j < _piSymbols.length; j++) {
                            final var muCycle = cycleIndex[_piSymbols[j]];
                            if (muCycle != null && !labels.containsKey(muCycle))
                                labels.put(muCycle, labels.size() + 1);
                        }

                        for (final var entry : labels.entrySet())
                            if (entry.getKey().size() != _case.getSpi().get(entry.getValue() - 1).size()
                                    || isOriented(pi, entry.getKey()) != isOriented(_case.getPi(),
                                    _case.getSpi().get(entry.getValue() - 1)))
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

    // TODO remove
    public static Cycle[] tempSearchForSeq(final List<Cycle> mu, final MulticyclePermutation spi, final Cycle pi,
                                           final Collection<Configuration> cases) {
        if (cases != null) {
            final var cycleIndex = createCycleIndex(mu, pi);

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

            for /* O(1) */ (final var __case : cases)
                if (symbolsCount == __case.getSignature().getContent().length)
                    for (final var _case : Arrays.asList(__case, new Configuration(__case.getSpi(), __case.getPi().getInverse())))
                        rotation:
                                for /* O(1) */ (var i = 0; i < piSymbols.length; i++) {
                                    final var _piSymbols = getStartingBy(piSymbols, i);

                                    final Map<Cycle, Integer> labels = new HashMap<>();
                                    for /* O(1) */ (var j = 0; j < _piSymbols.length; j++) {
                                        final var muCycle = cycleIndex[_piSymbols[j]];
                                        if (muCycle != null && !labels.containsKey(muCycle))
                                            labels.put(muCycle, labels.size() + 1);
                                    }

                                    for (var j = 0; j < _piSymbols.length; j++) {
                                        if (_case.getSignature().getContent()[j] != labels.get(cycleIndex[_piSymbols[j]]))
                                            continue rotation;
                                        else {
                                            if (j == _case.getSignature().getContent().length - 1) {
                                                return new Cycle[1];
                                            }
                                        }
                                    }
                                }

        }

        return null;
    }

    @SuppressWarnings({"unchecked"})
    public int sort(Cycle pi) {
        final var n = pi.size();

        final var array = new byte[pi.size()];
        for (var i = 0; i < pi.size(); i++)
            array[i] = (byte) i;

        final var sigma = CANONICAL_PI[10];

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
        List<Cycle> spiWithoOutBadSmallComponents;
        while (!spi.isIdentity() && !(spiWithoOutBadSmallComponents = ListUtils.subtract(
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
                    distance += apply11_8SeqOrientedCase(orientedCycle, spi, pi);
                } else {
                    List<Cycle> mu = new ArrayList<>();
                    final var initialFactor = spiWithoOutBadSmallComponents.stream().filter(c -> c.size() > 1)
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
                final var _11_8Seq = searchFor11_8Seq(mu, spi, pi);
                for (final var rho : _11_8Seq) {
                    pi = computeProduct(rho, pi).asNCycle();
                }
                distance += _11_8Seq.length;
                spi = computeProduct(true, sigma, pi.getInverse());
            }
        }

        // O(n)
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

    private Cycle[] searchFor11_8Seq(final List<Cycle> mu, final MulticyclePermutation spi, final Cycle pi) {
        Cycle[] rhos = null;

        if (mu.stream().anyMatch(c -> c.size() == 5 && isOriented(pi, c)))
            rhos = searchForSeq(mu, spi, pi, _11_8OrientedCases.get(mu.size()));

        if (rhos == null)
            rhos = searchForSeq(mu, spi, pi, _11_8UnorientedCases.get(mu.size()));

        return rhos;
    }

    private Cycle searchForOrientedCycleBiggerThan5(final MulticyclePermutation spi, final Cycle pi) {
        return spi.stream().filter(c -> c.size() > 5 && isOriented(pi, c)).findFirst()
                .orElse(null);
    }

    // O(n^3)
    private int apply11_8SeqOrientedCase(final Cycle orientedCycle, final MulticyclePermutation spi, final Cycle pi) {
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
                        final var _orientedCycleCase = _case.getSpi().get(0);

                        final var substitution = new byte[7];
                        for (var k = 0; k < substitution.length; k++)
                            substitution[_orientedCycleCase.getSymbols()[k]] = factor.get(k);

                        final var rhos = new Cycle[_case.getRhos().size()];
                        for (var k = 0; k < rhos.length; k++) {
                            final var _rho = Arrays.copyOf(_case.getRhos().get(k).getSymbols(), 3);
                            replace(_rho, substitution);
                            rhos[k] = new Cycle(_rho);
                        }

                        if (is11_8(spi, pi, Arrays.asList(rhos))) {
                            applyMoves(pi, rhos);
                            return rhos.length;
                        }
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
            final var initialFactor = spi.stream().filter(c -> c.size() > 1).findFirst().get();
            List<Cycle> mu = new ArrayList<>();
            mu.add(new Cycle(initialFactor.get(0), initialFactor.get(1), initialFactor.get(2)));
            for (var i = 0; i < 2; i++) {
                mu = extend(mu, spi, pi);
                final var rhos = searchForSeq(mu, spi, pi, _3_2Cases);
                if (rhos != null) {
                    applyMoves(pi, rhos);
                    return;
                }
            }
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
                applyMoves(pi, rho1, rho2, rho3);
                return;
            }
        }
    }
}
