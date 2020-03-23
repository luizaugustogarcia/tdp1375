package br.unb.cic.tdp.permutation;

import java.io.Serializable;

public interface Permutation extends Serializable {

    Permutation getInverse();

    int getNumberOfEvenCycles();

    int size();

    Cycle asNCycle();

    default Permutation conjugateBy(final Permutation conjugator) {
        return PermutationGroups.computeProduct(false,conjugator, this, conjugator.getInverse());
    }
}
