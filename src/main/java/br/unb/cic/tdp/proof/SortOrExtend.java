package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.Configuration;
import lombok.AllArgsConstructor;
import lombok.val;

import java.util.concurrent.RecursiveAction;

import static br.unb.cic.tdp.base.CommonOperations.searchForSorting;

@AllArgsConstructor
public abstract class SortOrExtend extends RecursiveAction {
    final Configuration extendedFrom;
    final Configuration configuration;
    final ProofStorage storage;
    final double minRate;

    @Override
    protected void compute() {
        val canonical = configuration.getCanonical();

        if (storage.isAlreadySorted(canonical)) {
            return;
        }

        if (!storage.isBadCase(canonical)) {
            if (storage.tryLock(canonical)) {
                try {
                    val sorting = searchForSorting(canonical, minRate);
                    if (sorting.isPresent()) {
                        storage.saveSorting(extendedFrom, canonical, sorting.get());
                        return;
                    } else {
                        storage.markBadCase(canonical);
                    }
                    extend(canonical);
                } finally {
                    storage.unlock(canonical);
                }
            } // else: another thread is already working on this configuration
        }
    }

    protected abstract void extend(Configuration configuration);
}