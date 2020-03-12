package br.unb.cic.tdp.proof.eh.desimplification;

public class DesimplifyUnorientedConfigs {
/*

    public static void main(String[] args) throws FileNotFoundException {
        generate("C:\\Users\\USER-Admin\\workspace\\tdp1375\\sbt1375_proof\\",
                new PrintStream(new File("desimplify-unoriented")));
    }

    public static void generate(final String ehProofFolder, final PrintStream printer) {
        final var processedConfigs = new HashSet<OrientedConfiguration>();

        final Consumer<Triplet<OrientedConfiguration, List<Cycle>, Integer>> configConsumer = triplet -> {
            if (!processedConfigs.contains(triplet.first) && !triplet.second.isEmpty()) {
                print(printer, triplet.first, triplet.second);
                desimplify(triplet.first, triplet.second, processedConfigs, printer);
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

    private static void desimplify(final OrientedConfiguration configuration, final List<Cycle> rhos,
                                   final Set<OrientedConfiguration> verifiedConfigurations, final PrintStream printer) {
        for (final var combination : combinations(configuration.getSpi(), 2)) {
            // only join cycles which are not intersecting
            if (areNotIntersecting(combination.getVector(), configuration.getPi())) {
                final var joiningPairs = getJoinPairs(combination.getVector(), configuration.getPi());

                for (final var joinPair : joiningPairs) {
                    final var join = join(configuration.getSpi(), configuration.getPi(), rhos, joinPair);

                    final var cr = canonicalize(join.first, join.second, join.third);
                    final var _spi = cr.first;
                    final var _pi = cr.second;
                    final var _sorting = cr.third;

                    if (is11_8(_spi, _pi, _sorting)) {
                        final var _config = new OrientedConfiguration(_spi, _pi);

                        if (!verifiedConfigurations.contains(_config)) {
                            verifiedConfigurations.add(_config);
                            print(printer, _config, _sorting);
                            desimplify(_config, _sorting, verifiedConfigurations, printer);
                        }
                    } else {
                        throw new RuntimeException("ERROR");
                    }
                }
            }
        }
    }

    private static void print(final PrintStream printer, final OrientedConfiguration config, final List<Cycle> sorting) {
        printer.println(config.hashCode() + "#" + config.getSpi().toString() + "->" +
                sorting.stream().map(Cycle::toString).collect(Collectors.joining(";")));
    }
*/
}
