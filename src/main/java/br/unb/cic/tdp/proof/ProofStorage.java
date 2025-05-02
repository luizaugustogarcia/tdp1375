package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ProofStorage {

    boolean isAlreadySorted(Configuration configuration);

    boolean isBadCase(Configuration configuration);

    boolean tryLock(Configuration configuration);

    void unlock(Configuration configuration);

    void markBadCase(Configuration configuration);

    void saveSorting(Configuration configuration, Set<Integer> pivots, List<Cycle> sorting);

    void markNoSorting(Configuration configuration, Configuration parent);

    boolean hasNoSorting(Configuration configuration);

    void saveComponentSorting(Configuration configuration, List<Cycle> cycles);

    Optional<List<Cycle>> findSorting(String spi);

    Optional<List<Cycle>> findCompSorting(String spi);
}
