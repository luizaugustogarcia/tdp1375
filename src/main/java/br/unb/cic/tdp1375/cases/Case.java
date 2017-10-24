package br.unb.cic.tdp1375.cases;

import java.util.List;

import br.unb.cic.tdp1375.permutations.Cycle;
import br.unb.cic.tdp1375.util.Util;

public class Case {

	private int[] cycleSizes;
	private byte[] signature;
	private List<byte[]> rhos;
	private List<Cycle> sigmaPiInverse;
	private Cycle pi;

	public Case(byte[] pi, List<Cycle> sigmaPiInverse, List<byte[]> rhos) {
		signature = Util.signature(pi, sigmaPiInverse);
		cycleSizes = new int[sigmaPiInverse.size()];
		for (int i = 0; i < pi.length; i++) {
			int cycleLabel = signature[i] - 1;
			cycleSizes[cycleLabel] += 1;
		}

		this.pi = new Cycle(pi);
		this.sigmaPiInverse = sigmaPiInverse;
		this.rhos = rhos;
	}

	public Cycle getPi() {
		return pi;
	}

	public List<byte[]> getRhos() {
		return rhos;
	}

	public byte[] getSignature() {
		return signature;
	}

	public int[] getCycleSizes() {
		return cycleSizes;
	}

	public List<Cycle> getSigmaPiInverse() {
		return sigmaPiInverse;
	}

	public int getCyclesCount() {
		return sigmaPiInverse.size();
	}

	@Override
	public String toString() {
		return "Case [rhos=" + rhos + ", sigmaPiInverse=" + sigmaPiInverse + "]";
	}
}
