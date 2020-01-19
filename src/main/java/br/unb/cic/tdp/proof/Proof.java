package br.unb.cic.tdp.proof;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Proof {

  public static void main(String[] args) {
    generateProof(System.out);
  }

  private static void generateProof(final PrintStream printer) {
    final Consumer<Case> printCase = _case -> {
      printer.println(_case.getSigmaPiInverse() + ";"
          + _case.getRhos().stream().map(r -> Arrays.toString(r)).collect(Collectors.joining("-")));
    };

    /**
     * If there is an odd (even-length) cycle in $\spi$, then a $2$-move exists.
     */
    printer.println("------------------");
    printer.println("- Proposition 17 -");
    printer.println("------------------");

    OddCyclesCases.generate1_1Cases().forEach(printCase);
    printer.println();

    /**
     * If $\spi \neq\ iota$, then there is a $(3/2)$-sequence.
     */
    printer.println("------------");
    printer.println("- Lemma 18 -");
    printer.println("------------");

    Cases_3_2.generate().forEach(printCase);
    Oriented5Cycle.generate().forEach(printCase);
    printer.println();

    /**
     * If there is an even (odd-length) $k$-cycle $\gamma=(a\dots b\dots c\dots)$ in
     * $\spi$ such that $k\geq 7$ and $(a,b,c)$ is an oriented triple, then there is
     * a $\frac{4}{3}$-sequence.
     */
    printer.println("------------");
    printer.println("- Lemma 19 -");
    printer.println("------------");

    OrientedCycleGreaterThan5.generate().forEach(printCase);
    printer.println();

    /**
     * If it is possible to build a sufficient configuration $\Gamma$ of $\spi$,
     * then there is $\frac{11}{8}$-sequence.
     */
    printer.println("------------");
    printer.println("- Lemma 24 -");
    printer.println("------------");

    final var cases = new ArrayList<Case>();
    cases.addAll(DesimplifyUnoriented.generate("C:\\Users\\USER-Admin\\Temp\\sbt1375_proof.tar\\sbt1375_proof\\"));
    cases.addAll(DesimplifyOriented.generate("C:\\Users\\USER-Admin\\Temp\\sbt1375_proof.tar\\sbt1375_proof\\"));
    cases.forEach(printCase);

    // TODO now build the cases using the BFS search and ensure that each one whose
    // norm is at most 9 has a solution given by the previous step
  }
}