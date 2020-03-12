package br.unb.cic.tdp.permutation;

public interface Permutation {

    Permutation getInverse();

    int getNumberOfEvenCycles();

    int size();

    Cycle asNCycle();

    default Permutation conjugateBy(final Permutation conjugator) {
        return PermutationGroups.computeProduct(false,conjugator, this, conjugator.getInverse());
    }
}
