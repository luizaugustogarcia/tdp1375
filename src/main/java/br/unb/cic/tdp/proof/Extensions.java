package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.util.Pair;
import lombok.SneakyThrows;
import lombok.val;

import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import static br.unb.cic.tdp.proof.ProofGenerator.permutationToJsArray;
import static br.unb.cic.tdp.proof.SortOrExtend.*;

public class Extensions {

    @SneakyThrows
    public static void generate(final String outputDir, final double minRate) {
        val dfsDir = outputDir + "/dfs/";

        Files.createDirectories(Paths.get(dfsDir));
        Files.createDirectories(Paths.get(dfsDir + "/working/"));
        Files.createDirectories(Paths.get(dfsDir + "/bad-cases/"));

        val storage = new DefaultProofStorage(dfsDir);
        val root = Configuration.ofSignature(new float[]{1F});

        val pool = new ForkJoinPool(Integer.parseInt(System.getProperty("java.util.concurrent.ForkJoinPool.common.parallelism",
                Runtime.getRuntime().availableProcessors() + "")));
        pool.execute(new SortOrExtend(root, new Configuration("(0 2 1)"), storage, minRate));
        pool.shutdown();
        // boundless
        pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);

        val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        Files.list(Paths.get(outputDir + "/dfs/bad-cases/"))
                .map(Path::toFile)
                .forEach(file -> executor.submit(
                        () -> makeHtmlNavigation(new Configuration(new MulticyclePermutation(file.getName())), outputDir)));

