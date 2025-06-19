package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.Collectors;

import static br.unb.cic.tdp.base.CommonOperations.rightMostSymbol;
import static br.unb.cic.tdp.proof.SortOrExtend.configurationPair;
import static br.unb.cic.tdp.proof.SortOrExtend.type1Extensions;
import static java.util.function.Predicate.not;

public class TwoCycles {

    @SneakyThrows
    public static void generate(final String outputDir, final double minRate) {
        try (val pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors())) {
            val storage = new DerbyProofStorage(outputDir, "2cycles");
            pool.execute(new TwoCyclesSortOrExtend(configurationPair("(0)", 0), configurationPair("(0 2)(1 3)"), storage, minRate));
            pool.execute(new TwoCyclesSortOrExtend(configurationPair("(0)", 0), configurationPair("(0 2)(1 4 3)", 4), storage, minRate));
        }
    }

    private static class TwoCyclesSortOrExtend extends AbstractSortOrExtend {

        public TwoCyclesSortOrExtend(final Pair<Configuration, Set<Integer>> parent,
                                     final Pair<Configuration, Set<Integer>> configurationPair,
                                     final ProofStorage storage,
                                     final double minRate) {
            super(parent, configurationPair, storage, minRate);
        }

        @Override
        protected Set<Integer> sortingPivots(final Configuration configuration) {
            return configuration.getSpi().stream()
                    .filter(not(Cycle::isTwoCycle))
                    .map(cycle -> rightMostSymbol(cycle, configuration.getPi()))
                    .collect(Collectors.toSet());
        }

        @Override
        protected void extend(final Pair<Configuration, Set<Integer>> configurationPair) {
            val noSortingConfig = configurationPair.getLeft();

            val num2Cycles = noSortingConfig.getSpi().stream().filter(Cycle::isTwoCycle).count();
            type1Extensions(noSortingConfig).stream()
                    .map(Pair::getRight)
                    .filter(extension -> extension.openGates().count() <= 1)
                    .filter(extension -> extension.getSpi().stream().filter(Cycle::isTwoCycle).count() >= num2Cycles)
                    .map(extension -> new TwoCyclesSortOrExtend(configurationPair, Pair.of(extension, sortingPivots(extension)), storage, minRate))
                    .forEach(ForkJoinTask::fork);
        }
    }
}