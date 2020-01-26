package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.CommonOperations;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import com.google.common.base.Throwables;
import org.apache.commons.lang.ArrayUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

class DesimplifyOriented extends DesimplifyUnoriented {

    static List<Case> generate(final String inputDir) {
        final var verifiedConfigurations = new HashSet<Configuration>();

        final var visitedFiles = new HashSet<String>();

        final var cases = new ArrayList<Case>();

        // unoriented interleaving pair
        generate(verifiedConfigurations, cases, inputDir + "bfs_files/", "[3](0_4_2)[3](1_5_3).html", visitedFiles);

        // BAD SMALL COMPONENTS
        // the unoriented interleaving pair
        generate(verifiedConfigurations, cases, inputDir + "comb_files/", "[3](0_4_2)[3](1_5_3).html", visitedFiles);

        return cases;
    }

    private static void generate(final Set<Configuration> verifiedConfigurations, final List<Case> cases,
                                 final String baseFolder, final String file, final Set<String> visitedFiles) {
        if (visitedFiles.contains(baseFolder + file))
            return;

        final var rhos = getSorting(baseFolder + file);

        if (!rhos.isEmpty()) {
            final var spi = getSpi(file);
            final var n = spi.stream().mapToInt(Cycle::size).sum();
            final var pi = new byte[n];
            for (var i = 0; i < n; i++) {
                pi[i] = (byte) i;
            }

            desimplify(verifiedConfigurations, cases, spi, pi, rhos);
        } else {
            try (final var fr = new BufferedReader(new FileReader(baseFolder + file), 100000)) {
                try (final var scanner = new Scanner(fr)) {
                    scanner.useDelimiter("\\n");

                    while (scanner.hasNext()) {
                        final var line = scanner.next();

                        if (line.startsWith("View")) {
                            final var matcher = spiPattern.matcher(line);
                            if (matcher.matches())
                                generate(verifiedConfigurations, cases, baseFolder, matcher.group(1), visitedFiles);
                        }
                    }
                }
            } catch (final Exception e) {
                Throwables.propagate(e);
            }
        }

        visitedFiles.add(baseFolder + file);
    }

    private static void desimplify(final Set<Configuration> verifiedConfigurations, final List<Case> cases,
                                   final MulticyclePermutation spi, final byte[] pi, final List<byte[]> rhos) {
        final var _piInverse = Arrays.copyOf(pi, pi.length);
        ArrayUtils.reverse(_piInverse);
        final var piInverse = new Cycle(_piInverse);

        for (final var combination : CommonOperations.combinations(spi, 2)) {
            for (final var joinPair : getJoinPairs(combination.getVector(), pi)) {
                final var join = join(joinPair, spi, pi, rhos);

                final var cr = CommonOperations.canonicalize(join.getValue0(), join.getValue1(), join.getValue2());
                final var _pi = cr.getValue1();
                final var _spi = cr.getValue0();
                final var _rhos = cr.getValue2();

                // Skipping configuration containing cycles > 5, since all oriented 7-cycle accept (4,3)-sequence
                final var isThereOrientedCycleGreaterThan5 = _spi.stream().anyMatch(c -> !piInverse.areSymbolsInCyclicOrder(c.getSymbols()) && c.size() > 5);
                // Skipping configurations not containing an oriented 5-cycle
                final var isThereOriented5Cycle = _spi.stream().anyMatch(c -> c.size() == 5 && !piInverse.areSymbolsInCyclicOrder(c.getSymbols()));
                // Skipping configurations containing 2-moves
                final var isThereOriented3Segment = CommonOperations.searchFor2MoveFromOrientedCycle(_spi, new Cycle(_pi)) != null;

                if (!isThereOrientedCycleGreaterThan5 && isThereOriented5Cycle && !isThereOriented3Segment) {
                    if (CommonOperations.is11_8(_spi, _pi, _rhos)) {
                        final var configuration = new Configuration(_pi, _spi);

                        if (!verifiedConfigurations.contains(configuration)) {
                            verifiedConfigurations.add(configuration);
                            cases.add(new Case(_pi, _spi, _rhos));
                            desimplify(verifiedConfigurations, cases, _spi, _pi, _rhos);
                        }
                    } else {
                        throw new RuntimeException("ERROR");
                    }
                }
            }
        }
    }
}
