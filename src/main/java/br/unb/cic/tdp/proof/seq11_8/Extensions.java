package br.unb.cic.tdp.proof.seq11_8;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.proof.util.SortOrExtend;
import br.unb.cic.tdp.util.Pair;
import cern.colt.list.FloatArrayList;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static br.unb.cic.tdp.base.CommonOperations.*;
import static br.unb.cic.tdp.proof.ProofGenerator.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

import static br.unb.cic.tdp.base.Configuration.*;

public class Extensions {

    @SneakyThrows
    public static void generate(final String outputDir) {
        Files.createDirectories(Paths.get(outputDir + "/dfs/"));
        Files.createDirectories(Paths.get(outputDir + "/dfs/working/"));
        Files.createDirectories(Paths.get(outputDir + "/dfs/bad-cases/"));

        cleanUpIncompleteCases(outputDir + "/dfs/");

        cleanUpBadExtensionAndInvalidFiles(outputDir + "/dfs/");

        // ATTENTION: The Sort Or Extend fork/join can never run with BAD EXTENSION files in the dfs directory.
        // Otherwise, it will wrongly skip cases.

        var pool = new ForkJoinPool();
        // oriented 5-cycle
        pool.execute(new SortOrExtendExtensions(new Configuration(new MulticyclePermutation("(0,3,1,4,2)")), outputDir + "/dfs/"));
        // interleaving pair
        pool.execute(new SortOrExtendExtensions(new Configuration(new MulticyclePermutation("(0,4,2)(1,5,3)")), outputDir + "/dfs/"));
        // intersecting pair
        pool.execute(new SortOrExtendExtensions(new Configuration(new MulticyclePermutation("(0,3,1)(2,5,4)")), outputDir + "/dfs/"));
        pool.shutdown();
        // boundless
        pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);

        final var executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        Files.list(Paths.get(outputDir + "/dfs/bad-cases/"))
                .map(Path::toFile)
                .forEach(file -> {
                    executor.submit(() -> makeHtmlNavigation(new Configuration(new MulticyclePermutation(file.getName())), outputDir));
                });

