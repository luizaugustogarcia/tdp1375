package br.unb.cic.tdp.experiments;

import br.unb.cic.tdp.EliasAndHartman;
import br.unb.cic.tdp.Silvaetal;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.PermutationGroups;
import org.apache.commons.lang.time.StopWatch;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static br.unb.cic.tdp.base.CommonOperations.CANONICAL_PI;

public class LargePermutations {

    public static void main(String[] args) throws URISyntaxException, IOException {
        final var silva = new Silvaetal();
        final var eh = new EliasAndHartman();

        for (int i = 2; i <= 50; i++) {
            var out = new FileOutputStream("C:\\Users\\Luiz Silva\\Projects\\tdp1375\\src\\main\\resources\\datasets\\stats" + i * 10 + ".txt");
            var ps = new PrintStream(out);

            var resource = LargePermutations.class.getResource("/datasets/large" + i * 10 + ".txt");
            var path = Paths.get(resource.toURI());

            Files.lines(path).forEach (line -> {
                var pi = Cycle.create(line);
                var sigmaPiInverse = PermutationGroups.computeProduct(CANONICAL_PI[pi.size()],
                        pi.getInverse());

                ps.print(sigmaPiInverse.get3Norm());
                ps.print(",");

                var sw = new StopWatch();
                sw.start();
                var sorting = silva.sort(pi);
                sw.stop();

                ps.print(sorting.size());
                ps.print(",");

                ps.print(sw.getTime());
                ps.print(",");

                sw = new StopWatch();
                sw.start();
                sorting = eh.sort(pi);
                sw.stop();

                ps.print(sorting.size());
                ps.print(",");

                ps.println(sw.getTime());

                ps.flush();
            });
        }
    }
}
