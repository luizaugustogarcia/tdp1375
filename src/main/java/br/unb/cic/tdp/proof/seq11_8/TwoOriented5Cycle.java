package br.unb.cic.tdp.proof.seq11_8;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import org.apache.commons.math3.util.Pair;
import org.paukov.combinatorics.Factory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import static br.unb.cic.tdp.base.CommonOperations.searchForSortingSeq;
import static br.unb.cic.tdp.proof.ProofGenerator.renderSorting;
import static br.unb.cic.tdp.proof.ProofGenerator.searchForSorting;

/* The Extension.java extends to oriented 5-cycles. Moreover, the Combinations.java also treats
the case where we have combinations of oriented 5-cycles, so this case analysis it not necessary anymore. */
@Deprecated
public class TwoOriented5Cycle {

    public static void generate(final Pair<Map<Configuration, List<Cycle>>,
            Map<Integer, List<Configuration>>> knownSortings, final boolean shouldAlsoUseBruteForce,
                                final String outputDir) throws IOException {
        final var configs = new HashSet<Configuration>();
        final var fractions = new float[]{0.1F, 0.3F, 0.5F, 0.2F, 0.4F};

        Files.createDirectories(Paths.get(outputDir + "\\two-oriented-5-cycle\\"));

        final var out = new PrintStream(new File(outputDir + "\\two-oriented-5-cycle\\index.html"));
        out.println("<HTML>" +
                "<script src=\"../draw-config.js\"></script>" +
                "<link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.4.1/css/bootstrap.min.css\">" +
                "<script src=\"https://ajax.googleapis.com/ajax/libs/jquery/3.4.1/jquery.min.js\"></script>" +
                "<script src=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.4.1/js/bootstrap.min.js\"></script>" +
                "<HEAD><style>* { font-size: small; }</style><TITLE></TITLE>" +
                "</HEAD><BODY><PRE>");

        out.println("<pi>The possible configurations are (all have a (4,3)-sequence): ");

        for (final var permutation : Factory.createPermutationGenerator(Factory.createVector(new Byte[]{1, 1, 1, 1, 1, 2, 2, 2, 2, 2}))) {
            final var signature = new float[10];

            var nextFraction = new int[2];
            for (int i = 0; i < permutation.getSize(); i++) {
                signature[i] = permutation.getValue(i) + fractions[nextFraction[permutation.getValue(i) - 1]];
                nextFraction[permutation.getValue(i) - 1]++;
            }

            final var config = Configuration.fromSignature(signature);
            final var canonicalConfig = config.getCanonical();
            out.println("<p>" + config.getSpi());
            out.println("Hash code " + config.hashCode());
            out.println("Signature " + config.getSignature());
            out.println(String.format("View canonical configuration <a href=\"%s.html\">%s</a>",
                    canonicalConfig.getSpi(), canonicalConfig.getSpi()));

            if (!configs.contains(config)) {
                configs.add(config);

                var sorting = searchForSorting(config, knownSortings, shouldAlsoUseBruteForce);
                if (sorting == null) {
                    sorting = searchForSortingSeq(config.getPi(), config.getSpi(), new Stack<>(), config.getSpi().getNumberOfEvenCycles(), 1.375F);
                }

                assert sorting != null : "ERROR";

                try (final var writer = new FileWriter(
                        new File(outputDir + "\\two-oriented-5-cycle\\" + canonicalConfig.getSpi() + ".html"))) {
                    renderSorting(canonicalConfig, canonicalConfig.translatedSorting(config, sorting), writer);
                }
            }
        }
    }
}
