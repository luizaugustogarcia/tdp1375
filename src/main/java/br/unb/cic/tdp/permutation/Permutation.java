package br.unb.cic.tdp.permutation;

import java.util.List;

public interface Permutation {

    Permutation getInverse();

    List<Cycle> default2CycleFactorization();

    int getNumberOfEvenCycles();

    int size();
}
