package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import com.google.common.primitives.Ints;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

import static br.unb.cic.tdp.base.CommonOperations.pivots;
import static br.unb.cic.tdp.permutation.PermutationGroups.computeProduct;
import static br.unb.cic.tdp.proof.SortOrExtend.*;

@Slf4j
public class Extensions {

    public static void generate(final String outputDir, final double minRate) throws SQLException {
        val storage = new DerbyProofStorage(outputDir, "extensions");

        val parallelism = Integer.parseInt(System.getProperty("java.util.concurrent.ForkJoinPool.common.parallelism",
                "%d".formatted(Runtime.getRuntime().availableProcessors())));
        try (val pool = new ForkJoinPool(parallelism)) {
            pool.execute(new SortOrExtend(configurationPair("(0)", 0), configurationPair("(0)", 0), storage, minRate));
            pool.execute(new SortOrExtend(configurationPair("(0)", 0), configurationPair("(0 2 1)", 0), storage, minRate));
        }

//        storage.findAllNoSortings().stream()
//                .parallel()
//                .forEach(configurationPair -> renderNoSortingCase(configurationPair.getLeft(), configurationPair.getRight(), outputDir, storage));
//
//        try (val cursor = storage.findAllSortings()) {
//            cursor.stream()
//                    .parallel()
//                    .forEach(record -> {
//                        val config = record.value1().split("#");
//                        val configurationPair = Pair.of(
//                                new Configuration(config[0]),
//                                Arrays.stream(config[1].replaceAll("[\\[\\]\\s]", "").split(","))
//                                        .map(Integer::parseInt)
//                                        .collect(Collectors.toCollection(TreeSet::new))
//                        );
//                        val sorting = Arrays.stream(record.value2().replace("[", "").replace("]", "").split(", "))
//                                .map(Cycle::of)
//                                .toList();
//
//                        renderSorting(configurationPair.getLeft(), configurationPair.getRight(), sorting, outputDir);
//                    });
//        }
    }

    @SneakyThrows
    private static void renderNoSortingCase(
            final Configuration configuration,
            final Set<Integer> pivots,
            final String outputDir,
            final ProofStorage storage
    ) {
        val out = new PrintStream("%s/search/%s%s.html".formatted(outputDir, configuration.getSpi(), pivots));

        out.println("""
                <html>
                   <head>
                      <link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.4.1/css/bootstrap.min.css" integrity="sha384-Vkoo8x4CGsO3+Hhxv8T/Q5PaXtkKtu6ug5TOeNV6gBiFeWPGFN9MuhOf23Q9Ifjh" crossorigin="anonymous">
                      <script src="https://code.jquery.com/jquery-3.4.1.slim.min.js" integrity="sha384-J6qa4849blE2+poT4WnyKhv5vZF5SrPo0iEjwBvKU7imGFAV0wwj1yYfoRSJoZ+n" crossorigin="anonymous"></script>
                      <script src="https://cdn.jsdelivr.net/npm/popper.js@1.16.0/dist/umd/popper.min.js" integrity="sha384-Q6E9RHvbIyZFJoft+2mJbHaEWldlvI9IOYy5n3zV9zzTtmI3UksdQRVvoxMfooAo" crossorigin="anonymous"></script>
                      <script src="https://stackpath.bootstrapcdn.com/bootstrap/4.4.1/js/bootstrap.min.js" integrity="sha384-wfSDF2E50Y2D1uUdj0O3uMBJnjuUD4Ih7YwaYd1iqfktj0Uod8GCExl3Og8ifwB6" crossorigin="anonymous"></script>
                      <script src="../draw-config.js"></script>
                      <style>* { font-size: small; }</style>
                   </head>
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
                   function updateCanvas(canvasId, spi) {
                      var pi = []; for (var i = 0; i < spi.flatMap(c => c).length; i++) { pi.push(i); }\
                      var canvas = document.getElementById(canvasId);
                      canvas.height = calcHeight(canvas, spi, pi);
                      canvas.width = pi.length * padding;
                      draw(canvas, spi, pi);
                   }
                </script>
                <div style="margin-top: 10px; margin-left: 10px">""");

        out.println("<canvas id=\"canvas\"></canvas>");
        out.printf("<script>updateCanvas('canvas', %s);</script>%n",
                permutationToJsArray(configuration.getSpi()));

        out.printf("<h6>%s</h6>%n", configuration.getSpi());

        out.printf("Pivots: %s<br>%n", pivots);

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
        //renderExtensions(type1Extensions(configuration), out, storage);
        out.println("    </td>");
        out.println("    <td style=\"vertical-align: baseline; border: 1px solid lightgray;\">");
        //renderExtensions(type1Extensions(configuration), out, storage);
        out.println("    </td>");
        out.println("    <td style=\"vertical-align: baseline; border: 1px solid lightgray;\">");
        //renderExtensions(type3Extensions(configuration), out, storage);
        out.println("    </td>");
        out.println("  </tr>");
        out.println("</table>");

        out.println("</body>");
        out.println("</html>");
    }

