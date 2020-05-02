package br.unb.cic.tdp.base;

import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import com.google.common.collect.Lists;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Floats;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.*;
import java.util.stream.Collectors;

import static br.unb.cic.tdp.base.CommonOperations.*;
import static br.unb.cic.tdp.permutation.PermutationGroups.computeProduct;
import static java.util.Comparator.comparing;

@ToString
public class Configuration {

    @Getter
    private final MulticyclePermutation spi;

    @Getter
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

    public Configuration(final MulticyclePermutation spi) {
        this(spi, CANONICAL_PI[spi.getNumberOfSymbols()]);
    }

    public static float[] signature(final List<Cycle> spi, final Cycle pi) {
        final var labelByCycle = new HashMap<Cycle, Float>();
        final var cycleIndex = cycleIndex(spi, pi);
        final var orientedCycles = spi.stream().filter(c -> !areSymbolsInCyclicOrder(pi.getInverse(), c.getSymbols()))
                .collect(Collectors.toSet());
        final var symbolIndexByOrientedCycle = new HashMap<Cycle, byte[]>();

        final var signature = new float[pi.size()];

        for (var i = 0; i < signature.length; i++) {
            final int symbol = pi.get(i);
            final var cycle = cycleIndex[symbol];
            if (orientedCycles.contains(cycle)) {
                symbolIndexByOrientedCycle.computeIfAbsent(cycle, c -> {
                    final var symbolIndex = new byte[pi.getMaxSymbol() + 1];
                    final var symbolMinIndex = Bytes.asList(c.getSymbols()).stream().sorted(comparing(s -> pi.indexOf(s))).findFirst().get();
                    c = c.getStartingBy(symbolMinIndex);
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
        return (a % 1 == 0 && a == b) || (a % 1 > 0 && b % 1 > 0 && c % 1 > 0 &&
                Math.floor(a) == Math.floor(b) && Math.floor(a) == Math.floor(c) && c < a && a < b && c < b);
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

    public Configuration getCanonical() {
        if (canonical == null) {
            canonical = fromSignature(getEquivalentSignatures().stream()
                    .sorted(comparing(Signature::hashCode)).findFirst().get().getContent());
        }
        return canonical;
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

    /**
     * Assumes that this configuration is equivalent to the one provided as parameter.
     */
    public List<Cycle> translatedSorting(final Configuration config, final List<Cycle> sorting) {
        final var matchedSignature = config.getEquivalentSignatures().stream()
                .filter(c -> Arrays.equals(c.getContent(), this.getSignature().getContent()))
                .findFirst().get();

        var shiftedOrMirroredSorting = sorting;

        if (matchedSignature.isMirror()) {
            final var pis = Lists.newArrayList(config.getPi());
            final var spis = Lists.newArrayList(new MulticyclePermutation[]{config.getSpi()});
            var mirroredRhos = new ArrayList<Cycle>();
            for (final var rho : sorting) {
                pis.add(computeProduct(rho, pis.get(pis.size() - 1)).asNCycle());
                spis.add(computeProduct(spis.get(spis.size() - 1), rho.getInverse()));
                mirroredRhos.add(rho.getInverse().conjugateBy(spis.get(spis.size() - 1)).asNCycle());
            }
            shiftedOrMirroredSorting = mirroredRhos;
        }

        final var translatedSorting = new ArrayList<Cycle>();

        var pi = matchedSignature.getPi();
        var _pi = this.pi;
        for (final var rho : shiftedOrMirroredSorting) {
            translatedSorting.add(new Cycle(
                    _pi.get(pi.indexOf(rho.get(0))),
                    _pi.get(pi.indexOf(rho.get(1))),
                    _pi.get(pi.indexOf(rho.get(2)))));
            pi = applyTransposition(pi, rho);
            _pi = applyTransposition(_pi, translatedSorting.get(translatedSorting.size() - 1));
        }

        return translatedSorting;
    }

    @Deprecated // replace by translatedSorting
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
        return getOpenGates().size();
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

        if (this.signature.content.length != other.signature.content.length ||
                this.spi.size() != other.spi.size()) {
            return false;
        }

        return getEquivalentSignatures().contains(((Configuration) obj).signature);
    }

    public List<Integer> getOpenGates() {
        final var result = new ArrayList<Integer>();
        final var n = signature.content.length;
        for (int i = 0; i < n; i++) {
            if (isOpenGate(i, signature.content)) {
                result.add(i);
            }
        }
        return result;
    }

    @AllArgsConstructor
    public class Signature {

        @Getter
        private final Cycle pi;

        @Getter
        private final float[] content;

        @Getter
        private final boolean mirror;

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
            return "[" + Floats.asList(content).stream()
                    .map(f -> f % 1 == 0 ? Byte.toString((byte) Math.floor(f)) : Float.toString(f))
                    .collect(Collectors.joining(",")) + "]";
        }
    }
}