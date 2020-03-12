package br.unb.cic.tdp.base;

import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import com.google.common.primitives.Bytes;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.*;
import java.util.stream.Collectors;

import static br.unb.cic.tdp.base.CommonOperations.*;
import static br.unb.cic.tdp.permutation.PermutationGroups.computeProduct;

@ToString
public class OrientedConfiguration {

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

    public OrientedConfiguration(final MulticyclePermutation spi, final Cycle pi) {
        this.spi = spi;
        this.pi = pi;
        this.signature = new Signature(false, pi, signature(spi, pi));
    }

    public static OrientedConfiguration fromSignature(float[] signature) {
        final var pi = CANONICAL_PI[signature.length];

        final var cyclesByLabel = new HashMap<Byte, List<Byte>>();
        final var piSymbolsByOrientedCycleSymbols = new HashMap<Float, Byte>();
        final var orientedCyclesByLabel = new HashMap<Byte, List<Byte>>();

        for (int i = signature.length - 1; i >= 0; i--) {
            byte label = (byte) Math.floor(signature[i]);
            cyclesByLabel.computeIfAbsent(label, key -> new ArrayList<>());
            cyclesByLabel.get(label).add((byte) i);
            if (signature[i] % 1 > 0) {
                piSymbolsByOrientedCycleSymbols.put(signature[i], pi.get(i));
                orientedCyclesByLabel.computeIfAbsent(label, key -> cyclesByLabel.get(label));
            }
        }

        orientedCyclesByLabel.entrySet().stream().forEach(e -> {
            final var sortedSignature = signature.clone();
            Arrays.sort(sortedSignature);
            final var orientedCycle = new ArrayList<Byte>();
            for (int i = 0; i < signature.length; i++) {
                if (Math.floor(sortedSignature[i]) == e.getKey()) {
                    orientedCycle.add(piSymbolsByOrientedCycleSymbols.get(sortedSignature[i]));
                }
            }
            e.getValue().clear();
            e.getValue().addAll(orientedCycle);
        });

        final var spi = cyclesByLabel.values().stream().map(c -> new Cycle(Bytes.toArray(c)))
                .collect(Collectors.toCollection(MulticyclePermutation::new));

        return new OrientedConfiguration(spi, pi);
    }

    public static boolean isOpenGate(final int i, final float[] signature) {
        final var n = signature.length;
        float a = signature[i], b = signature[mod(i - 1, n)], c = signature[mod(i - 2, n)];
        return (a % 1 == 0 && a == b) || (a % 1 > 0 && b % 1 > 0 && c % 1 > 0 && c < a && a < b && c < b);
    }

    public static float[] signature(final List<Cycle> spi, final Cycle pi) {
        final var labelByCycle = new HashMap<Cycle, Float>();
        final var cycleIndex = createCycleIndex(spi, pi);
        final var orientedCycles = spi.stream().filter(c -> !areSymbolsInCyclicOrder(c.getSymbols(), pi.getInverse().getSymbols()))
                .collect(Collectors.toSet());
        final var symbolIndexByOrientedCycle = new HashMap<Cycle, byte[]>();

        final var signature = new float[pi.size()];

        for (var i = 0; i < signature.length; i++) {
            final int symbol = pi.get(i);
            final var cycle = cycleIndex[symbol];
            if (orientedCycles.contains(cycle)) {
                symbolIndexByOrientedCycle.computeIfAbsent(cycle, c -> {
                    final var symbolIndex = new byte[pi.size()];
                    for (int j = 0; j < c.size(); j++) {
                        symbolIndex[c.get(j)] = (byte) (j + 1);
                    }
                    return symbolIndex;
                });
            }
            labelByCycle.computeIfAbsent(cycle, c -> (float) (labelByCycle.size() + 1));
            signature[i] = orientedCycles.contains(cycle) ?
                    labelByCycle.get(cycle) + (float) symbolIndexByOrientedCycle.get(cycle)[symbol] / 10 : labelByCycle.get(cycle);
        }

        return signature;
    }

    private Set<Signature> getEquivalentSignatures() {
        if (equivalentSignatures != null) {
            return equivalentSignatures;
        }

        equivalentSignatures = new HashSet<>();

        final var sigma = computeProduct(spi, pi);

        for (var i = 0; i < pi.size(); i++) {
            final var shifting = pi.getStartingBy(pi.get(i));
            equivalentSignatures.add(new Signature(false, shifting, signature(spi, shifting)));
            final var mirroring = shifting.conjugateBy(sigma).asNCycle();
            equivalentSignatures.add(new Signature(true, mirroring, signature(spi.getInverse(), mirroring)));
        }

        return equivalentSignatures;
    }

    @ToString.Include
    public boolean isFull() {
        return getNumberOfOpenGates() == 0;
    }

    @ToString.Include
    public int get3Norm() {
        return this.spi.get3Norm();
    }

    @ToString.Include
    public int getNumberOfOpenGates() {
        var numberOfOpenGates = 0;
        final var n = signature.content.length;
        for (int i = 0; i < n; i++) {
            if (isOpenGate(i, signature.content)) {
                numberOfOpenGates++;
            }
        }
        return numberOfOpenGates;
    }

    public Map<Byte, Byte> getOpenGatesByCycleLabel() {
        final var openGatesByCycleLabel = new HashMap<Byte, Byte>();
        for (int i = 0; i < signature.content.length; i++) {
            if (isOpenGate(i, signature.content)) {
                openGatesByCycleLabel.computeIfAbsent((byte) Math.floor(signature.content[i]), key -> (byte) 0);
                openGatesByCycleLabel.computeIfPresent((byte) Math.floor(signature.content[i]), (key, value) -> (byte) (value + 1));
            }
        }
        return openGatesByCycleLabel;
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
        if (!(obj instanceof OrientedConfiguration)) {
            return false;
        }

        final var other = (OrientedConfiguration) obj;

        if (this.signature.content.length != other.signature.content.length || this.spi.size() != other.spi.size()) {
            return false;
        }

        return getEquivalentSignatures().contains(((OrientedConfiguration) obj).signature);
    }

    /**
     * Returns a configuration with the symbols changed as \sigma=(0,1,2,..,n).
     * Only applicable to full configurations.
     */
    public OrientedConfiguration getNormalConfiguration() {
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
        return new OrientedConfiguration(spi, pi);
    }


    @AllArgsConstructor
    class Signature {

        @Getter
        private final boolean mirror;

        @Getter
        private final Cycle pi;

        @Getter
        private float[] content;

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
