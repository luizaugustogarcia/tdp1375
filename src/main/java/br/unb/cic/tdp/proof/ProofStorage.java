package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Set;

public interface ProofStorage {

    boolean isAlreadySorted(Configuration configuration);

    boolean isBadCase(Configuration configuration);

    boolean tryLock(Configuration configuration);

    void unlock(Configuration configuration);

    void markBadCase(Configuration configuration);

    void saveSorting(final Configuration configuration, Set<Integer> pivots, List<Cycle> sorting);

    void markNoSorting(Configuration configuration);

    boolean hasNoSorting(Configuration configuration);

    void saveComponentSorting(Configuration configuration, List<Cycle> cycles);

    List<Pair<Configuration, Pair<Set<Integer>, List<Cycle>>>> findBySorting(String spi);
}
