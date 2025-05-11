package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.CommonOperations;
import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import lombok.AllArgsConstructor;
import lombok.val;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.RecursiveAction;
import java.util.stream.Collectors;

@AllArgsConstructor
public abstract class AbstractSortOrExtend extends RecursiveAction {
    final Configuration parent;
    final Configuration configuration;
    final ProofStorage storage;
    final double minRate;

    @Override
    protected void compute() {
        try {
            val configuration = canonicalize(this.configuration);

            if (storage.isAlreadySorted(configuration)) {
                return;
            }

            if (!storage.isBadCase(configuration)) {
                if (storage.tryLock(configuration)) {
                    try {
                        if (!storage.markedNoSorting(configuration)) {
                            val sorting = searchForSorting(configuration);
                            if (sorting.isPresent()) {
                                storage.saveSorting(configuration, sorting.get());
                                return;
                            } else {
                                System.out.println(configuration.getSpi());
                                storage.markNoSorting(configuration, canonicalize(parent));
                                storage.markBadCase(configuration);
                            }
                        }
                        extend(configuration);
                    } finally {
                        storage.unlock(configuration);
                    }
                } // else: another thread is already working on this configuration
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    protected static Configuration canonicalize(final Configuration configuration) {
        return Configuration.ofSignature(configuration.getSignature().getContent());
    }

    protected Optional<List<Cycle>> searchForSorting(final Configuration configuration) {
        val pivots = getPivots(configuration);
        return CommonOperations.searchForSorting(storage, configuration, minRate, pivots);
    }

    protected abstract Set<Integer> getPivots(Configuration configuration);

    protected abstract void extend(Configuration noSortingConfig);
}