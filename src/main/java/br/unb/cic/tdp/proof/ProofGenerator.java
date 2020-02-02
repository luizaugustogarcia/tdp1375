package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.permutation.Cycle;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class ProofGenerator {

    public static void main(final String[] args) throws FileNotFoundException {
        // mvn exec:java -Dexec.mainClass="br.unb.cic.tdp.proof.ProofGenerator" -Dexec.args=".\proof .\sbt1375_proof"
        generate(args[0], args[1]);
    }

    private static void generate(final String proofFolder, final String ehProofFolder) throws FileNotFoundException {
        final BiConsumer<Case, PrintStream> printCase = (_case, printer) -> {
            // By convenience, \pi is (0,1,2,...,n)
            printer.println(_case.getSpi() + "-"
                    + _case.getRhos().stream().map(Cycle::toString).collect(Collectors.joining(",")));
        };

        /*
         * ------------------
         * - Proposition 17 -
         * ------------------
         *
         * If there is an odd (even-length) cycle in $\spi$, then a $2$-move exists.
         */
        final var prop17Printer = new PrintStream(new File(proofFolder + "\\proposition-17.txt"));
        OddCyclesCases.generate().forEach(_case -> printCase.accept(_case, prop17Printer));

        /*
         * ------------
         * - Lemma 18 -
         * ------------
         *
         * If $\spi \neq\ iota$, then there is a $(3/2)$-sequence.
         */
        final var lemma18 = new PrintStream(new File(proofFolder + "\\lemma-18.txt"));
        Cases3_2.generate().forEach(_case -> printCase.accept(_case, lemma18));
        Oriented5Cycle.generate().forEach(_case -> printCase.accept(_case, lemma18));

        /*
         * ------------
         * - Lemma 19 -
         * ------------
         *
         * If there is an even (odd-length) $k$-cycle $\gamma=(a\dots b\dots c\dots)$ in
         * $\spi$ such that $k\geq 7$ and $(a,b,c)$ is an oriented triple, then there is
         * a $\frac{4}{3}$-sequence.
         */
        final var lemma19 = new PrintStream(new File(proofFolder + "\\lemma-19.txt"));
        OrientedCycleGreaterThan5.generate().forEach(_case -> printCase.accept(_case, lemma19));

        /*
         * ------------
         * - Lemma 24 -
         * ------------
         *
         * If it is possible to build a sufficient configuration $\Gamma$ of $\spi$,
         * then there is $\frac{11}{8}$-sequence.
         */

        final var cases = new ArrayList<Case>();
        cases.addAll(DesimplifyUnoriented.generate(ehProofFolder));
        cases.addAll(DesimplifyOriented.generate(ehProofFolder));
        final var lemma24 = new PrintStream(new File(proofFolder + "\\lemma-24.txt"));
        cases.forEach(_case -> printCase.accept(_case, lemma24));

        // TODO facilitate the execution of the algorithm, having generated the proof previously.
        // TODO update in the article, the number of cases generated - mirroring changed the number of cases
        // TODO when persisting the cases, care about the mirrors - EH list only one in their proof, maybe it is the case to create two entries for each case
        // TODO revise algorithms - care about the mirroring
        // TODO re-run/re-test the algorithms
    }
}