package br.unb.cic.tdp.base;

import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import com.google.common.collect.Lists;
import com.google.common.primitives.Bytes;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import static br.unb.cic.tdp.base.CommonOperations.*;
import static br.unb.cic.tdp.permutation.PermutationGroups.computeProduct;

@ToString
public class Configuration {

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

    @ToString.Exclude
    private Configuration canonical;

    public Configuration(final MulticyclePermutation spi, final Cycle pi) {
        this.spi = spi;
        this.pi = pi;
        this.mirroredPi = pi.getInverse().conjugateBy(spi).asNCycle();
        this.signature = new Signature(pi, signature(spi, pi), false);
    }

    public Configuration getCanonical() {
        if (canonical == null) {
            canonical = fromSignature(getEquivalentSignatures().stream()
                    .sorted(Comparator.comparing(Signature::hashCode)).findFirst().get().getSignature());
        }
        return canonical;
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

    public static boolean isOpenGate(final int i, final float[] signature) {
        final var n = signature.length;
        float a = signature[mod(i, n)], b = signature[mod(i - 1, n)], c = signature[mod(i - 2, n)];
        return (a % 1 == 0 && a == b) || (a % 1 > 0 && b % 1 > 0 && c % 1 > 0 && c < a && a < b && c < b);
    }

    public static Configuration fromSignature(float[] signature) {
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

        return new Configuration(spi, pi);
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

    public List<Cycle> equivalentSorting(final Signature signature, final List<Cycle> sorting) {
        if (signature.isMirror()) {
            final var pis = Lists.newArrayList(this.getPi());
            final var spis = Lists.newArrayList(new MulticyclePermutation[]{this.getSpi()});
            var mirroredRhos = new ArrayList<Cycle>();
            for (final var rho : sorting) {
                pis.add(computeProduct(rho, pis.get(pis.size() - 1)).asNCycle());
                spis.add(computeProduct(spis.get(spis.size() - 1), rho.getInverse()));
                mirroredRhos.add(rho.getInverse().conjugateBy(spis.get(spis.size() - 1)).asNCycle());
            }
            return mirroredRhos;
        }

        return sorting;
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
        final var n = signature.signature.length;
        for (int i = 0; i < n; i++) {
            if (isOpenGate(i, signature.signature)) {
                numberOfOpenGates++;
            }
        }
        return numberOfOpenGates;
    }

    @Override
    @ToString.Include
    public int hashCode() {
        if (this.hashCode == null) {
            this.hashCode = getCanonical().signature.hashCode();
        }
        return this.hashCode;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof Configuration)) {
            return false;
        }

        final var other = (Configuration) obj;

        if (this.signature.signature.length != other.signature.signature.length ||
                this.spi.size() != other.spi.size()) {
            return false;
        }

        return getEquivalentSignatures().contains(((Configuration) obj).signature);
    }


    @AllArgsConstructor
    public class Signature {

        @Getter
        private final Cycle pi;

        @Getter
        private final float[] signature;

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