package br.unb.cic.tdp.proof.seq11_8;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.util.Pair;
import cern.colt.list.FloatArrayList;
import com.google.common.base.Preconditions;
import lombok.SneakyThrows;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static br.unb.cic.tdp.proof.ProofGenerator.*;
import java.util.concurrent.*;

import static br.unb.cic.tdp.base.CommonOperations.cycleIndex;
import static br.unb.cic.tdp.base.Configuration.*;

public class Extensions {

    private static ExecutorService EXECUTOR = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    @SneakyThrows
    public static void generate(final String outputDir) {
        Files.createDirectories(Paths.get(outputDir + "/dfs/"));

        // oriented 5-cycle (extensions not leading to new oriented cycles)
        sortOrExtend(new Configuration(new MulticyclePermutation("(0,3,1,4,2)")), outputDir);

        // interleaving pair
        sortOrExtend(new Configuration(new MulticyclePermutation("(0,4,2)(1,5,3)")), outputDir);

        // intersecting pair
        sortOrExtend(new Configuration(new MulticyclePermutation("(0,3,1)(2,5,4)")), outputDir);

        EXECUTOR.shutdown();
    }

    @SneakyThrows
    private static void sortOrExtend(final Configuration config, final String outputDir) {
        final var canonical = config.getCanonical();

        final var file = new File(outputDir + "/dfs/" + canonical.getSpi() + ".html");

        if (file.exists()) {
            if (isFileComplete(file)) {
                return;
            } else {
                try (final var write = new PrintWriter(file)) {
                    System.out.println("Regenerating file: " + file);
                }
            }
        }

        try (final var out = new PrintStream(file)) {
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
            out.println(String.format("<script>updateCanvas('canvas', %s);</script>",
                    permutationToJsArray(canonical.getSpi())));

            out.println("<h6>" + canonical.getSpi() + "</h6>");

            out.println("Hash code: " + canonical.hashCode() + "<br>");
            out.println("Open gates: " + canonical.getOpenGates() + "<br>");
            out.println("Signature: " + canonical.getSignature() + "<br>");
            out.println("3-norm: " + canonical.getSpi().get3Norm());

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
            renderExtensions(canonical, type1Extensions(canonical), out, outputDir);
            out.println("    </td>");
            out.println("    <td style=\"vertical-align: baseline; border: 1px solid lightgray;\">");
            renderExtensions(canonical, type2Extensions(canonical), out, outputDir);
            out.println("    </td>");
            out.println("    <td style=\"vertical-align: baseline; border: 1px solid lightgray;\">");
            renderExtensions(canonical, type3Extensions(canonical), out, outputDir);
            out.println("    </td>");
            out.println("  </tr>");
            out.println("</table>");

            out.println("</body>");
            out.println("</html>");
        }
    }

