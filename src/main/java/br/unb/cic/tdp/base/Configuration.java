package br.unb.cic.tdp.base;

import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import com.google.common.collect.Lists;
import com.google.common.primitives.Floats;
import com.google.common.primitives.Ints;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.ToString;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.CRC32;

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
    private Configuration canonical;

    @ToString.Exclude
    private Collection<Signature> equivalentSignatures;

    @ToString.Include
    private Set<Integer> openGates;

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
        final var symbolIndexByOrientedCycle = new HashMap<Cycle, int[]>();

        final var signature = new float[pi.size()];

        for (var i = 0; i < signature.length; i++) {
            final int symbol = pi.get(i);
            final var cycle = cycleIndex[symbol];
            if (cycle != null) {
                if (orientedCycles.contains(cycle)) {
                    symbolIndexByOrientedCycle.computeIfAbsent(cycle, c -> {
                        final var symbolIndex = new int[pi.getMaxSymbol() + 1];
                        final var symbolMinIndex = Ints.asList(c.getSymbols()).stream().min(comparing(pi::indexOf)).get();
                        c = c.startingBy(symbolMinIndex);
                        for (int j = 0; j < c.size(); j++) {
                            symbolIndex[c.get(j)] = j + 1;
                        }
                        return symbolIndex;
                    });
                }
                labelByCycle.computeIfAbsent(cycle, c -> (float) (labelByCycle.size() + 1));
                signature[i] = orientedCycles.contains(cycle) ?
                        labelByCycle.get(cycle) + (float) symbolIndexByOrientedCycle.get(cycle)[symbol] / 10 : labelByCycle.get(cycle);
            }
        }

        return signature;
    }

    public static Configuration ofSignature(float[] signature) {
        final var pi = CANONICAL_PI[signature.length];

        final var cyclesByLabel = new HashMap<Integer, List<Integer>>();
        final var piSymbolsByOrientedCycleSymbols = new HashMap<Float, Integer>();
        final var orientedCyclesByLabel = new HashMap<Integer, List<Integer>>();

        for (int i = signature.length - 1; i >= 0; i--) {
            int label = (int) Math.floor(signature[i]);
            cyclesByLabel.computeIfAbsent(label, key -> new ArrayList<>());
            cyclesByLabel.get(label).add((int) i);
            if (signature[i] % 1 > 0) {
                piSymbolsByOrientedCycleSymbols.put(signature[i], pi.get(i));
                orientedCyclesByLabel.computeIfAbsent(label, key -> cyclesByLabel.get(label));
            }
        }

        orientedCyclesByLabel.forEach((key, value) -> {
            final var sortedSignature = signature.clone();
            Arrays.sort(sortedSignature);
            final var orientedCycle = new ArrayList<Integer>();
            for (int i = 0; i < signature.length; i++) {
                if (Math.floor(sortedSignature[i]) == key) {
                    orientedCycle.add(piSymbolsByOrientedCycleSymbols.get(sortedSignature[i]));
                }
            }
            value.clear();
            value.addAll(orientedCycle);
        });

        final var spi = cyclesByLabel.values().stream().map(c -> Cycle.create(Ints.toArray(c)))
                .collect(Collectors.toCollection(MulticyclePermutation::new));

        return new Configuration(spi, pi);
    }

    public Configuration getCanonical() {
        if (canonical == null) {
            canonical = ofSignature(getEquivalentSignatures().stream().min(comparing(Signature::hashCode)).get().getContent());
        }
        return canonical;
    }

    public Collection<Signature> getEquivalentSignatures() {
        if (equivalentSignatures != null) {
            return equivalentSignatures;
        }

        equivalentSignatures = new HashSet<>();

        for (var i = 0; i < pi.size(); i++) {
            final var shifting = pi.startingBy(pi.get(i));
            equivalentSignatures.add(new Signature(shifting, signature(spi, shifting), false));

            final var mirroredShifting = mirroredPi.startingBy(mirroredPi.get(i));
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
            var mirroredMoves = new ArrayList<Cycle>();
            for (final var move : sorting) {
                pis.add(computeProduct(move, pis.get(pis.size() - 1)).asNCycle());
                spis.add(computeProduct(spis.get(spis.size() - 1), move.getInverse()));
                mirroredMoves.add(move.getInverse().conjugateBy(spis.get(spis.size() - 1)).asNCycle());
            }
            shiftedOrMirroredSorting = mirroredMoves;
        }

        final var translatedSorting = new ArrayList<Cycle>();

        var pi = matchedSignature.getPi();
        var _pi = this.pi;
        for (final var move : shiftedOrMirroredSorting) {
            translatedSorting.add(Cycle.create(
                    _pi.get(pi.indexOf(move.get(0))),
                    _pi.get(pi.indexOf(move.get(1))),
                    _pi.get(pi.indexOf(move.get(2)))));
            pi = applyTransposition(pi, move);
            _pi = applyTransposition(_pi, translatedSorting.get(translatedSorting.size() - 1));
        }

        return translatedSorting;
    }

    @Deprecated // replace by translatedSorting
    public List<Cycle> equivalentSorting(final Signature signature, final List<Cycle> sorting) {
        if (signature.isMirror()) {
            final var pis = Lists.newArrayList(this.getPi());
            final var spis = Lists.newArrayList(new MulticyclePermutation[]{this.getSpi()});
            var mirroredMoves = new ArrayList<Cycle>();
            for (final var move : sorting) {
                pis.add(computeProduct(move, pis.get(pis.size() - 1)).asNCycle());
                spis.add(computeProduct(spis.get(spis.size() - 1), move.getInverse()));
                mirroredMoves.add(move.getInverse().conjugateBy(spis.get(spis.size() - 1)).asNCycle());
            }
            return mirroredMoves;
        }

        return sorting;
    }

    @ToString.Include
    public boolean isFull() {
        return getOpenGates().isEmpty();
    }

    @ToString.Include
    public int get3Norm() {
        return this.spi.get3Norm();
    }

    @Override
    @ToString.Include
    public int hashCode() {
        return getCanonical().signature.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof Configuration)) {
            return false;
        }

        if (this.hashCode() != obj.hashCode()) {
            return false;
        }

        final var other = (Configuration) obj;

        if (this.signature.content.length != other.signature.content.length ||
                this.spi.size() != other.spi.size()) {
            return false;
        }

        return getEquivalentSignatures().contains(((Configuration) obj).signature);
    }

    public Set<Integer> getOpenGates() {
        if (openGates != null)
            return openGates;

        return openGates = CommonOperations.getOpenGates(spi, pi, signature.content);
    }

    public static class Signature {

        @Getter
        private final Cycle pi;

        @Getter
        private final float[] content;

        @Getter
        private final boolean mirror;

        private Integer hashCode;

        public Signature(final Cycle pi, final float[] content, final boolean mirror) {
            this.pi = pi;
            this.content = content;
            this.mirror = mirror;
        }

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

        @SneakyThrows
        @Override
        public int hashCode() {
            if (hashCode == null) {
                final var bas = new ByteArrayOutputStream();
                final var ds = new DataOutputStream(bas);
                for (float f : content)
                    ds.writeFloat(f);
                byte[] bytes = bas.toByteArray();
                final var crc32 = new CRC32();
                crc32.update(bytes, 0, bytes.length);
                hashCode = (int) crc32.getValue();
            }
            return hashCode;
        }

        @Override
        public String toString() {
            return "[" + Floats.asList(content).stream()
                    .map(f -> f % 1 == 0 ? Integer.toString((int) Math.floor(f)) : Float.toString(f))
                    .collect(Collectors.joining(",")) + "]";
        }
    }
}