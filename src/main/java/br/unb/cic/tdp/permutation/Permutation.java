package br.unb.cic.tdp.permutation;

public interface Permutation {

    Permutation getInverse();

    int getNumberOfEvenCycles();

    int size();
}
