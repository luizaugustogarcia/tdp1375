package br.unb.cic.tdp.proof;

import java.util.List;

import br.unb.cic.tdp.Util;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;

public class Case {

	private int[] cycleSizes;
	private byte[] signature;
	private List<byte[]> rhos;
	private MulticyclePermutation spi;
	private Cycle pi;

	public Case(final byte[] pi, final MulticyclePermutation spi, final List<byte[]> rhos) {
		final var cr = Util.canonicalize(spi, pi, rhos);
		this.pi = new Cycle(cr.getValue1());
		this.spi = cr.getValue0();
		this.rhos = cr.getValue2();

		this.signature = Util.signature(this.pi.getSymbols(), this.spi);
		this.cycleSizes = new int[this.spi.size()];
		for (int i = 0; i < this.pi.size(); i++) {
			int cycleLabel = signature[i] - 1;
			this.cycleSizes[cycleLabel] += 1;
		}
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
		return spi;
	}

	public int getCyclesCount() {
		return spi.size();
	}

	@Override
	public String toString() {
		return "Case [rhos=" + rhos + ", sigmaPiInverse=" + spi + "]";
	}
}
