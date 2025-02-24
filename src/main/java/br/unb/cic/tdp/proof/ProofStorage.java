package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;

import java.util.List;

public interface ProofStorage {

    boolean isAlreadySorted(Configuration configuration);

    boolean isBadCase(Configuration configuration);

    boolean tryLock(Configuration configuration);

    void unlock(Configuration configuration);

    void markBadCase(Configuration configuration);

    void saveSorting(Configuration configuration, List<Cycle> sorting);
}
