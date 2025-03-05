package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import lombok.AllArgsConstructor;
import lombok.val;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.RecursiveAction;

import static br.unb.cic.tdp.base.CommonOperations.searchForSorting;

@AllArgsConstructor
public abstract class AbstractSortOrExtend extends RecursiveAction {
    final Configuration configuration;
    final ProofStorage storage;
    final double minRate;

    @Override
    protected void compute() {
        if (storage.isAlreadySorted(configuration)) {
            return;
        }

        if (!storage.isBadCase(configuration)) {
            if (storage.tryLock(configuration)) {
                try {
                    val sorting = getSorting();
                    if (sorting.isPresent()) {
                        storage.saveSorting(configuration, sorting.get());
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

    private Optional<List<Cycle>> getSorting() {
        if (storage.hasNoSorting(configuration)) {
            return Optional.empty();
        }
        return searchForSorting(configuration, minRate);
    }

    protected abstract void extend(Configuration configuration);
}