    private static void renderExtensions(
            final List<Pair<String, Configuration>> extensions,
            final PrintStream out,
            final ProofStorage storage
    ) {
        for (val extension : extensions) {
            val configuration = Configuration.ofSignature(extension.getRight().getSignature().getContent());
            val pivots = pivots(configuration);

            val canonical = getCanonical(Pair.of(configuration, pivots), c -> pivots);

            val hasSorting = storage.findSorting(canonical);
            out.println(hasSorting.isPresent() ? "<div style=\"margin-top: 10px; background-color: rgba(153, 255, 153, 0.15)\">" :
                    "<div style=\"margin-top: 10px; background-color: rgba(255, 0, 0, 0.05);\">");
            out.printf("%s<br>%n", extension.getLeft());
            out.printf("%s<br>%n", "%s EXTENSION".formatted(hasSorting.isPresent() ? "GOOD" : "BAD"));
            out.printf("Pivots: %s<br>%n", pivots);
            val jsSpi = permutationToJsArray(configuration.getSpi());
            val label = configuration.getSpi().toString() + pivots;
            out.printf("""
                            Extension: <a href="" \
                            onclick="\
                            updateCanvas('modalCanvas', %s); \
                            $('h6.modal-title').text('%s');\
                            $('#modal').modal('show'); \
                            return false;">%s</a><br>%n""",
                    jsSpi, label, configuration.getSpi().toString());
            out.printf("View canonical extension: <a href=\"%s.html\">%s</a>%n", "%s%s".formatted(canonical.getLeft().getSpi(), canonical.getRight()), canonical.getLeft().getSpi());
            out.println("</div>");
        }
    }


    public static String permutationToJsArray(final MulticyclePermutation permutation) {
        return "[%s]".formatted(permutation
                .stream().map(c -> "[%s]".formatted(Ints.asList(c.getSymbols()).stream()
                        .map(s -> Integer.toString(s))
                        .collect(Collectors.joining(","))))
                .collect(Collectors.joining(",")));
    }

    private static String cycleToJsArray(final Cycle cycle) {
        return "[%s]".formatted(Ints.asList(cycle.getSymbols()).stream()
                .map(s -> Integer.toString(s))
                .collect(Collectors.joining(",")));
    }

    @SneakyThrows
    private static void renderSorting(
            final Configuration configuration,
            final Set<Integer> pivots,
            final List<Cycle> sorting,
            final String outputDir
    ) {
        try (val out = new PrintStream("%s/search/%s%s.html".formatted(outputDir, configuration.getSpi(), pivots));
             val writer = new PrintWriter(out)) {
//            val context = new VelocityContext();
//            context.put("spi", configuration.getSpi());
//            context.put("piSize", configuration.getPi().size());
//            context.put("jsSpi", permutationToJsArray(configuration.getSpi()));
//            context.put("jsPi", cycleToJsArray(configuration.getPi()));
//            context.put("sorting", sorting);
//            context.put("pivots", pivots);

            val spis = new ArrayList<MulticyclePermutation>();
            val jsSpis = new ArrayList<String>();
            val jsPis = new ArrayList<String>();
            var spi = configuration.getSpi();
            var pi = configuration.getPi();
            for (val move : sorting) {
                spis.add(spi = computeProduct(spi, move.getInverse()));
                jsSpis.add(permutationToJsArray(spi));
                jsPis.add(cycleToJsArray(pi = computeProduct(move, pi).asNCycle()));
            }
//            context.put("spis", spis);
//            context.put("jsSpis", jsSpis);
//            context.put("jsPis", jsPis);

//            val template = Velocity.getTemplate("templates/sorting.html");
//            template.merge(context, writer);
        }
    }
}