package br.unb.cic.tdp.proof.eh;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.commons.lang.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;

import static br.unb.cic.tdp.base.CommonOperations.CANONICAL_PI;
import static br.unb.cic.tdp.base.CommonOperations.applyTranspositionOptimized;

public class EHProofTraverser {

    private static final Pattern SPI_PATTERN = Pattern.compile(".*\"(.*)\".*");
    private static final Pattern SORTING_PATTERN = Pattern.compile(".*a = (\\d+).*b = (\\d+).*c = (\\d+).*");

    public static void traverse(final String baseFolder, final String startFile,
                                final CaseProcessor processor, final Set<Configuration> processedConfigs) {
        traverse(baseFolder, startFile, 0, processor, processedConfigs);
    }

    @SneakyThrows
    private static void traverse(final String baseFolder, final String startFile, final int depth,
                                 final CaseProcessor processor, final Set<Configuration> processedConfigs) {
        val spi = readSpi(startFile);
        val configuration = new Configuration(spi);
        val sorting = readSorting(baseFolder + startFile);

        processor.process(configuration, sorting, depth, processedConfigs.contains(configuration));

        if (processedConfigs.contains(configuration)) {
            return;
        }

        processedConfigs.add(configuration);

        if (!sorting.isEmpty()) {
            return;
        }

        try (val fr = new BufferedReader(new FileReader(baseFolder + startFile))) {
            try (val scanner = new Scanner(fr)) {
                scanner.useDelimiter("\\n");

                while (scanner.hasNext()) {
                    val line = scanner.next();

                    if (line.startsWith("View")) {
                        val matcher = SPI_PATTERN.matcher(line);
                        if (matcher.matches())
                            traverse(baseFolder, matcher.group(1), depth + 1, processor, processedConfigs);
                    }
                }
            }
        }
    }

    private static MulticyclePermutation readSpi(final String file) {
        val _file = new File(file);
        var str = StringUtils.removeEnd(_file.getName().replace("_", ","), ".html");
        str = str.replaceAll("\\[.*?\\]", "");
        str = str.replace(" ", ",");
        return new MulticyclePermutation(str);
    }

    @SneakyThrows
    private static List<Cycle> readSorting(final String file) {
        val spi = readSpi(file);
        var pi = CANONICAL_PI[spi.getNumberOfSymbols()];

        var hasSorting = false;

        val sorting = new ArrayList<Cycle>();

        try (val fr = new BufferedReader(new FileReader(file), 1024 * 1024)) {
            try (val scanner = new Scanner(fr)) {
                scanner.useDelimiter("\\n");

                while (scanner.hasNext()) {
                    val line = scanner.next();

                    if (line.contains("SORTING"))
                        hasSorting = true;

                    if (hasSorting) {
                        val matcher = SORTING_PATTERN.matcher(line);

                        if (matcher.matches()) {
                            val a = Integer.parseInt(matcher.group(1));
                            val b = Integer.parseInt(matcher.group(2));
                            val c = Integer.parseInt(matcher.group(3));

                            val rho = Cycle.create(pi.get(a), pi.get(b), pi.get(c));
                            sorting.add(rho);

                            pi = applyTranspositionOptimized(pi, rho);
                        }
                    }
                }
            }
        }

        return sorting;
    }

    interface CaseProcessor {

        void process(Configuration configuration, List<Cycle> sorting, int depth, boolean alreadyVisited);
    }
}
