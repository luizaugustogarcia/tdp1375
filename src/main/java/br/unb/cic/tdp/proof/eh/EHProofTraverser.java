package br.unb.cic.tdp.proof.eh;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import com.google.common.base.Throwables;
import org.apache.commons.lang.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;

import static br.unb.cic.tdp.base.CommonOperations.CANONICAL_PI;
import static br.unb.cic.tdp.base.CommonOperations.applyTransposition;

public class EHProofTraverser {

    private static final Pattern SPI_PATTERN = Pattern.compile(".*\"(.*)\".*");
    private static final Pattern SORTING_PATTERN = Pattern.compile(".*a = (\\d+).*b = (\\d+).*c = (\\d+).*");

    public static void traverse(final String baseFolder, final String startFile,
                                final CaseProcessor processor, final Set<Configuration> processedConfigs) {
        traverse(baseFolder, startFile, 0, processor, processedConfigs);
    }

    private static void traverse(final String baseFolder, final String startFile, final int depth,
                                 final CaseProcessor processor, final Set<Configuration> processedConfigs) {
        final var spi = readSpi(startFile);
        final var configuration = new Configuration(spi);
        final var sorting = readSorting(baseFolder + startFile);

        processor.process(configuration, sorting, depth, processedConfigs.contains(configuration));

        if (processedConfigs.contains(configuration)) {
            return;
        }

        processedConfigs.add(configuration);

        if (!sorting.isEmpty()) {
            return;
        }

        try (final var fr = new BufferedReader(new FileReader(baseFolder + startFile))) {
            try (final var scanner = new Scanner(fr)) {
                scanner.useDelimiter("\\n");

                while (scanner.hasNext()) {
                    final var line = scanner.next();

                    if (line.startsWith("View")) {
                        final var matcher = SPI_PATTERN.matcher(line);
                        if (matcher.matches())
                            traverse(baseFolder, matcher.group(1), depth + 1, processor, processedConfigs);
                    }
                }
            }
        } catch (final Exception e) {
            Throwables.propagate(e);
        }
    }

    private static MulticyclePermutation readSpi(final String file) {
        final var _file = new File(file);
        var str = StringUtils.removeEnd(_file.getName().replace("_", ","), ".html");
        str = str.replaceAll("\\[.*?\\]", "");
        str = str.replace(" ", ",");
        return new MulticyclePermutation(str);
    }

    private static List<Cycle> readSorting(final String file) {
        final var spi = readSpi(file);
        var pi = CANONICAL_PI[spi.getNumberOfSymbols()];

        var hasSorting = false;

        final var sorting = new ArrayList<Cycle>();

        try (final var fr = new BufferedReader(new FileReader(file), 1024 * 1024)) {
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
                            sorting.add(rho);

                            pi = applyTransposition(pi, rho);
                        }
                    }
                }
            }
        } catch (final IOException e) {
            Throwables.propagate(e);
        }

        return sorting;
    }

    interface CaseProcessor {

        void process(Configuration configuration, List<Cycle> sorting, int depth, boolean alreadyVisited);
    }
}
