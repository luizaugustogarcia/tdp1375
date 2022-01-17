package br.unb.cic.tdp.base;

import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import com.google.common.collect.Lists;
import com.google.common.primitives.Floats;
import com.google.common.primitives.Ints;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.ToString;
import org.apache.commons.lang.ArrayUtils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.CRC32;

import static br.unb.cic.tdp.base.CommonOperations.*;
import static br.unb.cic.tdp.permutation.PermutationGroups.computeProduct;
import static java.util.Comparator.comparing;

/**
 * Only supports oriented cycles whose length is at most 100.
 */
@ToString
public class Configuration {

    @Getter
    private final MulticyclePermutation spi;

    @Getter
    private final Cycle pi;

    @Getter
    private final Signature signature;

    @ToString.Exclude
    private Configuration canonical;

    public Configuration(final MulticyclePermutation spi, final Cycle pi) {
        this.spi = spi;
        this.pi = pi;
        this.signature = new Signature(pi, signature(spi, pi), false);
    }

    public Configuration(final MulticyclePermutation spi) {
        this(spi, CANONICAL_PI[spi.getNumberOfSymbols()]);
    }

    public Configuration(String spi) {
        this(new MulticyclePermutation(spi));
    }

    public static float[] signature(final Collection<Cycle> spi, final Cycle pi) {
        final var labelByCycle = new HashMap<Cycle, Float>();
        final var cycleIndex = cycleIndex(spi, pi);
        final var orientedCycles = spi.stream().filter(c -> !areSymbolsInCyclicOrder(pi.getInverse(), c.getSymbols()))
                .collect(Collectors.toSet());
        final var symbolIndexByOrientedCycle = new HashMap<Cycle, int[]>();

        final var signature = new float[pi.size()];

        for (var i = 0; i < signature.length; i++) {
            final int symbol = pi.get(i);
            final var cycle = cycleIndex[symbol];
            if (orientedCycles.contains(cycle)) {
                symbolIndexByOrientedCycle.computeIfAbsent(cycle, c -> {
                    final var symbolIndex = new int[pi.getMaxSymbol() + 1];
                    final int symbolMinIndex = Ints.asList(c.getSymbols()).stream().min(comparing(pi::indexOf)).get();
                    for (int j = 0; j < c.getSymbols().length; j++) {
                        if (c.getSymbols()[j] == symbolMinIndex) {
                            for (int k = 0; k < c.getSymbols().length; k++) {
                                symbolIndex[c.getSymbols()[(j + k) % c.getSymbols().length]] = k + 1;
                            }
                            break;
                        }
                    }
                    return symbolIndex;
                });
            }
            labelByCycle.computeIfAbsent(cycle, c -> (float) (labelByCycle.size() + 1));
            signature[i] = orientedCycles.contains(cycle) ?
                    labelByCycle.get(cycle) + (float) symbolIndexByOrientedCycle.get(cycle)[symbol] / 100 : labelByCycle.get(cycle);
        }

        return signature;
    }

    public static boolean isOpenGate(final int i, final float[] signature) {
        final var n = signature.length;
        float a = signature[mod(i, n)], b = signature[mod(i - 1, n)], c = signature[mod(i - 2, n)];
        return (a % 1 == 0 && a == b) || (a % 1 > 0 && b % 1 > 0 && c % 1 > 0 &&
                Math.floor(a) == Math.floor(b) && Math.floor(a) == Math.floor(c) && c < a && a < b);
    }

    public static Configuration ofSignature(float[] signature) {
        final var pi = CANONICAL_PI[signature.length];

        final var cyclesByLabel = new HashMap<Integer, List<Integer>>();
        final var piSymbolsByOrientedCycleSymbols = new HashMap<Float, Integer>();
        final var orientedCyclesByLabel = new HashMap<Integer, List<Integer>>();

        for (int i = signature.length - 1; i >= 0; i--) {
            int label = (int) Math.floor(signature[i]);
            cyclesByLabel.computeIfAbsent(label, key -> new ArrayList<>());
            cyclesByLabel.get(label).add(i);
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
            float[] canonicalSignature = null;
            var leastHashCode = Integer.MAX_VALUE;

            for (final var equivalentSignature : getEquivalentSignatures()) {
                var hashCode = equivalentSignature.hashCode();

                if (hashCode < leastHashCode) {
                    leastHashCode = hashCode;
                    canonicalSignature = equivalentSignature.content;
                } else if (hashCode == leastHashCode) {
                    canonicalSignature = least(equivalentSignature.content, canonicalSignature);
                }
            }

            canonical = ofSignature(canonicalSignature);
        }

        return canonical;
    }

