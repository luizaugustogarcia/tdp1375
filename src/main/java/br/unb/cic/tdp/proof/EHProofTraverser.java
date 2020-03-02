package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import com.google.common.base.Throwables;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static br.unb.cic.tdp.CommonOperations.CANONICAL_PI;
import static br.unb.cic.tdp.CommonOperations.applyTransposition;

public abstract class EHProofTraverser {

    private static final Pattern SPI_PATTERN = Pattern.compile(".*\"(.*)\".*");
    private static final Pattern SORTING_PATTERN = Pattern.compile(".*a = (\\d+).*b = (\\d+).*c = (\\d+).*");

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

    private static MulticyclePermutation getSpi(final String file) {
        final var _file = new File(file);
        var str = StringUtils.removeEnd(_file.getName().replace("_", ","), ".html");
        str = str.replaceAll("\\[.*?\\]", "");
        str = str.replace(" ", ",");
        return new MulticyclePermutation(str);
    }

    public void traverse(final String baseFolder, final String startFile, final Set<String> visitedFiles,
                         final Set<Configuration> verifiedConfigurations, final PrintStream printer, final int depth) {
        final var spi = getSpi(startFile);
        final var configuration = new Configuration(spi, CANONICAL_PI[spi.getNumberOfSymbols()]);

        if (visitedFiles.contains(baseFolder + startFile)) {
            printer.println(StringUtils.repeat("\t", depth) + "^" +
                    configuration.hashCode() + "#" + spi.toString());
            return;
        }

        visitedFiles.add(baseFolder + startFile);

        final var rhos = getSorting(baseFolder + startFile);

        if (!rhos.isEmpty()) {
            readEHCase(configuration, rhos, printer, depth);

            desimplify(configuration, rhos, verifiedConfigurations, printer, depth + 1);
        } else {
            printCase(configuration, Collections.emptyList(), printer, depth);

            try (final var fr = new BufferedReader(new FileReader(baseFolder + startFile))) {
                try (final var scanner = new Scanner(fr)) {
                    scanner.useDelimiter("\\n");

                    while (scanner.hasNext()) {
                        final var line = scanner.next();

                        if (line.startsWith("View")) {
                            final var matcher = SPI_PATTERN.matcher(line);
                            if (matcher.matches())
                                traverse(baseFolder, matcher.group(1), visitedFiles, verifiedConfigurations,
                                        printer, depth + 1);
                        }
                    }
                }
            } catch (final Exception e) {
                Throwables.propagate(e);
            }
        }
    }

    protected void printCase(final Configuration configuration, List<Cycle> rhos, final PrintStream printer, int depth) {
        if (rhos.isEmpty()) {
            printer.println(StringUtils.repeat("\t", depth) +
                    configuration.hashCode() + "#" + configuration.getSpi().toString());
        } else {
            printer.println(StringUtils.repeat("\t", depth) +
                    configuration.hashCode() + "#" + configuration.getSpi().toString() + "->" + rhos.stream().map(Cycle::toString)
                    .collect(Collectors.joining(";")));
        }
    }

    protected abstract void desimplify(final Configuration configuration, final List<Cycle> rhos,
                                       final Set<Configuration> verifiedConfigurations, final PrintStream printer,
                                       final int depth);

    protected void readEHCase(final Configuration configuration, final List<Cycle> rhos, final PrintStream printer,
                              final int depth) {
        // by default, does nothing
    }
}
