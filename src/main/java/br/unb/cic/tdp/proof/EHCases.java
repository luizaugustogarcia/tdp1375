package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.Configuration;
import br.unb.cic.tdp.permutation.Cycle;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static br.unb.cic.tdp.CommonOperations.is11_8;

public class EHCases extends EHProofTraverser {

    public static void main(String[] args) throws FileNotFoundException {
        final var printer = new PrintStream(new File("C:\\Users\\USER-Admin\\workspace\\tdp1375\\proof" + "\\eh-unoriented.txt"));
        generate("C:\\Users\\USER-Admin\\workspace\\tdp1375\\sbt1375_proof\\", printer);
    }

    public static void generate(final String inputDir, final PrintStream printer) {
        final var verifiedConfigurations = new HashSet<Configuration>();

        final var visitedFiles = new HashSet<String>();

        final var cases = new EHCases();

        // unoriented interleaving pair
        cases.traverse(inputDir + "bfs_files/", "[3](0_4_2)[3](1_5_3).html",
                visitedFiles, verifiedConfigurations, printer, 0);

        // unoriented intersecting pair
        cases.traverse(inputDir + "bfs_files/", "[3](0_3_1)[3](2_5_4).html",
                visitedFiles, verifiedConfigurations, printer, 0);

        // --------------------
        // BAD SMALL COMPONENTS
        // --------------------

        // the unoriented interleaving pair
        cases.traverse(inputDir + "comb_files/", "[3](0_4_2)[3](1_5_3).html",
                visitedFiles, verifiedConfigurations, printer, 0);

        // the unoriented necklaces of size 4
        cases.traverse(inputDir + "comb_files/", "[3](0_10_2)[3](1_5_3)[3](4_8_6)[3](7_11_9).html",
                visitedFiles, verifiedConfigurations, printer, 0);

        // the twisted necklace of size 4
        cases.traverse(inputDir + "comb_files/", "[3](0_7_5)[3](1_11_9)[3](2_6_4)[3](3_10_8).html",
                visitedFiles, verifiedConfigurations, printer, 0);

        // the unoriented necklaces of size 5
        cases.traverse(inputDir + "comb_files/", "[3](0_4_2)[3](1_14_12)[3](3_7_5)[3](6_10_8)[3](9_13_11).html",
                visitedFiles, verifiedConfigurations, printer, 0);

        // the unoriented necklaces of size 6
        cases.traverse(inputDir + "comb_files/", "[3](0_16_2)[3](1_5_3)[3](4_8_6)[3](7_11_9)[3](10_14_12)[3](13_17_15).html",
                visitedFiles, verifiedConfigurations, printer, 0);
    }

    protected void desimplify(final Configuration configuration, final List<Cycle> rhos,
                              final Set<Configuration> verifiedConfigurations, final PrintStream printer, final int depth) {

    }

    @Override
    protected void readEHCase(final Configuration configuration, final List<Cycle> rhos, final PrintStream printer,
                              final int depth) {
        if (is11_8(configuration.getSpi(), configuration.getPi(), rhos)) {
            printCase(configuration, rhos, printer, depth);
        } else {
            throw new RuntimeException("ERROR");
        }
    }
}
