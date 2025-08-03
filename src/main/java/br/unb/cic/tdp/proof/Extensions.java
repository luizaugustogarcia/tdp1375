package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import com.google.common.primitives.Ints;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ForkJoinPool;

import static br.unb.cic.tdp.base.PivotedConfiguration.of;
import static java.util.stream.Collectors.joining;

@Slf4j
public class Extensions {

    public static void generate(final String outputDir, final double minRate) {
        val storage = new DerbyProofStorage(outputDir, "");

        val parallelism = Integer.parseInt(System.getProperty("java.util.concurrent.ForkJoinPool.common.parallelism",
                "%d".formatted(Runtime.getRuntime().availableProcessors())));
        try (val pool = new ForkJoinPool(parallelism)) {
            pool.execute(new SortOrExtend(of("(0)", 0), of("(0)", 0), storage, minRate));
        }
    }

    public static String permutationToJsArray(final MulticyclePermutation permutation) {
        return "[%s]".formatted(permutation
                .stream().map(c -> "[%s]".formatted(Ints.asList(c.getSymbols()).stream()
                        .map(s -> Integer.toString(s))
                        .collect(joining(","))))
                .collect(joining(",")));
    }
}