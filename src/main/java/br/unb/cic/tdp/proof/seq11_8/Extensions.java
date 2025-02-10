package br.unb.cic.tdp.proof.seq11_8;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.util.Pair;
import com.google.common.base.Throwables;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static br.unb.cic.tdp.base.CommonOperations.is11_8;
import static br.unb.cic.tdp.proof.ProofGenerator.permutationToJsArray;
import static br.unb.cic.tdp.proof.seq11_8.SortOrExtendExtensions.*;

public class Extensions {

    @SneakyThrows
    public static void generate(final String outputDir) {
        val dfsDir = outputDir + "/dfs/";

        Files.createDirectories(Paths.get(dfsDir));
        Files.createDirectories(Paths.get(dfsDir + "/working/"));
        Files.createDirectories(Paths.get(dfsDir + "/bad-cases/"));

        val storage = new DefaultProofStorage(dfsDir);
        val root = Configuration.ofSignature(new float[]{1F});

        final Predicate<Configuration> shouldStop = configuration -> Boolean.FALSE;
        final Predicate<Configuration> isValidExtension = configuration -> {
            val isFull = configuration.isFull();

            if (isFull && configuration.getSpi().times(configuration.getPi()).size() > 1) {
                System.out.println("invalid full configuration -> " + configuration.getSpi());
                return false;
            }
            return true;
        };

        val pool = new ForkJoinPool();
        // oriented 5-cycle
        pool.execute(new SortOrExtendExtensions(root, new Configuration(new MulticyclePermutation("(0,3,1,4,2)")), shouldStop, isValidExtension, storage));
        // interleaving pair
        pool.execute(new SortOrExtendExtensions(root, new Configuration(new MulticyclePermutation("(0,4,2)(1,5,3)")), shouldStop, isValidExtension, storage));
        // intersecting pair
        pool.execute(new SortOrExtendExtensions(root, new Configuration(new MulticyclePermutation("(0,3,1)(2,5,4)")), shouldStop, isValidExtension, storage));
        pool.shutdown();
        // boundless
        pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);

        val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        Files.list(Paths.get(outputDir + "/dfs/bad-cases/"))
                .map(Path::toFile)
                .forEach(file -> executor.submit(
                        () -> makeHtmlNavigation(new Configuration(new MulticyclePermutation(file.getName())), outputDir)));

        executor.shutdown();
        pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }

    @SneakyThrows
    public static void cleanUpBadExtensionAndInvalidFiles(final String outputDir) {
        val dir = new File(outputDir);

        val files = new ArrayList<File>();
        Stream.of(dir.listFiles(file -> file.getName().endsWith(".html")))
                .parallel()
                .forEach(f -> {
                    val file = new File(outputDir + f.getName());

                    try {
                        if (isBadExtension(file)) {
                            files.add(file);
                        } else {
                            val canonical = new Configuration(new MulticyclePermutation(f.getName().replace(" ", ",")));
                            val sorting = getSorting(file.toPath());
                            if (!is11_8(canonical.getSpi(), canonical.getPi(), sorting.getSecond())) {
                                files.add(file);
                            }
                        }
                    } catch (IllegalStateException e) {
                        files.add(file);
                    }
                });

        boolean canContinue = true;
        for (val file : files) {
            boolean deleted = FileUtils.deleteQuietly(file);
            canContinue &= deleted;
            if (!deleted) {
                System.out.println("rm \"" + file + "\"");
            }
        }

        if (!canContinue)
            throw new RuntimeException("ERROR: files not deleted, cannot continue.");
    }

    @SneakyThrows
    public static Pair<MulticyclePermutation, List<Cycle>> getSorting(final Path path) {
        val reader = new BufferedReader(new FileReader(path.toFile()), 1024 * 10);
        var line = reader.readLine();
        MulticyclePermutation spi = null;

        while ((line = reader.readLine()) != null) {
            line = line.trim();

            if (line.startsWith("<h6>")) {
                spi = new MulticyclePermutation(line.trim().replace("<h6>", "")
                        .replace("</h6>", "").replace(" ", ","));
            }

            if (line.equals("THE EXTENSIONS ARE:")) {
                return new Pair<>(spi, null);
            }

            val sorting = new ArrayList<Cycle>();
            if (line.trim().equals("ALLOWS (11/8)-SEQUENCE")) {
                while ((line = reader.readLine()) != null) {
                    line = line.trim();

                    if (!line.equals("<div style=\"margin-top: 10px; \">")) {
                        continue;
                    }

                    line = reader.readLine();

                    val move = line.split(": ")[1].replace(" ", ",")
                            .replace("<br>", "");
                    sorting.add(Cycle.of(move));
                }
                return new Pair<>(spi, sorting);
            }
        }

        throw new IllegalStateException("Unknown file " + path.toFile());
    }

    public static boolean isBadExtension(final File file) {
        final BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(file), 1024 * 10);
            var line = reader.readLine();

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.equals("THE EXTENSIONS ARE:")) {
                    return true;
                }

                if (line.trim().equals("ALLOWS (11/8)-SEQUENCE")) {
                    return false;
                }
            }
        } catch (Throwable e) {
            Throwables.propagate(e);
        }

        throw new IllegalStateException("Unknown file");
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
    public static void cleanUpIncompleteCases(final String outputDir) {
        val excludeFiles = new ArrayList<File>();

        Files.list(Paths.get(outputDir + "/working/"))
                .map(Path::toFile)
                .forEach(excludeFiles::add);

        boolean canContinue = true;
        for (val file : excludeFiles) {
            boolean deleted = FileUtils.deleteQuietly(file);
            canContinue &= deleted;
            if (!deleted) {
                System.out.println("rm \"" + file + "\"");
            }
        }

        if (!canContinue)
            throw new RuntimeException("ERROR: working files not deleted, cannot continue.");
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