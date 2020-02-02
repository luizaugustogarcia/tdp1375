package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;

import java.util.List;

import static br.unb.cic.tdp.CommonOperations.canonicalize;
import static br.unb.cic.tdp.CommonOperations.signature;

public class Case {

    private byte[] signature;
    private List<Cycle> rhos;
    private MulticyclePermutation spi;
    private Cycle pi;

    public Case(final Cycle pi, final MulticyclePermutation spi, final List<Cycle> rhos) {
        final var cr = canonicalize(spi, pi, rhos);
        this.spi = cr.first;
        this.pi = cr.second;
        this.rhos = cr.third;

        this.signature = signature(this.spi, this.pi);
    }

    public Cycle getPi() {
        return pi;
    }

    public List<Cycle> getRhos() {
        return rhos;
    }

    public byte[] getSignature() {
        return signature;
    }

    public MulticyclePermutation getSpi() {
        return spi;
    }

    public int getCyclesCount() {
        return spi.size();
    }

    @Override
    public String toString() {
        return "Case [rhos=" + rhos + ", spi=" + spi + "]";
    }
}
