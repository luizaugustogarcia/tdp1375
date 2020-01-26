package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.CommonOperations;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;

import java.util.List;

public class Case {

    private byte[] signature;
    private List<byte[]> rhos;
    private MulticyclePermutation spi;
    private Cycle pi;

    public Case(final byte[] pi, final MulticyclePermutation spi, final List<byte[]> rhos) {
        final var cr = CommonOperations.canonicalize(spi, pi, rhos);
        this.pi = new Cycle(cr.getValue1());
        this.spi = cr.getValue0();
        this.rhos = cr.getValue2();

        this.signature = CommonOperations.signature(this.pi.getSymbols(), this.spi);
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
