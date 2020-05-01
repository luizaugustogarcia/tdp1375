package br.unb.cic.tdp.proof.eh;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;


public class EHCases {

    public static void main(String[] args) throws FileNotFoundException {
        generate(args[0], new PrintStream(new File(args[1])));
    }

    public static void generate(final String ehProofFolder, final PrintStream printer) {
        final EHProofTraverser.CaseProcessor configConsumer = (configuration, sorting, depth, alreadyVisited) -> {
            print(printer, configuration, sorting, depth, alreadyVisited);
        };

        // unoriented interleaving pair
        EHProofTraverser.traverse(ehProofFolder + "bfs_files/", "[3](0_4_2)[3](1_5_3).html",
                configConsumer, new HashSet<>());

        // unoriented intersecting pair
        EHProofTraverser.traverse(ehProofFolder + "bfs_files/", "[3](0_3_1)[3](2_5_4).html",
                configConsumer, new HashSet<>());

        // --------------------
        // BAD SMALL COMPONENTS
        // --------------------

        // the unoriented interleaving pair
        EHProofTraverser.traverse(
                ehProofFolder + "comb_files/", "[3](0_4_2)[3](1_5_3).html", configConsumer, new HashSet<>());

        // the unoriented necklaces of size 4
        EHProofTraverser.traverse(
                ehProofFolder + "comb_files/", "[3](0_10_2)[3](1_5_3)[3](4_8_6)[3](7_11_9).html",
                configConsumer, new HashSet<>());

        // the twisted necklace of size 4
        EHProofTraverser.traverse(
                ehProofFolder + "comb_files/", "[3](0_7_5)[3](1_11_9)[3](2_6_4)[3](3_10_8).html",
                configConsumer, new HashSet<>());

        // the unoriented necklaces of size 5
        EHProofTraverser.traverse(
                ehProofFolder + "comb_files/",
                "[3](0_4_2)[3](1_14_12)[3](3_7_5)[3](6_10_8)[3](9_13_11).html",
                configConsumer, new HashSet<>());

        // the unoriented necklaces of size 6
        EHProofTraverser.traverse(
                ehProofFolder + "comb_files/",
                "[3](0_16_2)[3](1_5_3)[3](4_8_6)[3](7_11_9)[3](10_14_12)[3](13_17_15).html",
                configConsumer, new HashSet<>());
    }

    private static void print(final PrintStream printer, final Configuration config,
                              final List<Cycle> sorting, final int depth, final boolean alreadyVisited) {
        if (alreadyVisited) {
            printer.println(StringUtils.repeat("\t", depth) + "^" + config.hashCode() + "#" +
                    config.getSpi().toString());
        } else {
            printer.println(StringUtils.repeat("\t", depth) + config.hashCode() + "#" +
                    config.getSpi().toString() + (!sorting.isEmpty() ? "->" + sorting.stream()
                    .map(Cycle::toString).collect(Collectors.joining(";")) : ""));
        }
    }
}
