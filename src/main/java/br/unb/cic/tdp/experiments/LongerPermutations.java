package br.unb.cic.tdp.experiments;

import br.unb.cic.tdp.AbstractSbtAlgorithm;
import br.unb.cic.tdp.EliasAndHartman;
import br.unb.cic.tdp.Silvaetal;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.PermutationGroups;
import com.google.common.base.Stopwatch;
import lombok.SneakyThrows;
import lombok.val;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static br.unb.cic.tdp.base.CommonOperations.CANONICAL_PI;

public class LongerPermutations {
    private static final AbstractSbtAlgorithm silva = new Silvaetal();
    private static final AbstractSbtAlgorithm eh = new EliasAndHartman();

    @SneakyThrows
    public static void main(String[] args) {
        for (var i = 2; i <= 50; i++) {
             run(args[0], i);
        }
    }

    @SneakyThrows
    private static void run(final String root, final int i) {
        System.out.println("stats" + i * 10 + ".txt");
        val out = new FileOutputStream( root + "/stats/stats" + i * 10 + ".txt");
        val ps = new PrintStream(out);

        val resource = LongerPermutations.class.getResource("/datasets/large" + i * 10 + ".txt");
        val path = Paths.get(resource.toURI());

        Files.lines(path).forEach (line -> {
            var pi = Cycle.of(line);
            var sigmaPiInverse = PermutationGroups.computeProduct(CANONICAL_PI[pi.size()],
                    pi.getInverse());

            ps.print(sigmaPiInverse.get3Norm());
            ps.print(",");

            var sw = Stopwatch.createStarted();
            val silvaSorting = silva.sort(pi);
            sw.stop();

            ps.print(silvaSorting.getSecond().size());
            ps.print(",");

            ps.print(sw.elapsed(TimeUnit.MILLISECONDS));
            ps.print(",");

            sw = Stopwatch.createStarted();
            val ehSorting = eh.sort(pi);
            sw.stop();

            ps.print(ehSorting.getSecond().size());
            ps.print(",");

            ps.println(sw.elapsed(TimeUnit.MILLISECONDS));
        });
    }
}
