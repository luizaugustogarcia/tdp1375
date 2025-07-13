package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.CommonOperations;
import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.base.PivotedConfiguration;
import br.unb.cic.tdp.permutation.Cycle;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.RecursiveAction;

@AllArgsConstructor
@Slf4j
public abstract class AbstractSortOrExtend extends RecursiveAction {
    final PivotedConfiguration parent;
    final PivotedConfiguration pivotedConfiguration;
    final ProofStorage storage;
    final double minRate;

    @Override
    protected void compute() {
        try {
            val canonical = this.pivotedConfiguration.getCanonical();

            if (storage.isAlreadySorted(canonical)) {
                return;
            }

            if (!storage.isBadCase(canonical)) {
                if (storage.tryLock(canonical)) {
                    try {
                        if (!storage.markedNoSorting(canonical)) {
                            val sorting = searchForSorting(canonical);
                            val parent = this.parent.getCanonical();
                            if (sorting.isPresent()) {
                                storage.saveSorting(canonical, parent, sorting.get());
                                return;
                            } else {
                                storage.markNoSorting(canonical, parent);
                                storage.markBadCase(canonical);
                            }
                        }
                        extend(this.pivotedConfiguration);
                    } finally {
                        storage.unlock(canonical);
                    }
                } // else: another thread is already working on this canonical
            }
        } catch (final Exception e) {
            log.error(e.getMessage());
        }
    }

    protected Optional<List<Cycle>> searchForSorting(final PivotedConfiguration pivotedConfiguration) {
        return CommonOperations.searchForSorting(storage, pivotedConfiguration.getConfiguration(), minRate, pivotedConfiguration.getPivots());
    }

    public abstract TreeSet<Integer> sortingPivots(Configuration configuration);

    protected abstract void extend(PivotedConfiguration pivotedConfiguration);

    protected abstract boolean isValid(final Configuration configuration, final Configuration extension);
}