package br.unb.cic.tdp.eh;

import br.unb.cic.tdp.CommonOperations;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.proof.Configuration;
import com.google.common.base.Throwables;
import org.apache.commons.lang.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

public class TraverseEHProof {

    private static final Pattern SPI_PATTERN = Pattern.compile(".*\"(.*)\".*");
    private static final Pattern SORTING_PATTERN = Pattern.compile(".*a = (\\d+).*b = (\\d+).*c = (\\d+).*");
    private static final Cycle[] CANONICAL_PI;

    static {
        CANONICAL_PI = new Cycle[50];
        for (var i = 0; i < 50; i++) {
            final var pi = new byte[i + 1];
            for (var j = 0; j <= i; j++) {
                pi[j] = (byte) j;
            }
            CANONICAL_PI[i] = new Cycle(pi);
        }
    }

    public static void main(String[] args) {
        visit("C:\\Users\\USER-Admin\\Temp\\sbt1375_proof.tar\\sbt1375_proof\\bfs_files\\", "[3](0_3_1)[3](2_5_4).html", new HashSet<String>(), 0);
    }

    private static void visit(final String baseFolder, final String file, final Set<String> visitedFiles, int depth) {
        if (visitedFiles.contains(baseFolder + file))
            return;

        final var spi = getSpi(file);
        final var n = spi.stream().mapToInt(Cycle::size).sum();
        final var pi = new byte[n];
        for (var i = 0; i < n; i++) {
            pi[i] = (byte) i;
        }

        final var rhos = getSorting(baseFolder + file);

        final var configuration = new Configuration(new Cycle(pi), spi);

        if (!rhos.isEmpty()) {
            // TODO consume verifiedConfigurations, cases, spi, pi, rhos
        } else {
            try (final var fr = new BufferedReader(new FileReader(baseFolder + file), 100000)) {
                try (final var scanner = new Scanner(fr)) {
                    scanner.useDelimiter("\\n");

                    while (scanner.hasNext()) {
                        final var line = scanner.next();

                        if (line.startsWith("View")) {
                            final var matcher = SPI_PATTERN.matcher(line);
                            if (matcher.matches())
                                visit(baseFolder, matcher.group(1), visitedFiles, depth + 1);
                        }
                    }
                }
            } catch (final Exception e) {
                Throwables.propagate(e);
            }
        }

        visitedFiles.add(baseFolder + file);
    }

    private static List<Cycle> getSorting(final String file) {
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

                            pi = CommonOperations.applyTransposition(pi, rho);
                        }
                    }
                }
            }
        } catch (final IOException e) {
            Throwables.propagate(e);
        }

        return rhos;
    }

    private static MulticyclePermutation getSpi(final String file) {
        final var _file = new File(file);
        var str = StringUtils.removeEnd(_file.getName().replace("_", ","), ".html");
        str = str.replaceAll("\\[.*?\\]", "");
        str = str.replace(" ", ",");
        return new MulticyclePermutation(str);
    }
}
