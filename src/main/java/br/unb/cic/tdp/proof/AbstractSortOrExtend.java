package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import lombok.AllArgsConstructor;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.RecursiveAction;

import static br.unb.cic.tdp.base.CommonOperations.searchForSorting;

@AllArgsConstructor
public abstract class AbstractSortOrExtend extends RecursiveAction {
    final Configuration configuration;
    final ProofStorage storage;
    final double minRate;

    @Override
    protected void compute() {
        val configuration = this.configuration.getCanonical();
   
        if (storage.isAlreadySorted(configuration)) {
            return;
        }

        if (!storage.isBadCase(configuration)) {
            if (storage.tryLock(configuration)) {
                try {
                    val sorting = getSorting(configuration);
                    if (sorting.isPresent()) {
                        sorting.get().forEach(pair -> {
                            storage.saveSorting(configuration, pair.getKey(), pair.getValue());
                        });
                        return;
                    } else {
                        storage.noSorting(configuration);
                        storage.markBadCase(configuration);
                    }
                    extend(configuration);
                } finally {
                    storage.unlock(configuration);
                }
            } // else: another thread is already working on this configuration
        }
    }

    private Optional<List<Pair<Set<Integer>, List<Cycle>>>> getSorting(final Configuration configuration) {
        if (storage.hasNoSorting(configuration)) {
            return Optional.empty();
        }
        return searchForSorting(storage, configuration, minRate);
    }

    protected abstract void extend(Configuration configuration);
}