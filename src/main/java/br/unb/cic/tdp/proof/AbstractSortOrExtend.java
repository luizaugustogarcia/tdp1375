package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.CommonOperations;
import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.RecursiveAction;
import java.util.logging.Logger;

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
                            val parent = Pair.of(Configuration.ofSignature(this.parent.getLeft().getSignature().getContent()), getStandardPivots(this.parent.getLeft()));
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
        val pivots = getStandardPivots(configurationPair.getLeft());
        return CommonOperations.searchForSorting(storage, configurationPair.getLeft(), minRate, pivots);
    }

    public static Pair<Configuration, Set<Integer>> configurationPair(final String spi, int... pivots) {
        val pivotSet = new TreeSet<Integer>();
        for (int pivot : pivots) {
            pivotSet.add(pivot);
        }
        return Pair.of(new Configuration(spi), pivotSet);
    }

    protected abstract Pair<Configuration, Set<Integer>> canonicalize(final Pair<Configuration, Set<Integer>> configurationPair);

    protected abstract Set<Integer> getStandardPivots(Configuration configuration);

    protected abstract void extend(Pair<Configuration, Set<Integer>> configurationPair);
}