package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import com.google.common.base.Throwables;
import org.apache.commons.lang.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

import static br.unb.cic.tdp.CommonOperations.*;

class DesimplifyUnoriented {

    protected static final Pattern SPI_PATTERN = Pattern.compile(".*\"(.*)\".*");
    private static final Pattern SORTING_PATTERN = Pattern.compile(".*a = (\\d+).*b = (\\d+).*c = (\\d+).*");

    static List<Case> generate(final String inputDir) {
        final var verifiedConfigurations = new HashSet<Configuration>();

        final var visitedFiles = new HashSet<String>();

        final var cases = new ArrayList<Case>();

        // unoriented interleaving pair
        generate(verifiedConfigurations, cases, inputDir + "bfs_files/", "[3](0_4_2)[3](1_5_3).html", visitedFiles);

        // unoriented intersecting pair
        generate(verifiedConfigurations, cases, inputDir + "bfs_files/", "[3](0_3_1)[3](2_5_4).html", visitedFiles);

        // --------------------
        // BAD SMALL COMPONENTS
        // --------------------

        // the unoriented interleaving pair
        generate(verifiedConfigurations, cases, inputDir + "comb_files/", "[3](0_4_2)[3](1_5_3).html", visitedFiles);
        // the unoriented necklaces of size 4
        generate(verifiedConfigurations, cases, inputDir + "comb_files/", "[3](0_10_2)[3](1_5_3)[3](4_8_6)[3](7_11_9).html",
                visitedFiles);
        // the twisted necklace of size 4
        generate(verifiedConfigurations, cases, inputDir + "comb_files/", "[3](0_7_5)[3](1_11_9)[3](2_6_4)[3](3_10_8).html",
                visitedFiles);
        // the unoriented necklaces of size 5
        generate(verifiedConfigurations, cases, inputDir + "comb_files/",
                "[3](0_4_2)[3](1_14_12)[3](3_7_5)[3](6_10_8)[3](9_13_11).html", visitedFiles);
        // the unoriented necklaces of size 6
        generate(verifiedConfigurations, cases, inputDir + "comb_files/",
                "[3](0_16_2)[3](1_5_3)[3](4_8_6)[3](7_11_9)[3](10_14_12)[3](13_17_15).html", visitedFiles);

        return cases;
    }

    protected static List<Cycle> getSorting(final String file) {
        final var mu = getSpi(file);
        var pi = CANONICAL_PI[mu.getNumberOfSymbols()];

        var hasSorting = false;

        final var rhos = new ArrayList<Cycle>();

        try (final var fr = new BufferedReader(new FileReader(file), 1024)) {
            try (final var scanner = new Scanner(fr)) {
                scanner.useDelimiter("\\n");

                while (scanner.hasNext()) {
                    final var line = scanner.next();

                    if (line.contains("SORTING"))
                        hasSorting = true;

                    if (hasSorting) {
                        final var matcher = SORTING_PATTERN.matcher(line);

                        if (matcher.matches()) {
                            final var a = Byte.parseByte(matcher.group(1));
                            final var b = Byte.parseByte(matcher.group(2));
                            final var c = Byte.parseByte(matcher.group(3));

                            final var rho = new Cycle(pi.get(a), pi.get(b), pi.get(c));
                            rhos.add(rho);

                            pi = applyTransposition(pi, rho);
                        }
                    }
                }
            }
        } catch (final IOException e) {
            Throwables.propagate(e);
        }

        return rhos;
    }

    protected static MulticyclePermutation getSpi(final String file) {
        final var _file = new File(file);
        var str = StringUtils.removeEnd(_file.getName().replace("_", ","), ".html");
        str = str.replaceAll("\\[.*?\\]", "");
        str = str.replace(" ", ",");
        return new MulticyclePermutation(str);
    }

    private static void generate(final Set<Configuration> verifiedConfigurations, final List<Case> cases,
                                 final String baseFolder, final String file, final Set<String> visitedFiles) {
        if (visitedFiles.contains(baseFolder + file))
            return;

        final var spi = getSpi(file);
        final var pi = CANONICAL_PI[spi.getNumberOfSymbols()];

        final var rhos = getSorting(baseFolder + file);

        if (!rhos.isEmpty()) {
            if (is11_8(spi, pi, rhos)) {
                cases.add(new Case(pi, spi, rhos));
            } else {
                throw new RuntimeException("ERROR");
            }

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
            // only join cycles which are not intersecting
            if (areNotIntersecting(combination.getVector(), pi)) {
                final var joiningPairs = getJoinPairs(combination.getVector(), pi);

                for (final var joinPair : joiningPairs) {
                    final var join = join(spi, pi, rhos, joinPair);

                    final var cr = canonicalize(join.first, join.second, join.third);
                    final var _spi = cr.first;
                    final var _pi = cr.second;
                    final var _rhos = cr.third;

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