        executor.shutdown();
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }

    @SneakyThrows
    private static void renderExtensions(final List<Pair<String, Configuration>> extensions, final PrintStream out,
                                         final String outputDir) {
        for (val extension : extensions) {
            val configuration = extension.getSecond();
            val canonical = extension.getSecond().getCanonical();

            val badCaseFile = new File(outputDir + "/dfs/bad-cases/" + canonical.getSpi());
            val hasSorting = !badCaseFile.exists();
            out.println(hasSorting ? "<div style=\"margin-top: 10px; background-color: rgba(153, 255, 153, 0.15)\">" :
                    "<div style=\"margin-top: 10px; background-color: rgba(255, 0, 0, 0.05);\">");
            out.println(extension.getFirst() + "<br>");
            out.println(((hasSorting ? "GOOD" : "BAD") + " EXTENSION") + "<br>");
            out.println("Hash code: " + configuration.hashCode() + "<br>");
            out.println("3-norm: " + configuration.getSpi().get3Norm() + "<br>");
            out.println("Signature: " + configuration.getSignature() + "<br>");
            val jsSpi = permutationToJsArray(configuration.getSpi());
            out.printf("Extension: <a href=\"\" " +
                            "onclick=\"" +
                            "updateCanvas('modalCanvas', %s); " +
                            "$('h6.modal-title').text('%s');" +
                            "$('#modal').modal('show'); " +
                            "return false;\">%s</a><br>%n",
                    jsSpi, configuration.getSpi(), configuration.getSpi());
            out.printf("View canonical extension: <a href=\"%s.html\">%s</a>%n", canonical.getSpi(), canonical.getSpi());
            out.println("</div>");
        }
    }

    @SneakyThrows
    private static void makeHtmlNavigation(final Configuration configuration, final String outputDir) {
        try (val out = new PrintStream(outputDir + "/dfs/" + configuration.getSpi() + ".html")) {
            out.println("<html>\n" +
                    "\t<head>\n" +
                    "\t\t<link rel=\"stylesheet\" href=\"https://stackpath.bootstrapcdn.com/bootstrap/4.4.1/css/bootstrap.min.css\" integrity=\"sha384-Vkoo8x4CGsO3+Hhxv8T/Q5PaXtkKtu6ug5TOeNV6gBiFeWPGFN9MuhOf23Q9Ifjh\" crossorigin=\"anonymous\">\n" +
                    "\t\t<script src=\"https://code.jquery.com/jquery-3.4.1.slim.min.js\" integrity=\"sha384-J6qa4849blE2+poT4WnyKhv5vZF5SrPo0iEjwBvKU7imGFAV0wwj1yYfoRSJoZ+n\" crossorigin=\"anonymous\"></script>\n" +
                    "\t\t<script src=\"https://cdn.jsdelivr.net/npm/popper.js@1.16.0/dist/umd/popper.min.js\" integrity=\"sha384-Q6E9RHvbIyZFJoft+2mJbHaEWldlvI9IOYy5n3zV9zzTtmI3UksdQRVvoxMfooAo\" crossorigin=\"anonymous\"></script>\n" +
                    "\t\t<script src=\"https://stackpath.bootstrapcdn.com/bootstrap/4.4.1/js/bootstrap.min.js\" integrity=\"sha384-wfSDF2E50Y2D1uUdj0O3uMBJnjuUD4Ih7YwaYd1iqfktj0Uod8GCExl3Og8ifwB6\" crossorigin=\"anonymous\"></script>\n" +
                    "\t\t<script src=\"../draw-config.js\"></script>\n" +
                    "\t\t<style>* { font-size: small; }</style>\n" +
                    "\t</head>\n" +
                    "<body>\n" +
                    "<div class=\"modal fade\" id=\"modal\" role=\"dialog\">\n" +
                    "    <div class=\"modal-dialog\" style=\"left: 25px; max-width: unset;\">\n" +
                    "      <!-- Modal content-->\n" +
                    "      <div class=\"modal-content\" style=\"width: fit-content;\">\n" +
                    "        <div class=\"modal-header\">\n" +
                    "          <h6 class=\"modal-title\">--------</h6>\n" +
                    "          <button type=\"button\" class=\"close\" data-dismiss=\"modal\">&times;</button>\n" +
                    "        </div>\n" +
                    "        <div class=\"modal-body\">\n" +
                    "          <canvas id=\"modalCanvas\"></canvas>\n" +
                    "        </div>\n" +
                    "      </div>\n" +
                    "    </div>\n" +
                    "</div>\n" +
                    "<script>\n" +
                    "\tfunction updateCanvas(canvasId, spi) {\n" +
                    "\t   var pi = []; for (var i = 0; i < spi.flatMap(c => c).length; i++) { pi.push(i); }" +
                    "\t   var canvas = document.getElementById(canvasId);\n" +
                    "\t   canvas.height = calcHeight(canvas, spi, pi);\n" +
                    "\t   canvas.width = pi.length * padding;\n" +
                    "\t   draw(canvas, spi, pi);\n" +
                    "\t}\n" +
                    "</script>\n" +
                    "<div style=\"margin-top: 10px; margin-left: 10px\">");

            out.println("<canvas id=\"canvas\"></canvas>");
            out.printf("<script>updateCanvas('canvas', %s);</script>%n",
                    permutationToJsArray(configuration.getSpi()));

            out.println("<h6>" + configuration.getSpi() + "</h6>");

            out.println("Hash code: " + configuration.hashCode() + "<br>");
            out.println("Open gates: " + configuration.getOpenGates() + "<br>");
            out.println("Signature: " + configuration.getSignature() + "<br>");
            out.println("3-norm: " + configuration.getSpi().get3Norm());

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
            renderExtensions(type1Extensions(configuration), out, outputDir);
            out.println("    </td>");
            out.println("    <td style=\"vertical-align: baseline; border: 1px solid lightgray;\">");
            renderExtensions(type2Extensions(configuration), out, outputDir);
            out.println("    </td>");
            out.println("    <td style=\"vertical-align: baseline; border: 1px solid lightgray;\">");
            renderExtensions(type3Extensions(configuration), out, outputDir);
            out.println("    </td>");
            out.println("  </tr>");
            out.println("</table>");

            out.println("</body>");
            out.println("</html>");
        }
    }
}