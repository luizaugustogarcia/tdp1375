package br.unb.cic.tdp.permutation;

import java.io.Serializable;

public interface Permutation extends Serializable {

    Permutation getInverse();

    int getNumberOfEvenCycles();

    int size();

    Cycle asNCycle();

    default MulticyclePermutation conjugateBy(final Permutation conjugator) {
        return PermutationGroups.computeProduct(true, conjugator, this, conjugator.getInverse());
    }

    default MulticyclePermutation times(final Permutation rightOperand) {
        return PermutationGroups.computeProduct(true, this, rightOperand);
    }

    boolean contains(int i);

    int image(int a);

    int getMaxSymbol();
}
