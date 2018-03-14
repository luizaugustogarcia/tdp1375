package br.unb.cic.tdp.permutation;

import java.util.List;

public interface Permutation {

	public Permutation getInverse();

	public List<Cycle> default2CycleFactorization();

	public int getNumberOfEvenCycles();

	public int getNumberOfEvenCycles(int n);

	public int size();
}
