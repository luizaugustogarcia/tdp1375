package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.Configuration;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;

import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

import static br.unb.cic.tdp.base.CommonOperations.pivots;
import static br.unb.cic.tdp.proof.ProofGenerator.permutationToJsArray;
import static br.unb.cic.tdp.proof.SortOrExtend.*;

@Slf4j
public class Extensions {

    public static void generate(final String outputDir, final double minRate) {
        val storage = new DerbyProofStorage(outputDir, "extensions");

        int parallelism = Integer.parseInt(System.getProperty("java.util.concurrent.ForkJoinPool.common.parallelism",
                "%d".formatted(Runtime.getRuntime().availableProcessors())));
        try (val pool = new ForkJoinPool(parallelism)) {
            pool.execute(new SortOrExtend(configurationPair("(0)", 0), configurationPair("(0 2 1)", 2), storage, minRate));
        }

        try (val pool = new ForkJoinPool(parallelism)) {
            // TODO render bad cases
            // TODO render sorting cases
            pool.submit(() -> makeHtmlNavigation(new Configuration("(0 2 1)"), outputDir, storage));
        }
    }

    private static void makeHtmlNavigation(final Configuration configuration, final String outputDir, final ProofStorage storage) {
        try (val out = new PrintStream("%s/search/%s%s.html".formatted(outputDir, configuration.getSpi(), pivots(configuration)))) {
            out.println("""
                    <html>
                    \t<head>
                    \t\t<link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.4.1/css/bootstrap.min.css" integrity="sha384-Vkoo8x4CGsO3+Hhxv8T/Q5PaXtkKtu6ug5TOeNV6gBiFeWPGFN9MuhOf23Q9Ifjh" crossorigin="anonymous">
                    \t\t<script src="https://code.jquery.com/jquery-3.4.1.slim.min.js" integrity="sha384-J6qa4849blE2+poT4WnyKhv5vZF5SrPo0iEjwBvKU7imGFAV0wwj1yYfoRSJoZ+n" crossorigin="anonymous"></script>
                    \t\t<script src="https://cdn.jsdelivr.net/npm/popper.js@1.16.0/dist/umd/popper.min.js" integrity="sha384-Q6E9RHvbIyZFJoft+2mJbHaEWldlvI9IOYy5n3zV9zzTtmI3UksdQRVvoxMfooAo" crossorigin="anonymous"></script>
                    \t\t<script src="https://stackpath.bootstrapcdn.com/bootstrap/4.4.1/js/bootstrap.min.js" integrity="sha384-wfSDF2E50Y2D1uUdj0O3uMBJnjuUD4Ih7YwaYd1iqfktj0Uod8GCExl3Og8ifwB6" crossorigin="anonymous"></script>
                    \t\t<script src="../draw-config.js"></script>
                    \t\t<style>* { font-size: small; }</style>
                    \t</head>
                    <body>
                    <div class="modal fade" id="modal" role="dialog">
                        <div class="modal-dialog" style="left: 25px; max-width: unset;">
                          <!-- Modal content-->
                          <div class="modal-content" style="width: fit-content;">
                            <div class="modal-header">
                              <h6 class="modal-title">--------</h6>
                              <button type="button" class="close" data-dismiss="modal">&times;</button>
                            </div>
                            <div class="modal-body">
                              <canvas id="modalCanvas"></canvas>
                            </div>
                          </div>
                        </div>
                    </div>
                    <script>
                    \tfunction updateCanvas(canvasId, spi) {
                    \t   var pi = []; for (var i = 0; i < spi.flatMap(c => c).length; i++) { pi.push(i); }\
                    \t   var canvas = document.getElementById(canvasId);
                    \t   canvas.height = calcHeight(canvas, spi, pi);
                    \t   canvas.width = pi.length * padding;
                    \t   draw(canvas, spi, pi);
                    \t}
                    </script>
                    <div style="margin-top: 10px; margin-left: 10px">""");

            out.println("<canvas id=\"canvas\"></canvas>");
            out.printf("<script>updateCanvas('canvas', %s);</script>%n",
                    permutationToJsArray(configuration.getSpi()));

            out.printf("<h6>%s</h6>%n", configuration.getSpi());

            out.printf("Pivots: %s<br>%n", pivots(configuration));

            out.println("<p style=\"margin-top: 10px;\"></p>");
            out.println("THE EXTENSIONS ARE:");

            out.println("<table style=\"width:100%; border: 1px solid lightgray; border-collapse: collapse;\">");
            out.println("  <tr>");
            out.println("    <th style=\"text-align: start; border: 1px solid lightgray;\">Type 1</th>");
            out.println("    <th style=\"text-align: start; border: 1px solid lightgray;\">Type 2</th>");
            out.println("    <th style=\"text-align: start; border: 1px solid lightgray;\">Type 3</th>");
            out.println("  </tr>");
            out.println("  <tr>");
            out.println("    <td style=\"vertical-align: baseline; border: 1px solid lightgray;\">");
            renderExtensions(type1Extensions(configuration), out, storage);
            out.println("    </td>");
            out.println("    <td style=\"vertical-align: baseline; border: 1px solid lightgray;\">");
            renderExtensions(type2Extensions(configuration), out, storage);
            out.println("    </td>");
            out.println("    <td style=\"vertical-align: baseline; border: 1px solid lightgray;\">");
            renderExtensions(type3Extensions(configuration), out, storage);
            out.println("    </td>");
            out.println("  </tr>");
            out.println("</table>");

            out.println("</body>");
            out.println("</html>");
        } catch (final Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static void renderExtensions(
            final List<Pair<String, Configuration>> extensions,
            final PrintStream out,
            final ProofStorage storage
    ) {
        for (val extension : extensions) {
            val configuration = extension.getRight();
            val pivots = pivots(configuration);

            val canonical = getCanonical(Pair.of(configuration, pivots));

            val hasSorting = storage.findSorting(canonical);
            out.println(hasSorting.isPresent() ? "<div style=\"margin-top: 10px; background-color: rgba(153, 255, 153, 0.15)\">" :
                    "<div style=\"margin-top: 10px; background-color: rgba(255, 0, 0, 0.05);\">");
            out.printf("%s<br>%n", extension.getLeft());
            out.printf("%s<br>%n", "%s EXTENSION".formatted(hasSorting.isPresent() ? "GOOD" : "BAD"));
            out.printf("Pivots: %s<br>%n", pivots);
            val jsSpi = permutationToJsArray(configuration.getSpi());
            val label = configuration.getSpi().toString() + pivots;
            out.printf("Extension: <a href=\"\" " +
                            "onclick=\"" +
                            "updateCanvas('modalCanvas', %s); " +
                            "$('h6.modal-title').text('%s');" +
                            "$('#modal').modal('show'); " +
                            "return false;\">%s</a><br>%n",
                    jsSpi, label, configuration.getSpi().toString());
            out.printf("View canonical extension: <a href=\"%s.html\">%s</a>%n", "%s%s".formatted(canonical.getLeft().getSpi(), canonical.getRight()), canonical.getLeft().getSpi());
            out.println("</div>");
        }
    }
}