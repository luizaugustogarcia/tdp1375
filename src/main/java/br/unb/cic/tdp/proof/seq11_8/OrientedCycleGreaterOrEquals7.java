package br.unb.cic.tdp.proof.seq11_8;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.permutation.PermutationGroups;
import com.google.common.primitives.Bytes;
import org.paukov.combinatorics.Factory;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Stack;

import static br.unb.cic.tdp.base.CommonOperations.*;
import static br.unb.cic.tdp.permutation.PermutationGroups.computeProduct;

public class OrientedCycleGreaterOrEquals7 {

    /**
     * Generate (4,3)-sequences to apply when there is a cycle in \spi with length
     * greater or equals to 7 that doesn't allow the application of a 2-move.
     */
    public static void generate(final String outputDir) throws IOException {
        Files.createDirectories(Paths.get(outputDir + "/oriented-7-cycle/"));

        final var out = new PrintStream(new File(outputDir + "/oriented-7-cycle/index.html"));
        out.println("<html>\n" +
                "\t<head>\n" +
                "\t\t<link rel=\"stylesheet\" href=\"https://stackpath.bootstrapcdn.com/bootstrap/4.4.1/css/bootstrap.min.css\" integrity=\"sha384-Vkoo8x4CGsO3+Hhxv8T/Q5PaXtkKtu6ug5TOeNV6gBiFeWPGFN9MuhOf23Q9Ifjh\" crossorigin=\"anonymous\">\n" +
                "\t\t<script src=\"https://code.jquery.com/jquery-3.4.1.slim.min.js\" integrity=\"sha384-J6qa4849blE2+poT4WnyKhv5vZF5SrPo0iEjwBvKU7imGFAV0wwj1yYfoRSJoZ+n\" crossorigin=\"anonymous\"></script>\n" +
                "\t\t<script src=\"https://cdn.jsdelivr.net/npm/popper.js@1.16.0/dist/umd/popper.min.js\" integrity=\"sha384-Q6E9RHvbIyZFJoft+2mJbHaEWldlvI9IOYy5n3zV9zzTtmI3UksdQRVvoxMfooAo\" crossorigin=\"anonymous\"></script>\n" +
                "\t\t<script src=\"https://stackpath.bootstrapcdn.com/bootstrap/4.4.1/js/bootstrap.min.js\" integrity=\"sha384-wfSDF2E50Y2D1uUdj0O3uMBJnjuUD4Ih7YwaYd1iqfktj0Uod8GCExl3Og8ifwB6\" crossorigin=\"anonymous\"></script>\n" +
                "\t\t<style>* { font-size: small; }</style>\n" +
                "\t\t<script type=\"text/x-mathjax-config\">\n" +
                "\t\tMathJax.Hub.Config({tex2jax: {inlineMath: [['$','$'], ['\\\\(','\\\\)']]}});\n" +
                "\t\t</script>\n" +
                "\t\t<script type=\"text/javascript\"\n" +
                "\t\tsrc=\"http://cdnjs.cloudflare.com/ajax/libs/mathjax/2.7.1/MathJax.js?config=TeX-AMS-MML_HTMLorMML\">\n" +
                "\t\t</script>\n" +
                "\t</head>\n" +
                "<body><div style=\"margin-top: 10px; margin-left: 10px;\"> \n");

        final var orientedCycle = Cycle.create("0,3,4,1,5,2,6");
        final var orientedTriple = new byte[]{0, 1, 2};

        out.println(orientedCycle + "<br>");
        out.println("<p>Oriented triple: (0,1,2)<br>");

        final var spi = new MulticyclePermutation(orientedCycle);

        out.println("<p>The possible forms of $\\bar{\\pi}$ are (among the 360 generated cases, there are only 71 distinct cases): <br>");

        final var verifiedPis = new HashSet<Cycle>();

        for (final var permutation : Factory.createPermutationGenerator(
                Factory.createVector(Bytes.asList(CANONICAL_PI[7].getSymbols())))) {
            final var pi = Cycle.create(Bytes.toArray(permutation.getVector()));

            if (!verifiedPis.contains(pi)) {
                verifiedPis.add(pi);

                if (areSymbolsInCyclicOrder(pi, orientedTriple)) {
                    final var config = new Configuration(spi, pi);

                    final var moves = searchForSortingSeq(pi, spi, new Stack<>(), 1, 1.375F);

                    assert !moves.isEmpty() : "ERROR";

                    out.println("<div style=\"margin-bottom: 10px; background-color: rgba(153, 255, 153, 0.15)\">");
                    out.println("$\\bar{\\pi}$: " + pi + "<br>");
                    out.println("Hash code: " + config.hashCode() + "<br>");
                    out.println("$\\frac{11}{8}$-SEQUENCE" + "<br>");
                    var _spi = spi;
                    for (int i = 0; i < moves.size(); i++) {
                        final var move = moves.get(i);
                        out.println(String.format("%d: %s <br>", i + 1, move));
                        out.println((_spi = computeProduct(_spi, move.getInverse())) + "<br>");
                    }
                    out.println("</div>");
                }
            }
        }

        out.println("</div></body></html>");
    }
}
