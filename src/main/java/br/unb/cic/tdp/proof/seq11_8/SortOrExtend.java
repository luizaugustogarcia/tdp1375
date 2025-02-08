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
        if (!isValidExtension.test(configuration) || storage.isAlreadySorted(configuration)) {
            return;
        }

        if (!storage.isBadCase(configuration)) {
            if (storage.tryLock(configuration)) {
                try {
                    val sorting = searchForSorting(configuration);
                    if (sorting.isPresent()) {
                        storage.saveSorting(extendedFrom, configuration, sorting.get());
                        return;
                    } else {
                        storage.markBadCase(configuration);
                    }
                } finally {
                    storage.unlock(configuration);
                }
            }
        }

        if (!shouldStop.test(configuration)) {
            extend(configuration);
        }
    }

    protected abstract void extend(Configuration configuration);
}