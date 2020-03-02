package br.unb.cic.tdp;

import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import com.google.common.primitives.Bytes;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.*;
import java.util.stream.Collectors;

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
    private Set<Signature> equivalentSignatures;

    @ToString.Exclude
    private Integer hashCode;

    public Configuration(final MulticyclePermutation spi, final Cycle pi) {
        this.spi = spi;
        this.pi = pi;
        this.signature = new Signature(signature(spi, pi));
    }

    public static Configuration fromSignature(byte[] signature) {
        final var pi = CANONICAL_PI[signature.length];
        final var cyclesMap = new HashMap<Byte, List<Byte>>();
        for (int i = signature.length - 1; i >= 0; i--) {
            cyclesMap.computeIfAbsent(signature[i], key -> new ArrayList<>());
            cyclesMap.get(signature[i]).add((byte) i);
        }
        final var spi = cyclesMap.values().stream().map(c -> new Cycle(Bytes.toArray(c)))
                .collect(Collectors.toCollection(MulticyclePermutation::new));
        return new Configuration(spi, pi);
    }

    private Set<Signature> getEquivalentSignatures() {
        if (equivalentSignatures != null) {
            return equivalentSignatures;
        }

        equivalentSignatures = new HashSet<>();

        // shifting
        for (var i = 0; i < pi.size(); i++) {
            equivalentSignatures.add(new Signature(
                    signature(spi, pi.getStartingBy(pi.get(i)))));
        }

        // mirroring
        for (var i = 0; i < pi.getInverse().size(); i++) {
            equivalentSignatures.add(new Signature(
                    signature(spi, pi.getInverse().getStartingBy(pi.getInverse().get(i)))));
        }

        return equivalentSignatures;
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
    public int get3Norm() {
        return this.spi.get3Norm();
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

    public Map<Byte, Byte> getOpenGatesByCycleLabel() {
        final var map = new HashMap<Byte, Byte>();
        for (int i = 0; i < signature.content.length; i++) {
            if (signature.content[i] == signature.content[(i + 1) % signature.content.length]) {
                map.computeIfAbsent(signature.content[i], key -> (byte) 0);
                map.computeIfPresent(signature.content[i], (key, value) -> (byte) (value + 1));
            }
        }
        return map;
    }

    @Override
    @ToString.Include
    public int hashCode() {
        if (this.hashCode == null) {
            this.hashCode = getEquivalentSignatures().stream().mapToInt(Signature::hashCode).min().getAsInt();
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

        return getEquivalentSignatures().contains(((Configuration) obj).signature);
    }

    /**
     * Returns a configuration with the symbols changed as \sigma=(0,1,2,..,n).
     * Only applicable to full configurations.
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
    public static class Signature {

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

        @Override
        public String toString() {
            return Arrays.toString(content);
        }
    }
}
