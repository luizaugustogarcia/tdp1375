package br.unb.cic.tdp.proof.seq11_8;

import br.unb.cic.tdp.base.Configuration;
import lombok.AllArgsConstructor;
import lombok.val;

import java.util.concurrent.RecursiveAction;
import java.util.function.Predicate;

import static br.unb.cic.tdp.proof.ProofGenerator.searchForSorting;

@AllArgsConstructor
public abstract class SortOrExtend extends RecursiveAction {
    final Configuration extendedFrom;
    final Configuration configuration;
    final Predicate<Configuration> shouldStop;
    final Predicate<Configuration> isValidExtension;
    final ProofStorage storage;

    @Override
    protected void compute() {
        val canonical = configuration.getCanonical();

        if (!isValidExtension.test(canonical) || storage.isAlreadySorted(canonical)) {
            return;
        }

        if (!storage.isBadCase(canonical)) {
            if (storage.tryLock(canonical)) {
                try {
                    val sorting = searchForSorting(canonical);
                    if (sorting.isPresent()) {
                        storage.saveSorting(extendedFrom, canonical, sorting.get());
                        return;
                    } else {
                        storage.markBadCase(canonical);
                    }
                } finally {
                    storage.unlock(canonical);
                }
            }
        }

        if (!shouldStop.test(canonical)) {
            extend(canonical);
        }
    }

    protected abstract void extend(Configuration configuration);
}