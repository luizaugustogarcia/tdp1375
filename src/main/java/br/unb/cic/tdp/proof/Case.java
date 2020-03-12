package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

import static br.unb.cic.tdp.base.CommonOperations.canonicalize;

@Getter
@ToString
public class Case {

    @ToString.Exclude
    private Cycle pi;

    private byte[] signature;

    private List<Cycle> rhos;

    private MulticyclePermutation spi;

    public Case(final MulticyclePermutation spi, final Cycle pi, final List<Cycle> rhos) {
        final var cr = canonicalize(spi, pi, rhos);
        this.spi = cr.first;
        this.pi = cr.second;
        this.rhos = cr.third;

        //this.signature = signature(this.spi, this.pi);
    }

    @ToString.Include
    public int getCyclesCount() {
        return spi.size();
    }
}
