package br.unb.cic.tdp;

import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static br.unb.cic.tdp.CommonOperations.CANONICAL_PI;
import static br.unb.cic.tdp.CommonOperations.signature;
import static br.unb.cic.tdp.permutation.PermutationGroups.computeProduct;

@ToString
public class Configuration {

    @Getter
    @ToString.Exclude
    private final Cycle pi;

    @Getter
    private final MulticyclePermutation spi;

    @Getter
    private final Signature signature;

    @ToString.Exclude
    private Set<Signature> signatures;

    @ToString.Exclude
    private Integer hashCode;

    public Configuration(final MulticyclePermutation spi, final Cycle pi) {
        this.spi = spi;
        this.pi = pi;
        this.signature = new Signature(signature(spi, pi));
    }

    private Set<Signature> getSignatures() {
        if (signatures != null) {
            return signatures;
        }

        signatures = new HashSet<>();

        // shifting
        for (var i = 0; i < pi.size(); i++) {
            signatures.add(new Signature(
                    signature(spi, pi.getStartingBy(pi.get(i)))));
        }

        // mirroring
        for (var i = 0; i < pi.getInverse().size(); i++) {
            signatures.add(new Signature(
                    signature(spi, pi.getInverse().getStartingBy(pi.getInverse().get(i)))));
        }

        return signatures;
    }

    @ToString.Include
    public boolean isFull() {
        for (int i = 0; i < signature.content.length; i++) {
            if (signature.content[i] == signature.content[(i + 1) % signature.content.length]) {
                return false;
            }
        }
        return true;
    }

    @ToString.Include
    public int numberOfOpenGates() {
        var numberOfOpenGates = 0;
        for (int i = 0; i < signature.content.length; i++) {
            if (signature.content[i] == signature.content[(i + 1) % signature.content.length]) {
                numberOfOpenGates++;
            }
        }
        return numberOfOpenGates;
    }

    @Override
    @ToString.Include
    public int hashCode() {
        if (this.hashCode == null) {
            this.hashCode = getSignatures().stream().mapToInt(Signature::hashCode).min().getAsInt();
        }
        return this.hashCode;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof Configuration)) {
            return false;
        }

        final var other = (Configuration) obj;

        if (this.signature.content.length != other.signature.content.length || this.spi.size() != other.spi.size()) {
            return false;
        }

        return getSignatures().contains(((Configuration) obj).signature);
    }

    /**
     * Returns a configuration with the symbols changed as \sigma were (0,1,2,..,n).
     * Only works with full configurations.
     */
    public Configuration getNormalConfiguration() {
        if (!isFull()) {
            return null;
        }

        final var _sigma = computeProduct(spi, pi).asNCycle();
        final var _pi = new byte[_sigma.size()];
        for (int i = 0; i < _pi.length; i++) {
            _pi[_sigma.get(i)] = (byte) i;
        }

        final var pi = new Cycle(_pi);
        final var sigma = CANONICAL_PI[pi.size()];
        final var spi = computeProduct(sigma, pi.getInverse());
        return new Configuration(spi, pi);
    }

    @AllArgsConstructor
    private static class Signature {

        @Getter
        private byte[] content;

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final var other = (Signature) o;
            for (int i = 0; i < other.content.length; i++) {
                if (content[i] != other.content[i]) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(content);
        }
    }
}
