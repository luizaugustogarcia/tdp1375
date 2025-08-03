package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.base.PivotedConfiguration;
import br.unb.cic.tdp.permutation.Cycle;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.Collectors;

import static br.unb.cic.tdp.base.CommonOperations.rightMostSymbol;
import static br.unb.cic.tdp.base.PivotedConfiguration.of;
import static java.util.function.Predicate.not;

public class TwoCycles {

    @SneakyThrows
    public static void generate(final String outputDir, final double minRate) {
        val storage = new DerbyProofStorage(outputDir, "two_cycles_");

        val parallelism = Integer.parseInt(System.getProperty("java.util.concurrent.ForkJoinPool.common.parallelism",
                "%d".formatted(Runtime.getRuntime().availableProcessors())));

        try (val pool = new ForkJoinPool(parallelism)) {
            pool.execute(new TwoCyclesSortOrExtend(of("(0)", 0), of("(0 2)(1 3)"), storage, minRate));
            pool.execute(new TwoCyclesSortOrExtend(of("(0)", 0), of("(0 2 1)", 0), storage, minRate));
        }
    }

    private static class TwoCyclesSortOrExtend extends SortOrExtend {

        public TwoCyclesSortOrExtend(final PivotedConfiguration parent,
                                     final PivotedConfiguration pivotedConfiguration,
                                     final ProofStorage storage,
                                     final double minRate) {
            super(parent, pivotedConfiguration, storage, minRate);
        }

        protected Optional<List<Cycle>> searchForSorting(final PivotedConfiguration pivotedConfiguration) {
            // check whether there is a regular sorting (with 2-cycles regarded as segments of longer cycles)
            val standardPivots = super.sortingPivots(pivotedConfiguration.getConfiguration());
            val regularSorting = storage.findSorting(of(pivotedConfiguration.getConfiguration(), standardPivots));
            // else, look for a sorting with 2-cycles not contributing with pivots
            return regularSorting.or(() -> super.searchForSorting(pivotedConfiguration));
        }

        @Override
        public TreeSet<Integer> sortingPivots(final Configuration configuration) {
            return configuration.getSpi().stream()
                    .filter(not(Cycle::isTwoCycle))
                    .map(cycle -> rightMostSymbol(cycle, configuration.getPi()))
                    .collect(Collectors.toCollection(TreeSet::new));
        }

        public List<Configuration> allExtensions(final Configuration configuration) {
            return concatStreams(
                    type1Extensions(configuration).map(Pair::getRight),
                    type2Extensions(configuration).map(Pair::getRight),
                    type3Extensions(configuration).map(Pair::getRight),
                    type4Extensions(configuration).map(Pair::getRight),
                    type5Extensions(configuration).map(Pair::getRight),
                    type6Extensions(configuration).map(Pair::getRight),
                    type7Extensions(configuration).map(Pair::getRight),
                    type8Extensions(configuration).map(Pair::getRight),
                    type9Extensions(configuration).map(Pair::getRight)
            ).toList();
        }

        @Override
        protected void extend(final PivotedConfiguration pivotedConfiguration) {
            allExtensions(pivotedConfiguration.getConfiguration())
                    .stream()
                    .map(Configuration::getSignature)
                    .distinct()
                    .map(s -> Configuration.ofSignature(s.getContent())) // canonical computation rely on this instantiation from signature
                    .map(extension -> new TwoCyclesSortOrExtend(
                            pivotedConfiguration,
                            of(extension, sortingPivots(extension)), // 2-cycles not contributing with pivots
                            storage,
                            minRate)
                    )
                    .forEach(ForkJoinTask::fork);
        }

        @Override
        protected boolean isValid(final Configuration configuration, final Configuration extension) {
            return extension.isFull() && onlyOneComponent(extension);
        }
    }
}