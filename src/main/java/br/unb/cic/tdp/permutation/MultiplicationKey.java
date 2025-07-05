package br.unb.cic.tdp.permutation;

import lombok.val;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MultiplicationKey {
    private final List<RingKey> rings;
    private boolean include1Cycle;
    private int n;

    public MultiplicationKey(final boolean include1Cycle, final int n, final Permutation[] arg) {
        this.include1Cycle = include1Cycle;
        this.n = n;
        rings = new ArrayList<>(arg.length);
        for (Permutation a : arg) {
            if (a instanceof Cycle c) {
                rings.add(new RingKey(c.getSymbols()));
            } else if (a instanceof MulticyclePermutation mc) {
                for (val c : mc) {
                    rings.add(new RingKey(c.getSymbols()));
                }
            }
        }
    }

    @Override
    public boolean equals(final Object o) {
        return o instanceof MultiplicationKey mk &&
                include1Cycle == mk.include1Cycle &&
                n == mk.n &&
                rings.equals(mk.rings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(include1Cycle, n, rings);
    }
}