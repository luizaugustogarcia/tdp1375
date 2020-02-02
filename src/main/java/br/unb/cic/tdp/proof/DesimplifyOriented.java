package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import com.google.common.base.Throwables;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

import static br.unb.cic.tdp.CommonOperations.*;

class DesimplifyOriented extends DesimplifyUnoriented {

    static List<Case> generate(final String inputDir) {
        final var verifiedConfigurations = new HashSet<Configuration>();

        final var visitedFiles = new HashSet<String>();

        final var cases = new ArrayList<Case>();

        // unoriented interleaving pair
        generate(verifiedConfigurations, cases, inputDir + "bfs_files/", "[3](0_4_2)[3](1_5_3).html", visitedFiles);

        // --------------------
        // BAD SMALL COMPONENTS
        // --------------------

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
            final var pi = CANONICAL_PI[spi.getNumberOfSymbols()];

            desimplify(verifiedConfigurations, cases, spi, pi, rhos);
        } else {
            try (final var fr = new BufferedReader(new FileReader(baseFolder + file), 100000)) {
                try (final var scanner = new Scanner(fr)) {
                    scanner.useDelimiter("\\n");

                    while (scanner.hasNext()) {
                        final var line = scanner.next();

                        if (line.startsWith("View")) {
                            final var matcher = SPI_PATTERN.matcher(line);
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
                                   final MulticyclePermutation spi, final Cycle pi, final List<Cycle> rhos) {
        for (final var combination : combinations(spi, 2)) {
            for (final var joinPair : getJoinPairs(combination.getVector(), pi)) {
                final var join = join(spi, pi, rhos, joinPair);

                final var cr = canonicalize(join.first, join.second, join.third);
                final var _spi = cr.first;
                final var _pi = cr.second;
                final var _rhos = cr.third;

                // Skipping configuration containing cycles > 5, since all oriented 7-cycle accept (4,3)-sequence
                final var isThereOrientedCycleGreaterThan5 = _spi.stream().anyMatch(cycle -> cycle.size() > 5 && isOriented(pi, cycle));
                // Skipping configurations not containing an oriented 5-cycle
                final var isThereOriented5Cycle = _spi.stream().anyMatch(cycle -> cycle.size() == 5 && isOriented(pi, cycle));
                // Skipping configurations containing 2-moves
                final var isThereOriented3Segment = searchFor2MoveFromOrientedCycle(_spi, _pi) != null;

                if (!isThereOrientedCycleGreaterThan5 && isThereOriented5Cycle && !isThereOriented3Segment) {
                    if (is11_8(_spi, _pi, _rhos)) {
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
