package br.unb.cic.tdp;

import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.permutation.PermutationGroups;
import br.unb.cic.tdp.proof.Case;
import br.unb.cic.tdp.proof.OddCycles;
import com.google.common.base.Throwables;
import org.apache.commons.math3.util.Pair;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static br.unb.cic.tdp.CommonOperations.applyTransposition;
import static br.unb.cic.tdp.CommonOperations.isOriented;
import static br.unb.cic.tdp.permutation.PermutationGroups.computeProduct;

public abstract class BaseAlgorithm {

    protected static final String UNRTD_3_2 = "unoriented/3_2";
    protected static final String UNRTD_INTERSECTING_PAIR = "unoriented/(0,3,1)(2,5,4)";
    protected static final String UNRTD_INTERLEAVING_PAIR = "unoriented/(0,4,2)(1,5,3)";
    protected static final String UNRTD_BAD_SMALL_INTERLEAVING_PAIR = "unoriented/bad-small-(0,4,2)(1,5,3)";
    protected static final String UNRTD_BAD_SMALL_NECKLACE_SIZE_4 = "unoriented/bad-small-(0,10,2)(1,5,3)(4,8,6)(7,11,9)";
    protected static final String UNRTD_BAD_SMALL_TWISTED_NECKLACE_SIZE_4 = "unoriented/bad-small-(0,7,5)(1,11,9)(2,6,4)(3,10,8)";
    protected static final String UNRTD_BAD_SMALL_NECKLACE_SIZE_5 = "unoriented/bad-small-(0,4,2)(1,14,12)(3,7,5)(6,10,8)(9,13,11)";
    protected static final String UNRTD_BAD_SMALL_NECKLACE_SIZE_6 = "unoriented/bad-small-(0,16,2)(1,5,3)(4,8,6)(7,11,9)(10,14,12)(13,17,15)";

    protected Map<Integer, List<Case>> _11_8UnorientedCases = new HashMap<>();
    protected List<Case> _1_1OddCyclesCases = new ArrayList<>();
    protected List<Case> _2_2OddCyclesCases = new ArrayList<>();
    protected List<Case> _3_2Cases = new ArrayList<>();

    public BaseAlgorithm(final String casesFolder) {
        //loadCases(casesFolder);
    }

    abstract int sort(Cycle pi);

    protected void loadCases(final String casesFolder) {
        // Generates the 2-moves to be applied when we have one odd cycle in sigma
        // pi^{-1}
        _1_1OddCyclesCases.addAll(OddCycles.generate());

        // Generates the (2,2)-sequences to be applied when we have four odd cycles in
        // sigma pi^{-1}
        _2_2OddCyclesCases.addAll(OddCycles.generate2_2Cases());

        // Loads the (3,2)-sequences to be applied to the interleaving pair and to
        // the cases where three 3-cycles are intersecting
        _3_2Cases = loadCasesFromFile(String.format("%s/%s", casesFolder, UNRTD_3_2));

        final List<Case> cases = new ArrayList<>();
        addCases(casesFolder, cases, UNRTD_INTERSECTING_PAIR, UNRTD_INTERLEAVING_PAIR,
                UNRTD_BAD_SMALL_INTERLEAVING_PAIR, UNRTD_BAD_SMALL_NECKLACE_SIZE_4,
                UNRTD_BAD_SMALL_TWISTED_NECKLACE_SIZE_4, UNRTD_BAD_SMALL_NECKLACE_SIZE_5,
                UNRTD_BAD_SMALL_NECKLACE_SIZE_6);

        _11_8UnorientedCases.putAll(cases.stream().collect(Collectors.groupingBy(Case::getCyclesCount)));
    }

    protected void addCases(final String casesFolder, final List<Case> cases, final String... caseFiles) {
        for (final var caseFile : caseFiles) {
            cases.addAll(loadCasesFromFile(String.format("%s/%s", casesFolder, caseFile)));
        }
    }

    public static int getNorm(final Collection<Cycle> mu) {
        return mu.stream().mapToInt(Cycle::getNorm).sum();
    }

    protected byte[] getStartingBy(final byte[] cycle, final int i) {
        final var _symbols = new byte[cycle.length];
        System.arraycopy(cycle, i, _symbols, 0, _symbols.length - i);
        System.arraycopy(cycle, 0, _symbols, _symbols.length - i, i);
        return _symbols;
    }

    public static boolean isOutOfInterval(final int x, final int left, final int right) {
        if (left < right)
            return x < left || x > right;
        return false;
    }

    // O(n)
    public static Cycle getIntersectingCycle(final int left, final int right, final Cycle[] cycleIndex,
                                         final Cycle piInverse) {
        final var gates = left < right ? right - left : piInverse.size() - (left - right);
        for (var i = 1; i < gates; i++) {
            final var index = (i + left) % piInverse.size();
            final var intersectingCycle = cycleIndex[piInverse.get(index)];
            if (intersectingCycle != null && intersectingCycle.size() > 1) {
                final var a = piInverse.get(index);
                final var b = intersectingCycle.image(a);
                if (isOutOfInterval(piInverse.indexOf(b), left, right)) {
                    return intersectingCycle.getStartingBy(a);
                }
            }
        }
        return null;
    }

