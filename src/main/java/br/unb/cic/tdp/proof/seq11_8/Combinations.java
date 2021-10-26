package br.unb.cic.tdp.proof.seq11_8;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import cern.colt.list.FloatArrayList;
import com.google.common.primitives.Floats;
import lombok.SneakyThrows;
import org.apache.commons.math3.util.Pair;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static br.unb.cic.tdp.proof.ProofGenerator.*;

public class Combinations {
    private static final Configuration oriented5Cycle = new Configuration(
            new MulticyclePermutation("(0,3,1,4,2)"));
    private static final Configuration interleavingPair = new Configuration(
            new MulticyclePermutation("(0,4,2)(1,5,3)"));
    private static final Configuration necklaceSize4 = new Configuration(
            new MulticyclePermutation("(0,10,2)(1,5,3)(4,8,6)(7,11,9)"));
    private static final Configuration twistedNecklaceSize4 = new Configuration(
            new MulticyclePermutation("(0,7,5)(1,11,9)(2,6,4)(3,10,8)"));
    private static final Configuration necklaceSize5 = new Configuration(
            new MulticyclePermutation("(0,4,2)(1,14,12)(3,7,5)(6,10,8)(9,13,11)"));
    private static final Configuration necklaceSize6 = new Configuration(
            new MulticyclePermutation("(0,16,2)(1,5,3)(4,8,6)(7,11,9)(10,14,12)(13,17,15)"));
    public static final Configuration[] BAD_SMALL_COMPONENTS = new Configuration[]{oriented5Cycle, interleavingPair,
            necklaceSize4, twistedNecklaceSize4, necklaceSize5, necklaceSize6};

    public static void generate(final String outputDir) throws IOException {

        Files.createDirectories(Paths.get(outputDir + "/comb/"));

        sortOrExtend(new Pair<>(null, oriented5Cycle), outputDir);
        sortOrExtend(new Pair<>(null, interleavingPair), outputDir);
        sortOrExtend(new Pair<>(null, necklaceSize4), outputDir);
        sortOrExtend(new Pair<>(null, twistedNecklaceSize4), outputDir);
        sortOrExtend(new Pair<>(null, necklaceSize5), outputDir);
        sortOrExtend(new Pair<>(null, necklaceSize6), outputDir);
    }

    @SneakyThrows
    private static void sortOrExtend(final Pair<String, Configuration> config, final String outputDir) {
        final var canonicalConfig = config.getSecond().getCanonical();
        final var file = new File(outputDir + "/comb/" + config.getSecond().getSpi() + ".html");
        if (file.exists()) {
            return;
        }

        var sorting = searchForSorting(config.getSecond(), true);
        if (sorting.isPresent() && !sorting.get().isEmpty()) {
            try (final var writer = new FileWriter(outputDir + "/comb/" + canonicalConfig.getSpi() + ".html")) {
                renderSorting(canonicalConfig, canonicalConfig.translatedSorting(config.getSecond(), sorting.get()), writer);
            }
            return;
        } else {
            // before performing any extension, the 3-norm must be less than 8
            assert config.getSecond().get3Norm() < 8 : "ERROR";
        }

        try (final var out = new PrintStream(outputDir + "/comb/" + config.getSecond().getSpi() + ".html")) {
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
                    permutationToJsArray(config.getSecond().getSpi())));

            out.println("<h6>" + config.getSecond().getSpi() + "</h6>");

            out.println("Hash code: " + config.getSecond().hashCode() + "<br>");
            out.println("Signature: " + config.getSecond().getSignature() + "<br>");
            out.println("3-norm: " + config.getSecond().getSpi().get3Norm());

            out.println("<p style=\"margin-top: 10px;\"></p>");
            out.println("THE EXTENSIONS ARE:");

            for (final var extension : extend(config.getSecond())) {
                final var s = searchForSorting(extension.getSecond(), true);
                final var hasSorting = s.isPresent() && !s.get().isEmpty();
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

                if (!hasSorting) {
                    out.println(String.format("View extension: <a href=\"%s.html\">%s</a>",
                            extension.getSecond().getSpi(), extension.getSecond().getSpi()));
                } else {
                    out.println(String.format("View sorting: <a href=\"%s.html\">%s</a>",
                            extension.getSecond().getCanonical().getSpi(), extension.getSecond().getCanonical().getSpi()));
                }

                out.println("</div>");

                sortOrExtend(extension, outputDir);
            }

            out.println("</div></body></html>");
        }
    }

    private static List<Pair<String, Configuration>> extend(final Configuration config) {
        final var result = new ArrayList<Pair<String, Configuration>>();
        for (final var badSmallComponent : BAD_SMALL_COMPONENTS) {
            for (int i = 0; i <= config.getPi().size(); i++) {
                final var badSmallComponentSignature = badSmallComponent.getSignature().getContent().clone();
                for (int j = 0; j < badSmallComponentSignature.length; j++) {
                    badSmallComponentSignature[j] += config.getSpi().size();
                }
                final var signature = new FloatArrayList(config.getSignature().getContent().clone());
                signature.beforeInsertAllOf(i, Floats.asList(badSmallComponentSignature));
                signature.trimToSize();

                final String info;
                if (badSmallComponent == oriented5Cycle)
                    info = "bad oriented 5-cycle";
                else if (badSmallComponent == interleavingPair)
                    info = "unoriented interleaving pair";
                else if (badSmallComponent == necklaceSize4)
                    info = "unoriented necklace of size 4";
                else if (badSmallComponent == twistedNecklaceSize4)
                    info = "unoriented twisted necklace of size 4";
                else if (badSmallComponent == necklaceSize5)
                    info = "unoriented necklace of size 5";
                else
                    info = "unoriented necklace of size 6";

                result.add(new Pair<>("pos=" + i + " add " + info, Configuration.ofSignature(signature.elements())));
            }
        }
        return result;
    }
}