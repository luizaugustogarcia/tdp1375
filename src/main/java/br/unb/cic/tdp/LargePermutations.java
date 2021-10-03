package br.unb.cic.tdp;

import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.PermutationGroups;
import org.apache.commons.lang.time.StopWatch;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static br.unb.cic.tdp.base.CommonOperations.CANONICAL_PI;

public class LargePermutations {

    public static void main(String[] args) throws URISyntaxException, IOException {
        final var algorithm = new Silvaetal();

        for (int i = 2; i <= 10; i++) {
            var resource = LargePermutations.class.getResource("/large" + i * 10 + ".txt");
            var path = Paths.get(resource.toURI());

            var stopWatch = new StopWatch();
            stopWatch.start();

            var maxRatio = Files.lines(path).mapToDouble(line -> {
                    var pi = Cycle.create(line);
                    var sigmaPiInverse = PermutationGroups.computeProduct(CANONICAL_PI[pi.size()],
                            pi.getInverse());
                    var sorting = algorithm.sort(pi);
                    return (double)sorting.size() / sigmaPiInverse.get3Norm();
                }
            ).max().getAsDouble();

            stopWatch.stop();

            System.out.println(i * 10 + " - " + stopWatch.getTime() + " - " + maxRatio);
        }
    }
}
