package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.CommonOperations;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import cern.colt.list.ByteArrayList;

import java.util.HashMap;
import java.util.TreeSet;

class Configuration {

    private byte[] pi;

    private MulticyclePermutation spi;

    private String cyclicSignature;

    Configuration(final byte[] pi, final MulticyclePermutation spi) {
        this.pi = pi;
        this.spi = spi;
    }

    Configuration(final Cycle pi, final MulticyclePermutation spi) {
        this.pi = pi.getSymbols();
        this.spi = spi;
    }

    private String cyclicSignature() {
        if (cyclicSignature != null) {
            return cyclicSignature;
        }

        final var symbolToCycles = CommonOperations.mapSymbolsToCycles(spi, pi);

        final var representations = new TreeSet<String>();

        for (var i = 0; i < pi.length; i++) {
            // pi rotation
            final var _pi = rotate(pi, i);

            final var representation = new ByteArrayList(_pi.length);
            final var labels = new HashMap<Cycle, Byte>();

            for (byte b : _pi) {
                final var label = labels.computeIfAbsent(symbolToCycles[b], cycle -> (byte) labels.size());
                representation.add(label);
            }

            representations.add(representation.toString());
        }

        return cyclicSignature = String.join("\n", representations);
    }

    private byte[] rotate(final byte[] array, final int distance) {
        final var rotation = new byte[array.length];
        System.arraycopy(array, 0, rotation, distance, array.length - distance);
        System.arraycopy(array, array.length - distance, rotation, 0, distance);
        return rotation;
    }

    @Override
    public int hashCode() {
        return cyclicSignature().hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        final var other = (Configuration) obj;

        if (this.pi.length != other.pi.length || this.spi.size() != other.spi.size()
                || !this.spi.isSameCycleType(other.spi)) {
            return false;
        }

        // Two configurations are equal when, rotating pi, they produce the same
        // configurations. The algorithm to generate the 'cyclic signature' is:
        // 1. for each rotation of pi
        // 1.2. rewrite spi changing the symbols according to the rotation
        // 1.3. add the string representation of spi to a set
        // 2. sort the set of representations and convert it into a string
        return cyclicSignature().equals(other.cyclicSignature());
    }
}