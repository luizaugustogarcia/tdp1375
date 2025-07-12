package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.base.PivotedConfiguration;
import br.unb.cic.tdp.permutation.Cycle;
import lombok.SneakyThrows;
import lombok.val;

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
        try (val pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors())) {
            val storage = new DerbyProofStorage(outputDir, "extensions");
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
            val standardPivots = super.sortingPivots(pivotedConfiguration.getConfiguration());
            val regularSorting = storage.findSorting(of(pivotedConfiguration.getConfiguration(), standardPivots));
            return regularSorting.or(() -> super.searchForSorting(pivotedConfiguration));
            // TODO clean up visited
        }

        @Override
        public TreeSet<Integer> sortingPivots(final Configuration configuration) {
            return configuration.getSpi().stream()
                    .filter(not(Cycle::isTwoCycle))
                    .map(cycle -> rightMostSymbol(cycle, configuration.getPi()))
                    .collect(Collectors.toCollection(TreeSet::new));
        }

        @Override
        protected void extend(final PivotedConfiguration pivotedConfiguration) {
            val maxSymbol = pivotedConfiguration.getConfiguration().getPi().getMaxSymbol();

            if (maxSymbol > n) {
                n = maxSymbol;
            }

            allExtensions(pivotedConfiguration.getConfiguration())
                    .map(Configuration::getSignature)
                    .distinct()
                    .map(s -> Configuration.ofSignature(s.getContent())) // canonical computation rely on this instantiation from signature
                    .map(extension -> new TwoCyclesSortOrExtend(
                            pivotedConfiguration,
                            of(extension, sortingPivots(extension)),
                            storage,
                            minRate)
                    )
                    .forEach(ForkJoinTask::fork);
        }

        @Override
        protected boolean isValid(final Configuration configuration, final Configuration extension) {
            return extension.isFull();
        }
    }
}