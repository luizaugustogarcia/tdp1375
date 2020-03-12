package br.unb.cic.tdp.proof.eh.desimplification;

import br.unb.cic.tdp.base.UnorientedConfiguration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.util.Triplet;
import com.google.common.base.Throwables;
import org.apache.commons.lang.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static br.unb.cic.tdp.base.CommonOperations.CANONICAL_PI;
import static br.unb.cic.tdp.base.CommonOperations.applyTransposition;

public class EHProofTraverser {

    private static final Pattern SPI_PATTERN = Pattern.compile(".*\"(.*)\".*");
    private static final Pattern SORTING_PATTERN = Pattern.compile(".*a = (\\d+).*b = (\\d+).*c = (\\d+).*");

    public static void traverse(final String baseFolder, final String startFile,
                                final Consumer<Triplet<UnorientedConfiguration, List<Cycle>, Integer>> consumer) {
        traverse(baseFolder, startFile, 0, consumer);
    }

    private static void traverse(final String baseFolder, final String startFile, final int depth,
                                 final Consumer<Triplet<UnorientedConfiguration, List<Cycle>, Integer>> consumer) {
        final var spi = readSpi(startFile);
        final var configuration = new UnorientedConfiguration(spi, CANONICAL_PI[spi.getNumberOfSymbols()]);
        final var sorting = readSorting(baseFolder + startFile);

        consumer.accept(new Triplet<>(configuration, sorting, depth));

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
                            traverse(baseFolder, matcher.group(1), depth + 1, consumer);
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

        try (final var fr = new BufferedReader(new FileReader(file))) {
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
}