    private static float[] least(final float[] signature, final float[] canonical) {
        for (int i = 0; i < signature.length; i++) {
            if (signature[i] != canonical[i]) {
                if (signature[i] < canonical[i])
                    return signature;
                else
                    return canonical;
            }
        }
        return canonical;
    }

    public Collection<Signature> getEquivalentSignatures() {
        Set<Signature> equivalentSignatures = new HashSet<>();

        final var cycleIndex = cycleIndex(spi, pi);

        for (var i = 0; i < pi.size(); i++) {
            final var shifting = pi.startingBy(pi.get(i));
            final var signature = signature(spi, shifting);
            equivalentSignatures.add(new Signature(shifting, signature, false));

            final var mirroredSignature = signature.clone();
            ArrayUtils.reverse(mirroredSignature);

            final var labelLabelMapping = new int[spi.size() + 1];
            final var orientedIndexMapping = new int[spi.size() + 1][];
            final var deltas = new float[spi.size() + 1];
            Arrays.fill(deltas, -1);

            var nextLabel = 1;
            for (int j = 0; j < mirroredSignature.length; j++) {
                final var label = mirroredSignature[j];

                if (labelLabelMapping[(int) label] == 0) {
                    labelLabelMapping[(int) label] = nextLabel++;
                }

                final var newLabel = labelLabelMapping[(int) label];

                if (label % 1 > 0) {
                    if (orientedIndexMapping[newLabel] == null) {
                        final var index = Math.abs(j - shifting.size()) - 1;
                        final var cycle = cycleIndex[shifting.get(index)].startingBy(shifting.get(index));
                        orientedIndexMapping[newLabel] = cycle.getInverse().getSymbolIndexes();
                        final var delta = cycle.size() - round((label % 1) * 100);
                        deltas[newLabel] = delta;
                    }

                    final var index = Math.abs(j - shifting.size()) - 1;
                    final var orientationIndex = orientedIndexMapping[newLabel][shifting.getSymbols()[index]] + 1;
                    mirroredSignature[j] = newLabel + (((orientationIndex + deltas[newLabel]) % cycleIndex[shifting.get(index)].size()) / 100);
                    if (mirroredSignature[j] % 1 == 0)
                        mirroredSignature[j] = newLabel + (cycleIndex[shifting.get(index)].size() / 100f);
                } else {
                    mirroredSignature[j] = newLabel;
                }
            }

            equivalentSignatures.add(new Signature(shifting, mirroredSignature, true));
        }

        return equivalentSignatures;
    }

    private float round(final float value) {
        return (float) Math.round(value * 100) / 100;
    }

    /**
     * Assumes that this configuration is equivalent to the one provided as parameter.
     */
    public List<Cycle> translatedSorting(final Configuration config, final List<Cycle> sorting) {
        final var matchedSignature = this.getEquivalentSignatures().stream()
                .filter(c -> Arrays.equals(c.getContent(), config.getSignature().getContent()))
                .findFirst().get();

        final var translatedSorting = new ArrayList<Cycle>();
        var pi = config.getPi();
        var _pi = matchedSignature.pi;

        for (final var move : sorting) {
            if (matchedSignature.isMirror()) {
                translatedSorting.add(Cycle.create(
                        _pi.get(Math.abs(pi.indexOf(move.get(0)) - pi.size()) - 1),
                        _pi.get(Math.abs(pi.indexOf(move.get(1)) - pi.size()) - 1),
                        _pi.get(Math.abs(pi.indexOf(move.get(2)) - pi.size()) - 1)).getInverse());
            } else {
                translatedSorting.add(Cycle.create(
                        _pi.get(pi.indexOf(move.get(0))),
                        _pi.get(pi.indexOf(move.get(1))),
                        _pi.get(pi.indexOf(move.get(2)))));
            }
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
        return getCanonical().signature.hashCode();
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

        return getCanonical().signature.equals(other.getCanonical().signature);
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