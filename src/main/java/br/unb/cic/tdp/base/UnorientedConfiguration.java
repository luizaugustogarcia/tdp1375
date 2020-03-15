package br.unb.cic.tdp.base;

import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import cern.colt.list.ByteArrayList;
import com.google.common.primitives.Bytes;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.*;
import java.util.stream.Collectors;

import static br.unb.cic.tdp.base.CommonOperations.CANONICAL_PI;
import static br.unb.cic.tdp.base.CommonOperations.createCycleIndex;

@ToString
public class UnorientedConfiguration {

    @Getter
    private final MulticyclePermutation spi;

    @Getter
    @ToString.Exclude
    private final Cycle pi;

    @Getter
    private final Cycle mirroredPi;

    @Getter
    private final Signature signature;

    @ToString.Exclude
    private Collection<Signature> equivalentSignatures;

    @ToString.Exclude
    private Integer hashCode;

    public UnorientedConfiguration(final MulticyclePermutation spi, final Cycle pi) {
        this.spi = spi;
        this.pi = pi;
        this.mirroredPi = pi.getInverse().conjugateBy(spi).asNCycle();
        this.signature = new Signature(pi, signature(spi, pi), false);
    }

    public static byte[] signature(final List<Cycle> spi, final Cycle pi) {
        final var labelMap = new HashMap<Cycle, Byte>();
        final var cycleIndex = createCycleIndex(spi, pi);

        final var signature = new byte[pi.size()];

        for (var i = 0; i < signature.length; i++) {
            final int symbol = pi.get(i);
            final var cycle = cycleIndex[symbol];
            labelMap.computeIfAbsent(cycle, c -> (byte) (labelMap.size() + 1));
            signature[i] = labelMap.get(cycle);
        }

        return signature;
    }

    public static UnorientedConfiguration fromSignature(byte[] signature) {
        final var pi = CANONICAL_PI[signature.length];
        final var cyclesMap = new HashMap<Byte, List<Byte>>();
        for (int i = signature.length - 1; i >= 0; i--) {
            cyclesMap.computeIfAbsent(signature[i], key -> new ArrayList<>());
            cyclesMap.get(signature[i]).add((byte) i);
        }
        final var spi = cyclesMap.values().stream().map(c -> new Cycle(Bytes.toArray(c)))
                .collect(Collectors.toCollection(MulticyclePermutation::new));
        return new UnorientedConfiguration(spi, pi);
    }

    public Collection<Signature> getEquivalentSignatures() {
        if (equivalentSignatures != null) {
            return equivalentSignatures;
        }

        equivalentSignatures = new HashSet<>();

        for (var i = 0; i < pi.size(); i++) {
            final var shifting = pi.getStartingBy(pi.get(i));
            equivalentSignatures.add(new Signature(shifting, signature(spi, shifting), false));

            final var mirroredShifting = mirroredPi.getStartingBy(mirroredPi.get(i));
            equivalentSignatures.add(new Signature(mirroredShifting, signature(spi.getInverse(), mirroredShifting), true));
        }

        return equivalentSignatures;
    }

    private MulticyclePermutation translate(final byte[] permutation) {
        return spi.stream().map(c -> {
            final var _c = new ByteArrayList(c.size());
            for (final var s : c.getSymbols()) {
                _c.add(pi.get(permutation[s]));
            }
            return new Cycle(_c);
        }).collect(Collectors.toCollection(MulticyclePermutation::new));
    }

    @ToString.Include
    public boolean isFull() {
        for (int i = 0; i < signature.signature.length; i++) {
            if (signature.signature[i] == signature.signature[(i + 1) % signature.signature.length]) {
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
    public int getNumberOfOpenGates() {
        var numberOfOpenGates = 0;
        for (int i = 0; i < signature.signature.length; i++) {
            if (signature.signature[i] == signature.signature[(i + 1) % signature.signature.length]) {
                numberOfOpenGates++;
            }
        }
        return numberOfOpenGates;
    }

/*    public Map<Byte, Byte> getOpenGatesByCycleLabel() {
        final var map = new HashMap<Byte, Byte>();
        for (int i = 0; i < signature.content.length; i++) {
            if (signature.content[i] == signature.content[(i + 1) % signature.content.length]) {
                map.computeIfAbsent(signature.content[i], key -> (byte) 0);
                map.computeIfPresent(signature.content[i], (key, value) -> (byte) (value + 1));
            }
        }
        return map;
    }*/

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
        if (!(obj instanceof UnorientedConfiguration)) {
            return false;
        }

        final var other = (UnorientedConfiguration) obj;

        if (this.signature.signature.length != other.signature.signature.length ||
                this.spi.size() != other.spi.size()) {
            return false;
        }

        return getEquivalentSignatures().contains(((UnorientedConfiguration) obj).signature);
    }
    /*

     */

    /**
     * Returns a configuration with the symbols changed as \sigma=(0,1,2,..,n).
     * Only applicable to full configurations.
     *//*

    public UnorientedConfiguration getNormalConfiguration() {
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
        return new UnorientedConfiguration(spi, pi);
    }
*/

    @AllArgsConstructor
    public class Signature {

        @Getter
        private final Cycle pi;

        @Getter
        private final byte[] signature;

        @Getter
        private final boolean mirror;

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final var other = (Signature) o;
            for (int i = 0; i < other.signature.length; i++) {
                if (signature[i] != other.signature[i]) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(signature);
        }

        @Override
        public String toString() {
            return Arrays.toString(signature);
        }
    }
}