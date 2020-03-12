package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.UnorientedConfiguration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.proof.eh.desimplification.EHProofTraverser;
import br.unb.cic.tdp.util.Triplet;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public class EHCases {

    public static void main(String[] args) throws FileNotFoundException {
        generate("C:\\Users\\USER-Admin\\workspace\\tdp1375\\sbt1375_proof\\",
                new PrintStream(new File("desimplify-unoriented")));
    }

    public static void generate(final String ehProofFolder, final PrintStream printer) {
        final var processedConfigs = new HashSet<UnorientedConfiguration>();

        final Consumer<Triplet<UnorientedConfiguration, List<Cycle>, Integer>> configConsumer = triplet -> {
            if (!processedConfigs.contains(triplet.first)) {
                print(printer, triplet.first, triplet.second, triplet.third);
            }
            processedConfigs.add(triplet.first);
        };

        // unoriented interleaving pair
        EHProofTraverser.traverse(ehProofFolder + "bfs_files/", "[3](0_4_2)[3](1_5_3).html",
                configConsumer);

        // unoriented intersecting pair
        EHProofTraverser.traverse(ehProofFolder + "bfs_files/", "[3](0_3_1)[3](2_5_4).html",
                configConsumer);

        // --------------------
        // BAD SMALL COMPONENTS
        // --------------------

        // the unoriented interleaving pair
        EHProofTraverser.traverse(
                ehProofFolder + "comb_files/", "[3](0_4_2)[3](1_5_3).html", configConsumer);

        // the unoriented necklaces of size 4
        EHProofTraverser.traverse(
                ehProofFolder + "comb_files/", "[3](0_10_2)[3](1_5_3)[3](4_8_6)[3](7_11_9).html",
                configConsumer);

        // the twisted necklace of size 4
        EHProofTraverser.traverse(
                ehProofFolder + "comb_files/", "[3](0_7_5)[3](1_11_9)[3](2_6_4)[3](3_10_8).html",
                configConsumer);

        // the unoriented necklaces of size 5
        EHProofTraverser.traverse(
                ehProofFolder + "comb_files/",
                "[3](0_4_2)[3](1_14_12)[3](3_7_5)[3](6_10_8)[3](9_13_11).html",
                configConsumer);

        // the unoriented necklaces of size 6
        EHProofTraverser.traverse(
                ehProofFolder + "comb_files/",
                "[3](0_16_2)[3](1_5_3)[3](4_8_6)[3](7_11_9)[3](10_14_12)[3](13_17_15).html",
                configConsumer);
    }

    private static void print(final PrintStream printer, final UnorientedConfiguration config, final List<Cycle> sorting, final int depth) {
        printer.println(StringUtils.repeat("\t", depth) + config.hashCode() + "#" + config.getSpi().toString() + "->" +
                sorting.stream().map(Cycle::toString).collect(Collectors.joining(";")));
    }
}
