package br.unb.cic.tdp.experiments;

import br.unb.cic.tdp.BaseAlgorithm;
import br.unb.cic.tdp.EliasAndHartman;
import br.unb.cic.tdp.Silvaetal;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.PermutationGroups;
import com.google.common.base.Stopwatch;
import lombok.SneakyThrows;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static br.unb.cic.tdp.base.CommonOperations.CANONICAL_PI;

public class LongerPermutations {
    private static BaseAlgorithm silva = new Silvaetal();
    private static BaseAlgorithm eh = new EliasAndHartman();

    @SneakyThrows
    public static void main(String[] args) throws URISyntaxException, IOException {
        for (int i = 2; i <= 50; i++) {
             run(args[0], i);
        }
    }

    @SneakyThrows
    private static void run(final String root, final int i) {
        System.out.println("stats" + i * 10 + ".txt");
        final var out = new FileOutputStream( root + "stats" + i * 10 + ".txt");
        final var ps = new PrintStream(out);

        final var resource = LongerPermutations.class.getResource("/datasets/large" + i * 10 + ".txt");
        final var path = Paths.get(resource.toURI());

        Files.lines(path).forEach (line -> {
            var pi = Cycle.create(line);
            var sigmaPiInverse = PermutationGroups.computeProduct(CANONICAL_PI[pi.size()],
                    pi.getInverse());

            ps.print(sigmaPiInverse.get3Norm());
            ps.print(",");

            var sw = Stopwatch.createStarted();
            final var silvaSorting = silva.sort(pi);
            sw.stop();

            ps.print(silvaSorting.getSecond().size());
            ps.print(",");

            ps.print(sw.elapsed(TimeUnit.MILLISECONDS));
            ps.print(",");

            sw = Stopwatch.createStarted();
            final var ehSorting = eh.sort(pi);
            sw.stop();

            ps.print(ehSorting.getSecond().size());
            ps.print(",");

            ps.println(sw.elapsed(TimeUnit.MILLISECONDS));
        });
    }
}
