package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.CommonOperations;
import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import lombok.AllArgsConstructor;
import lombok.val;

import java.util.*;
import java.util.concurrent.RecursiveAction;
import java.util.stream.Collectors;

@AllArgsConstructor
public abstract class AbstractSortOrExtend extends RecursiveAction {
    final Configuration configuration;
    final ProofStorage storage;
    final double minRate;

    @Override
    protected void compute() {
        val configuration = Configuration.ofSignature(this.configuration.getSignature().getContent());

        if (storage.isAlreadySorted(configuration)) {
            return;
        }

        if (!storage.isBadCase(configuration)) {
            if (storage.tryLock(configuration)) {
                try {
                    val sorting = searchForSorting(configuration);
                    if (sorting.isPresent()) {
                        storage.saveSorting(configuration, Set.of(), sorting.get());
                        return;
                    } else {
                        storage.markNoSorting(configuration, null); // TODO
                        storage.markBadCase(configuration);
                    }
                    extend(configuration);
                } finally {
                    storage.unlock(configuration);
                }
            } // else: another thread is already working on this configuration
        }
    }

    protected Optional<List<Cycle>> searchForSorting(Configuration configuration) {

        if (storage.hasNoSorting(configuration)) {
            return Optional.empty();
        }
        val pivots = configuration.getSpi().stream()
                .map(Cycle::getMinSymbol)
                .collect(Collectors.toSet());
        return CommonOperations.searchForSorting(storage, configuration, minRate, pivots);
    }

    protected abstract void extend(Configuration configuration);
}