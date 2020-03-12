package br.unb.cic.tdp.proof.eh.desimplification;

public class DesimplifyOrientedConfigs {
/*

    public static void main(String[] args) throws FileNotFoundException {
        generate("C:\\Users\\USER-Admin\\workspace\\tdp1375\\sbt1375_proof\\",
                new PrintStream(new File("desimplify-oriented")));
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

        // --------------------
        // BAD SMALL COMPONENTS
        // --------------------

        // the unoriented interleaving pair
        EHProofTraverser.traverse(ehProofFolder + "comb_files/", "[3](0_4_2)[3](1_5_3).html",
                configConsumer);
    }

    private static void desimplify(final OrientedConfiguration configuration, final List<Cycle> sorting,
                                   final Set<OrientedConfiguration> processedConfigs, final PrintStream printer) {
        for (final var combination : combinations(configuration.getSpi(), 2)) {
            for (final var joinPair : getJoinPairs(combination.getVector(), configuration.getPi())) {
                final var join = join(configuration.getSpi(), configuration.getPi(), sorting, joinPair);

                final var cr = canonicalize(join.first, join.second, join.third);
                final var _spi = cr.first;
                final var _pi = cr.second;
                final var _sorting = cr.third;

                // Skipping configuration containing cycles > 5, since all oriented 7-cycle accept (4,3)-sequence
                final var isThereOrientedCycleGreaterThan5 = _spi.stream().anyMatch(cycle -> cycle.size() > 5 &&
                        isOriented(configuration.getPi(), cycle));
                // Skipping configurations not containing an oriented 5-cycle
                final var isThereOriented5Cycle = _spi.stream().anyMatch(cycle -> cycle.size() == 5 &&
                        isOriented(configuration.getPi(), cycle));
                // Skipping configurations containing 2-moves
                final var isThereOriented3Segment = searchFor2MoveFromOrientedCycle(_spi, _pi) != null;

                if (!isThereOrientedCycleGreaterThan5 && isThereOriented5Cycle && !isThereOriented3Segment) {
                    if (is11_8(_spi, _pi, _sorting)) {
                        final var _config = new OrientedConfiguration(_spi, _pi);
                        if (!processedConfigs.contains(_config)) {
                            processedConfigs.add(_config);
                            print(printer, _config, _sorting);
                            desimplify(_config, _sorting, processedConfigs, printer);
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
