package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.Configuration;
import br.unb.cic.tdp.permutation.Cycle;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static br.unb.cic.tdp.CommonOperations.*;

public class DesimplifyOriented extends EHProofTraverser {

    public static void generate(final String inputDir, final PrintStream printer) {
        final var verifiedConfigurations = new HashSet<Configuration>();

        final var visitedFiles = new HashSet<String>();

        final var desimplify = new DesimplifyOriented();

        // unoriented interleaving pair
        desimplify.traverse(inputDir + "bfs_files/", "[3](0_4_2)[3](1_5_3).html",
                visitedFiles, verifiedConfigurations, printer, 0);

        // --------------------
        // BAD SMALL COMPONENTS
        // --------------------

        // the unoriented interleaving pair
        desimplify.traverse(inputDir + "comb_files/", "[3](0_4_2)[3](1_5_3).html",
                visitedFiles, verifiedConfigurations, printer, 0);
    }

    protected void desimplify(final Configuration configuration, final List<Cycle> rhos,
                              final Set<Configuration> verifiedConfigurations, final PrintStream printer, final int depth) {
        for (final var combination : combinations(configuration.getSpi(), 2)) {
            for (final var joinPair : getJoinPairs(combination.getVector(), configuration.getPi())) {
                final var join = join(configuration.getSpi(), configuration.getPi(), rhos, joinPair);

                final var cr = canonicalize(join.first, join.second, join.third);
                final var _spi = cr.first;
                final var _pi = cr.second;
                final var _rhos = cr.third;

                // Skipping configuration containing cycles > 5, since all oriented 7-cycle accept (4,3)-sequence
                final var isThereOrientedCycleGreaterThan5 = _spi.stream().anyMatch(cycle -> cycle.size() > 5 &&
                        isOriented(configuration.getPi(), cycle));
                // Skipping configurations not containing an oriented 5-cycle
                final var isThereOriented5Cycle = _spi.stream().anyMatch(cycle -> cycle.size() == 5 &&
                        isOriented(configuration.getPi(), cycle));
                // Skipping configurations containing 2-moves
                final var isThereOriented3Segment = searchFor2MoveFromOrientedCycle(_spi, _pi) != null;

                if (!isThereOrientedCycleGreaterThan5 && isThereOriented5Cycle && !isThereOriented3Segment) {
                    if (is11_8(_spi, _pi, _rhos)) {
                        final var _configuration = new Configuration(_spi, _pi);
                        if (!verifiedConfigurations.contains(_configuration)) {
                            verifiedConfigurations.add(_configuration);
                            printCase(_configuration, _rhos, printer, depth);
                            desimplify(_configuration, _rhos, verifiedConfigurations, printer, depth + 1);
                        }
                    } else {
                        throw new RuntimeException("ERROR");
                    }
                }
            }
        }
    }
}