        executor.shutdown();
        // boundless
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }

    @SneakyThrows
    public static void cleanUpBadExtensionAndInvalidFiles(final String outputDir) {
        final var dir = new File(outputDir);

        final var files = new ArrayList<File>();
        Stream.of(dir.listFiles(file -> file.getName().endsWith(".html")))
                .parallel()
                .forEach(f -> {
                    final var file = new File(outputDir + f.getName());

                    try {
                        if (isBadExtension(file)) {
                            files.add(file);
                        } else {
                            final var canonical = new Configuration(new MulticyclePermutation(f.getName().replace(" ", ",")));
                            final var sorting = getSorting(file.toPath());
                            if (!is11_8(canonical.getSpi(), canonical.getPi(), sorting.getSecond())) {
                                files.add(file);
                            }
                        }
                    } catch (IllegalStateException e) {
                        files.add(file);
                    }
                });

        boolean canContinue = true;
        for (final var file : files) {
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
        final var reader = new BufferedReader(new FileReader(path.toFile()), 1024 * 10);
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
    private static void renderExtensions(final List<Pair<String, Configuration>> extensions, final PrintStream out, final String outputDir) {
        for (final var extension : extensions) {
            final var configuration = extension.getSecond();
            final var canonical = extension.getSecond().getCanonical();

            final var badCaseFile = new File(outputDir + "/dfs/bad-cases/" + canonical.getSpi());
            final var hasSorting = !badCaseFile.exists();
            out.println(hasSorting ? "<div style=\"margin-top: 10px; background-color: rgba(153, 255, 153, 0.15)\">" :
                    "<div style=\"margin-top: 10px; background-color: rgba(255, 0, 0, 0.05);\">");
            out.println(extension.getFirst() + "<br>");
            out.println(((hasSorting ? "GOOD" : "BAD") + " EXTENSION") + "<br>");
            out.println("Hash code: " + configuration.hashCode() + "<br>");
            out.println("3-norm: " + configuration.getSpi().get3Norm() + "<br>");
            out.println("Signature: " + configuration.getSignature() + "<br>");
            final var jsSpi = permutationToJsArray(configuration.getSpi());
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
        final var excludeFiles = new ArrayList<File>();

        Files.list(Paths.get(outputDir + "/working/"))
                .map(Path::toFile)
                .forEach(excludeFiles::add);

        boolean canContinue = true;
        for (final var file : excludeFiles) {
            boolean deleted = FileUtils.deleteQuietly(file);
            canContinue &= deleted;
            if (!deleted) {
                System.out.println("rm \"" + file + "\"");
            }
        }

        if (!canContinue)
            throw new RuntimeException("ERROR: working files not deleted, cannot continue.");
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
                for (int b = 0; b < signature.length; b++) {
                    for (int c = b; c < signature.length; c++) {
                        if (!(i == b && b == c)) {
                            result.add(new Pair<>(String.format("a=%d b=%d c=%d", i, b, c),
                                    ofSignature(unorientedExtension(signature, newCycleLabel, i, b, c).elements())));
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

        final var newCycleLabel = config.getSpi().size() + 1;

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
            _cyclesSizes.putIfAbsent((int) Math.floor(signature[i]), 0);
            _cyclesSizes.computeIfPresent((int) Math.floor(signature[i]), (k, v) -> v + 1);
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
                        float[] extendedSignature = unorientedExtension(signature, label, a, b).elements();

                        Configuration extension = ofSignature(extendedSignature);

                        if (remainsUnoriented(indexesByLabel.get(label), a, b)) {
                            if (extension.getOpenGates().size() <= 2) {
                                result.add(new Pair<>(String.format("a=%d b=%d, extended cycle: %s", a, b, cyclesByLabel.get(label)), extension));
                            }
                        } else if (_cyclesSizes.get(label) == 3) {
                            final var extension_ = extend(cyclesByLabel, label, signature, a, b);
                            final var fractions = new float[]{0.1F, 0.3F, 0.5F, 0.2F, 0.4F};
                            for (int i = 0; i < fractions.length; i++) {
                                fractions[i] += label;
                            }

                            if (areSymbolsInCyclicOrder(extension_, fractions)) { // otherwise, it accepts a 2-move
                                result.add(new Pair<>(String.format("a=%d b=%d, extended cycle: %s, turn oriented", a, b,
                                        cyclesByLabel.get(label)), Configuration.ofSignature(extension_)));
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    public static boolean areSymbolsInCyclicOrder(final float[] elements, final float[] other) {
        int next = 0;

        outer: for (int i = 0; i < elements.length; i++) {
            if (elements[i] == other[next]) {
                for (int j = 0; j <= elements.length; j++) {
                    int index = (i + j) % elements.length;
                    if (Math.floor(elements[index]) == Math.floor(other[next % other.length])) {
                        if (elements[index] == other[next % other.length]) {
                            next++;
                            if (next > other.length) {
                                break outer;
                            }
                        } else {
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    private static float[] extend(final Map<Integer, Cycle> cyclesByLabel, final int label, float[] signature,
                                  final int a, final int b) {
        final Cycle cycle = cyclesByLabel.get(label).startingBy(cyclesByLabel.get(label).getMaxSymbol());

        float[] copiedsignature = new float[signature.length];
        System.arraycopy(signature, 0, copiedsignature, 0, signature.length);

        for (int i = cycle.getSymbols().length - 1; i >= 0; i--) {
            copiedsignature[cycle.getSymbols()[i]] += 0.1 * (i + 1);
        }

        float next = 0.5f;
        final var positions = new int[]{a, b};
        final var extension = new FloatArrayList(copiedsignature);
        int inserted = 0;
        for (int position : positions) {
            extension.beforeInsert(position + inserted, label + next);
            next -= 0.1f;
            inserted++;
        }
        extension.trimToSize();

        return extension.elements();
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
    private static void makeHtmlNavigation (final Configuration configuration, final String outputDir) {
        try (final var out = new PrintStream(outputDir + "/dfs/" + configuration.getSpi() + ".html")) {
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

    static class SortOrExtendExtensions extends SortOrExtend {

        public SortOrExtendExtensions(final Configuration configuration, final String outputDir) {
            super(configuration, outputDir);
        }

        @Override
        protected void extend(Configuration canonical) {
            type1Extensions(canonical).stream().map(extension -> new SortOrExtendExtensions(extension.getSecond(), outputDir)).forEach(ForkJoinTask::fork);
            type2Extensions(canonical).stream().map(extension -> new SortOrExtendExtensions(extension.getSecond(), outputDir)).forEach(ForkJoinTask::fork);
            type3Extensions(canonical).stream().map(extension -> new SortOrExtendExtensions(extension.getSecond(), outputDir)).forEach(ForkJoinTask::fork);
        }
    }
}