package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.permutation.MulticyclePermutation;
import com.google.common.primitives.Ints;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.sql.SQLException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

import static br.unb.cic.tdp.base.PivotedConfiguration.of;

@Slf4j
public class Extensions {

    public static void generate(final String outputDir, final double minRate) throws SQLException {
        System.setProperty("derby.system.durability", "test");

        val storage = new DerbyProofStorage(outputDir, "extensions");

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
                        .collect(Collectors.joining(","))))
                .collect(Collectors.joining(",")));
    }
}