    @SneakyThrows
    private static void renderExtensions(final Configuration extended,
                                         final List<Pair<String, Configuration>> extensions,
                                         final PrintStream out, final String outputDir) {
        Thread.currentThread().setName("main-" + extended.getSpi().toString());

        final var futureExtensions = new ArrayList<Future<Pair<Pair<String, Configuration>, Boolean>>>();

        for (final var extension : extensions) {
            futureExtensions.add(EXECUTOR.submit(() -> {
                final var canonical = extension.getSecond().getCanonical();

                final var file = new File(outputDir + "/dfs/" + canonical.getSpi() + ".html");

                boolean badExtension = false;
                if (file.exists() && isFileComplete(file)) {
                    final var sorting = getSorting(file.toPath());
                    if (sorting != null && sorting.getSecond() != null) {
                        return new Pair<>(extension, !sorting.getSecond().isEmpty());
                    } else {
                        // null sorting means a bad extension, or some weird file
                        badExtension = true;
                    }
                }

                if (!badExtension) {
                    final var pair = searchForSorting(canonical);

                    if (pair.isPresent()) {
                        try (final var writer = new FileWriter(file)) {
                            renderSorting(canonical, pair.get(), writer);
                        }
                    }

                    return new Pair<>(extension, pair.isPresent());
                }

                return new Pair<>(extension, Boolean.FALSE);
            }));
        }

        for (final var future : futureExtensions) {
            final var extension = future.get().getFirst();
            final var hasSorting = future.get().getSecond();
            out.println(hasSorting ? "<div style=\"margin-top: 10px; background-color: rgba(153, 255, 153, 0.15)\">" :
                    "<div style=\"margin-top: 10px; background-color: rgba(255, 0, 0, 0.05);\">");
            out.println(extension.getFirst() + "<br>");
            out.println(((hasSorting ? "GOOD" : "BAD") + " EXTENSION") + "<br>");
            out.println("Hash code: " + extension.getSecond().hashCode() + "<br>");
            out.println("3-norm: " + extension.getSecond().getSpi().get3Norm() + "<br>");
            out.println("Signature: " + extension.getSecond().getSignature() + "<br>");
            final var jsSpi = permutationToJsArray(extension.getSecond().getSpi());
            out.println(String.format("Extension: <a href=\"\" " +
                            "onclick=\"" +
                            "updateCanvas('modalCanvas', %s); " +
                            "$('h6.modal-title').text('%s');" +
                            "$('#modal').modal('show'); " +
                            "return false;\">%s</a><br>",
                    jsSpi, extension.getSecond().getSpi(), extension.getSecond().getSpi()));
            out.println(String.format("View canonical extension: <a href=\"%s.html\">%s</a>",
                    extension.getSecond().getCanonical().getSpi(), extension.getSecond().getCanonical().getSpi()));
            out.println("</div>");

            if (!hasSorting) {
                sortOrExtend(extension.getSecond(), outputDir);
            }
        }
    }

    @SneakyThrows
    private static boolean isFileComplete(final File file) {
        boolean complete = false;

        try (final var fr = new FileReader(file)) {
            try (final var input = new BufferedReader(fr,1024)) {
                String last = null, line;
                // checks whether the file generation was finished
                while ((line = input.readLine()) != null) {
                    last = line;
                }
                if (last != null && last.equals("</html>")) {
                    complete = true;
                }
            }
        }

        return complete;
    }

    /*
     * Type 1 extension.
     */
    private static List<Pair<String, Configuration>> type1Extensions(final Configuration config) {
        final var result = new ArrayList<Pair<String, Configuration>>();

        final var newCycleLabel = config.getSpi().size() + 1;

        final var signature = signature(config.getSpi(), config.getPi());

        for (int i = 0; i < signature.length; i++) {
            if (config.getOpenGates().contains(i)) {
                final var a = i;
                for (int b = 0; b < signature.length; b++) {
                    for (int c = b; c < signature.length; c++) {
                        if (!(a == b && b == c)) {
                            result.add(new Pair<>(String.format("a=%d b=%d c=%d", a, b, c),
                                    ofSignature(unorientedExtension(signature, newCycleLabel, a, b, c).elements())));
                        }
                    }
                }
            }
        }

        return result;
    }

    /*
     * Type 2 extension.
     */
    private static List<Pair<String, Configuration>> type2Extensions(final Configuration config) {
        if (!config.isFull()) {
            return Collections.emptyList();
        }

        final var result = new ArrayList<Pair<String, Configuration>>();

        final var newCycleLabel = (int) (config.getSpi().size() + 1);

        final var signature = signature(config.getSpi(), config.getPi());

        for (int a = 0; a < signature.length; a++) {
            for (int b = a; b < signature.length; b++) {
                for (int c = b; c < signature.length; c++) {
                    if (!(a == b && b == c)) {
                        result.add(new Pair<>(String.format("a=%d b=%d c=%d", a, b, c),
                                ofSignature(unorientedExtension(signature, newCycleLabel, a, b, c).elements())));
                    }
                }
            }
        }

        return result;
    }