    protected void apply2MoveTwoOddCycles(final MulticyclePermutation spi, final Cycle pi) {
        final var evenCycles = spi.getNumberOfEvenCycles();
        final var oddCycles = spi.stream().filter(c -> !c.isEven()).collect(Collectors.toList());
        for (final var c1 : oddCycles)
            for (final var c2 : oddCycles)
                if (c1 != c2) {
                    for (final var a : CommonOperations.getSegmentsOfLength2(c1))
                        for (final var b : CommonOperations.getSegmentsOfLength2(c2)) {
                            for (final var rho : CommonOperations
                                    .combinations(Arrays.asList(a.get(0), a.get(1), b.get(0), b.get(1)), 3)) {
                                final var rho1 = new Cycle(rho.getVector().stream().mapToInt(i -> i).toArray());
                                if (pi.isApplicable(rho1)
                                        && (computeProduct(spi, rho1.getInverse())).getNumberOfEvenCycles()
                                        - evenCycles == 2) {
                                    applyMoves(pi, rho1);
                                }
                            }
                        }
                }
    }

    protected boolean thereAreOddCycles(final MulticyclePermutation spi) {
        return spi.stream().anyMatch(c -> !c.isEven());
    }

    public static boolean contains(final Set<Byte> muSymbols, final Cycle cycle) {
        for (final Byte symbol : cycle.getSymbols())
            if (muSymbols.contains(symbol))
                return true;
        return false;
    }

    protected Pair<Cycle, Cycle> searchFor2_2Seq(final MulticyclePermutation spi, final Cycle pi) {
        final var oddCycles = spi.stream().filter(c -> !c.isEven()).collect(Collectors.toList());

        for /* O(n) */ (final var c1 : oddCycles)
            for /* O(n) */ (final var c2 : oddCycles)
                if (c1 != c2) {
                    for /* O(n) */ (final var a : CommonOperations.getSegmentsOfLength2(c1))
                        for (final Byte b : c2.getSymbols()) {
                            for (final var rho : CommonOperations
                                    .combinations(Arrays.asList(a.get(0), a.get(1), b), 3)) {
                                final var rho1 = new Cycle(rho.getVector().stream().mapToInt(i -> i).toArray());
                                if (pi.isApplicable(rho1)) {
                                    final var _spi = PermutationGroups
                                            .computeProduct(spi, rho1.getInverse());
                                    final var _pi = applyTransposition(pi, rho1);
                                    final var rho2 = CommonOperations.searchFor2Move(_spi, _pi);
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
                        if (pi.isOrientedTriple(a, b, c)) {
                            var after = cycle.getK(a, b) % 2 == 1 ? 1 : 0;
                            after += cycle.getK(b, c) % 2 == 1 ? 1 : 0;
                            after += cycle.getK(c, a) % 2 == 1 ? 1 : 0;
                            if (after - before == 2) {
                                final var rho1 = new Cycle(a, b, c);
                                final var _spi = PermutationGroups.computeProduct(spi,
                                        rho1.getInverse());
                                final var _pi = applyTransposition(pi, rho1);
                                final var rho2 = CommonOperations.searchFor2Move(_spi, _pi);
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

    protected void applyMoves(final Cycle pi, final Cycle... rhos) {
        var _pi = pi;
        for (final var rho : rhos) {
            _pi = CommonOperations.applyTransposition(_pi, rho);
        }
        pi.redefine(_pi.getSymbols());
    }

    protected List<Case> loadCasesFromFile(final String file) {
        final List<Case> _cases = new ArrayList<>();

        try (final var fr = new BufferedReader(new FileReader(file))) {
            String line;

            while ((line = fr.readLine()) != null) {
                final var parts = line.trim().split(";");
                final var spi = new MulticyclePermutation(parts[0]);
                final var pi = new byte[spi.stream().mapToInt(c -> c.getSymbols().length).sum()];
                for (var i = 0; i < pi.length; i++) {
                    pi[i] = (byte) i;
                }
                final var rhos = new ArrayList<Cycle>();
                for (var _rho : parts[1].split("-")) {
                    _rho = _rho.replaceAll("\\[|\\]|\\s", "");
                    final var rho = new byte[3];
                    final var symbols = _rho.split(",");
                    rho[0] = Byte.parseByte(symbols[0]);
                    rho[1] = Byte.parseByte(symbols[1]);
                    rho[2] = Byte.parseByte(symbols[2]);
                    rhos.add(new Cycle(rho));
                }
                _cases.add(new Case(spi, new Cycle(pi), rhos));
            }
        } catch (final IOException e) {
            Throwables.propagate(e);
        }

        return _cases;
    }

    protected void apply3_2_Unoriented(final MulticyclePermutation spi, final Cycle pi) {
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

    static List<Cycle> extend(List<Cycle> mu, MulticyclePermutation spi, Cycle pi) {
        return null;
    }

    abstract Cycle[] searchForSeq(List<Cycle> mu, MulticyclePermutation spi, Cycle pi, List<Case> cases);
}
