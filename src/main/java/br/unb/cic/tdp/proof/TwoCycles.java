package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.Collectors;

import static br.unb.cic.tdp.base.CommonOperations.rightMostSymbol;
import static br.unb.cic.tdp.proof.SortOrExtend.configurationPair;
import static java.util.function.Predicate.not;

public class TwoCycles {

    @SneakyThrows
    public static void generate(final String outputDir, final double minRate) {
        try (val pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors())) {
            val storage = new DerbyProofStorage(outputDir, "extensions");
            pool.execute(new TwoCyclesSortOrExtend(configurationPair("(0)", 0), configurationPair("(0 2)(1 3)"), storage, minRate));
            pool.execute(new TwoCyclesSortOrExtend(configurationPair("(0)", 0), configurationPair("(0 2 1)", 0), storage, minRate));
        }
    }

    private static class TwoCyclesSortOrExtend extends SortOrExtend {

        public TwoCyclesSortOrExtend(final Pair<Configuration, Set<Integer>> parent,
                                     final Pair<Configuration, Set<Integer>> configurationPair,
                                     final ProofStorage storage,
                                     final double minRate) {
            super(parent, configurationPair, storage, minRate);
        }

        protected Optional<List<Cycle>> searchForSorting(final Pair<Configuration, Set<Integer>> configurationPair) {
            val standardPivots = super.sortingPivots(configurationPair.getLeft());
            val regularSorting = storage.findSorting(Pair.of(configurationPair.getLeft(), standardPivots));
            return regularSorting.or(() -> super.searchForSorting(configurationPair));
            // TODO clean up visited
        }

        @Override
        public Set<Integer> sortingPivots(final Configuration configuration) {
            return configuration.getSpi().stream()
                    .filter(not(Cycle::isTwoCycle))
                    .map(cycle -> rightMostSymbol(cycle, configuration.getPi()))
                    .collect(Collectors.toSet());
        }

        @Override
        protected void extend(final Pair<Configuration, Set<Integer>> configurationPair) {
            val maxSymbol = configurationPair.getLeft().getPi().getMaxSymbol();

            if (maxSymbol > n) {
                n = maxSymbol;
            }

            allExtensions(configurationPair.getLeft())
                    .map(Configuration::getSignature)
                    .distinct()
                    .map(s -> Configuration.ofSignature(s.getContent())) // canonical computation rely on this instantiation from signature
                    .map(extension -> new TwoCyclesSortOrExtend(
                            configurationPair,
                            Pair.of(extension, sortingPivots(extension)),
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