    /*
     * Type 3 extension.
     */
    private static List<Pair<String, Configuration>> type3Extensions(final Configuration config) {
        final var result = new ArrayList<Pair<String, Configuration>>();

        final var signature = signature(config.getSpi(), config.getPi());
        final var _cyclesSizes = new HashMap<Integer, Integer>();
        final var indexesByLabel = new HashMap<Integer, List<Integer>>();
        for (int i = 0; i < signature.length; i++) {
            _cyclesSizes.computeIfAbsent((int) Math.floor(signature[i]), _s -> (int) 0);
            _cyclesSizes.computeIfPresent((int) Math.floor(signature[i]), (k, v) -> (int) (v + 1));
            indexesByLabel.computeIfAbsent((int) Math.floor(signature[i]), _s -> new ArrayList<>());
            int finalI = i;
            indexesByLabel.computeIfPresent((int) Math.floor(signature[i]), (k, v) -> {
                v.add(finalI);
                return v;
            });
        }

        final var cycleIndex = cycleIndex(config.getSpi(), config.getPi());
        final var cyclesByLabel = new HashMap<Integer, Cycle>();
        for (int i = 0; i < signature.length; i++) {
            final int _i = i;
            cyclesByLabel.computeIfAbsent((int) Math.floor(signature[i]), k -> cycleIndex[config.getPi().get(_i)]);
        }

        for (int label = 1; label <= config.getSpi().size(); label++) {
            if (!isOriented(signature, label)) {
                for (int a = 0; a < signature.length; a++) {
                    for (int b = a; b < signature.length; b++) {
                        final var extendedSignature = unorientedExtension(signature, (int) label, a, b).elements();
                        var extension = ofSignature(extendedSignature);
                        if (remainsUnoriented(indexesByLabel.get((int) label), a, b)) {
                            if (extension.getOpenGates().size() <= 2) {
                                result.add(new Pair<>(String.format("a=%d b=%d, extended cycle: %s", a, b, cyclesByLabel.get(label)), extension));
                            }
                        } else if (_cyclesSizes.get((int) label) == 3) {
                            extension = ofSignature(makeOriented5Cycle(extendedSignature, label));
                            result.add(new Pair<>(String.format("a=%d b=%d, extended cycle: %s, turn oriented", a, b,
                                    cyclesByLabel.get(label)), extension));
                        }
                    }
                }
            }
        }

        return result;
    }

    private static boolean remainsUnoriented(final List<Integer> indexes, final int... newIndices) {
        final var intervals = new HashSet<Pair<Integer, Integer>>();

        for (final var index : newIndices) {
            for (int i = 0; i < indexes.size(); i++) {
                int left = indexes.get(i), right = indexes.get((i + 1) % indexes.size());
                if ((left < index && index <= right) ||
                        (right < left && (left < index || index <= right))) {
                    intervals.add(new Pair<>(left, right));
                }
            }
        }

        return intervals.size() == 1;
    }

    private static float[] makeOriented5Cycle(final float[] extension, final int label) {
        final var fractions = new float[]{0.1F, 0.3F, 0.5F, 0.2F, 0.4F};
        int count = 0;
        for (int i = 0; i < extension.length; i++) {
            if (count > fractions.length) {
                break;
            }
            if (Math.floor(extension[i]) == label) {
                extension[i] += fractions[count++];
            }
        }
        return extension;
    }

    private static boolean isOriented(float[] signature, int label) {
        for (float s : signature) {
            if (s % 1 > 0 && Math.floor(s) == label) {
                return true;
            }
        }
        return false;
    }

    private static FloatArrayList unorientedExtension(final float[] signature, final int label, final int... positions) {
        Preconditions.checkArgument(1 < positions.length && positions.length <= 3);
        Arrays.sort(positions);
        final var extension = new FloatArrayList(signature);
        for (int i = 0; i < positions.length; i++) {
            extension.beforeInsert(positions[i] + i, label);
        }
        extension.trimToSize();
        return extension;
    }

    @SneakyThrows
    public static Pair<MulticyclePermutation, List<Cycle>> getSorting(final Path path) {
        final var reader = new BufferedReader(new FileReader(path.toFile()));
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

            final var sorting = new ArrayList<Cycle>();
            if (line.trim().equals("ALLOWS (11/8)-SEQUENCE")) {
                while ((line = reader.readLine()) != null) {
                    line = line.trim();

                    if (!line.equals("<div style=\"margin-top: 10px; \">")) {
                        continue;
                    }

                    line = reader.readLine();

                    final var move = line.split(": ")[1].replace(" ", ",")
                            .replace("<br>", "");
                    sorting.add(Cycle.create(move));
                }
                return new Pair<>(spi, sorting);
            }
        }

        return null;
    }
}