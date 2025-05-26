package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.CommonOperations;
import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.RecursiveAction;
import java.util.function.Function;
import java.util.stream.Collectors;

@AllArgsConstructor
@Slf4j
public abstract class AbstractSortOrExtend extends RecursiveAction {
    final Pair<Configuration, Set<Integer>> parent;
    final Pair<Configuration, Set<Integer>> configurationPair;
    final ProofStorage storage;
    final double minRate;

    @Override
    protected void compute() {
        try {
            val canonical = canonicalize(this.configurationPair);

            if (storage.isAlreadySorted(canonical)) {
                return;
            }

            if (!storage.isBadCase(canonical)) {
                if (storage.tryLock(canonical)) {
                    try {
                        if (!storage.markedNoSorting(canonical)) {
                            val sorting = searchForSorting(canonical);
                            val parent = canonicalize(this.parent);
                            if (sorting.isPresent()) {
                                storage.saveSorting(canonical, parent, sorting.get());
                                return;
                            } else {
                                storage.markNoSorting(canonical, parent);
                                storage.markBadCase(canonical);
                            }
                        }
                        extend(this.configurationPair);
                    } finally {
                        storage.unlock(canonical);
                    }
                } // else: another thread is already working on this canonical
            }
        } catch (final Exception e) {
            log.error(e.getMessage());
        }
    }

    protected Optional<List<Cycle>> searchForSorting(final Pair<Configuration, Set<Integer>> configurationPair) {
        val pivots = sortingPivots(configurationPair.getLeft());
        return CommonOperations.searchForSorting(storage, configurationPair.getLeft(), minRate, pivots);
    }

    public static Pair<Configuration, Set<Integer>> configurationPair(final String spi, int... pivots) {
        val pivotSet = new TreeSet<Integer>();
        for (int pivot : pivots) {
            pivotSet.add(pivot);
        }
        return Pair.of(new Configuration(spi), pivotSet);
    }

    protected Pair<Configuration, Set<Integer>> canonicalize(final Pair<Configuration, Set<Integer>> configurationPair) {
        return getCanonical(configurationPair, this::sortingPivots);
    }

    public static Pair<Configuration, Set<Integer>> getCanonical(
            final Pair<Configuration, Set<Integer>> configurationPair,
            final Function<Configuration, Set<Integer>> pivotsFn
    ) {
        val configuration = Configuration.ofSignature(configurationPair.getLeft().getSignature().getContent());
        return canonical(Pair.of(configuration, pivotsFn.apply(configuration)));
    }

    private static Pair<Configuration, Set<Integer>> canonical(final Pair<Configuration, Set<Integer>> configurationPair) {
        var canonical = configurationPair;
        var canonicalStr = configurationPair.toString();

        for (int i = 0; i < configurationPair.getLeft().getSpi().getMaxSymbol(); i++) {
            val rotation = rotate(i, configurationPair.getLeft().getSpi(), configurationPair.getRight());
            if (rotation.toString().compareTo(canonicalStr) < 0) {
                canonical = rotation;
                canonicalStr = rotation.toString();
            }
            val reflection = mirror(rotation.getLeft().getSpi(), rotation.getRight());
            if (reflection.toString().compareTo(canonicalStr) < 0) {
                canonical = reflection;
                canonicalStr = reflection.toString();
            }
        }
        return canonical;
    }

    private static Pair<Configuration, Set<Integer>> rotate(final int i, MulticyclePermutation spi, Set<Integer> pivots) {
        var conjugator = CommonOperations.CANONICAL_PI[spi.getNumberOfSymbols()].getInverse();

        for (int j = 0; j < i; j++) {
            spi = spi.conjugateBy(conjugator);
        }

        for (int j = 0; j < i; j++) {
            pivots = pivots.stream().map(conjugator::image).collect(Collectors.toCollection(TreeSet::new));
        }

        return Pair.of(new Configuration(spi), pivots);
    }

    private static Pair<Configuration, Set<Integer>> mirror(final MulticyclePermutation spi, final Set<Integer> pivots) {
        val pi = CommonOperations.CANONICAL_PI[spi.getNumberOfSymbols()];
        val conjugator = new MulticyclePermutation();
        for (var i = 0; i < pi.size() / 2; i++) {
            conjugator.add(Cycle.of(pi.get(i), pi.get(pi.size() - 1 - i)));
        }
        return Pair.of(new Configuration(spi.conjugateBy(conjugator).getInverse()), pivots.stream()
                .map(conjugator::image)
                .collect(Collectors.toCollection(TreeSet::new)));
    }

    protected abstract Set<Integer> sortingPivots(Configuration configuration);

    protected abstract void extend(Pair<Configuration, Set<Integer>> configurationPair);
}