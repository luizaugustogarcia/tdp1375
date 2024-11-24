package br.unb.cic.tdp.base;

import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import com.google.common.primitives.Floats;
import com.google.common.primitives.Ints;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.val;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.CRC32;

import static br.unb.cic.tdp.base.CommonOperations.*;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toCollection;

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

    public Configuration(final String spi) {
        this(new MulticyclePermutation(spi));
    }

    public static float[] signature(final Collection<Cycle> spi, final Cycle pi) {
        val labelByCycle = new HashMap<Cycle, Float>();
        val cycleIndex = cycleIndex(spi, pi);
        val orientedCycles = spi.stream().filter(c -> !areSymbolsInCyclicOrder(pi.getInverse(), c.getSymbols()))
                .collect(Collectors.toSet());
        val symbolIndexByOrientedCycle = new HashMap<Cycle, int[]>();

        val signature = new float[pi.size()];

        for (var i = 0; i < signature.length; i++) {
            val symbol = pi.get(i);
            val cycle = cycleIndex[symbol];
            if (orientedCycles.contains(cycle)) {
                symbolIndexByOrientedCycle.computeIfAbsent(cycle, c -> {
                    val symbolIndex = new int[pi.getMaxSymbol() + 1];
                    val symbolMinIndex = Ints.asList(c.getSymbols()).stream().min(comparing(pi::indexOf)).get();
                    for (var j = 0; j < c.getSymbols().length; j++) {
                        if (c.getSymbols()[j] == symbolMinIndex) {
                            for (var k = 0; k < c.getSymbols().length; k++) {
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

    public static Configuration ofSignature(final float[] signature) {
        val pi = CANONICAL_PI[signature.length];

        val cyclesByLabel = new HashMap<Integer, List<Integer>>();
        val piSymbolsByOrientedCycleSymbols = new HashMap<Float, Integer>();
        val orientedCyclesByLabel = new HashMap<Integer, List<Integer>>();

        for (var i = signature.length - 1; i >= 0; i--) {
            val label = (int) Math.floor(signature[i]);
            cyclesByLabel.computeIfAbsent(label, key -> new ArrayList<>());
            cyclesByLabel.get(label).add(i);
            if (signature[i] % 1 > 0) {
                piSymbolsByOrientedCycleSymbols.put(signature[i], pi.get(i));
                orientedCyclesByLabel.computeIfAbsent(label, key -> cyclesByLabel.get(label));
            }
        }

        orientedCyclesByLabel.forEach((key, value) -> {
            val sortedSignature = signature.clone();
            Arrays.sort(sortedSignature);
            val orientedCycle = new ArrayList<Integer>();
            for (var i = 0; i < signature.length; i++) {
                if (Math.floor(sortedSignature[i]) == key) {
                    orientedCycle.add(piSymbolsByOrientedCycleSymbols.get(sortedSignature[i]));
                }
            }
            value.clear();
            value.addAll(orientedCycle);
        });

        val spi = cyclesByLabel.values().stream().map(c -> Cycle.of(Ints.toArray(c)))
                .collect(toCollection(MulticyclePermutation::new));
        return new Configuration(spi, pi);
    }

    public Configuration getCanonical() {
        if (canonical == null) {
            float[] canonicalSignature = null;
            var leastHashCode = Integer.MAX_VALUE;

            for (var it = getEquivalentSignatures().iterator(); it.hasNext(); ) {
                val equivalentSignature = it.next();
                val hashCode = equivalentSignature.hashCode();

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

    private static float[] least(final float[] signature1, final float[] signature2) {
        return Arrays.compare(signature1, signature2) == -1 ? signature1 : signature2;
    }

    public Stream<Signature> getEquivalentSignatures() {
        return IntStream.range(0, pi.size()).boxed().flatMap(i -> {
            val shiftedPi = pi.startingBy(pi.get(i));
            return Stream.of(new Signature(shiftedPi, signature(spi, shiftedPi), false), mirror(spi, shiftedPi));
        });
    }

    private static Signature mirror(final MulticyclePermutation spi, final Cycle pi) {
        val conjugator = new MulticyclePermutation();
        for (var i = 0; i < pi.size() / 2; i++) {
            conjugator.add(Cycle.of(pi.get(i), pi.get(pi.size() - 1 - i)));
        }
        val mirroredSpi = spi.conjugateBy(conjugator).getInverse();
        return new Signature(pi, signature(mirroredSpi, pi), true);
    }

    public List<Cycle> translatedSorting(final Configuration config, final List<Cycle> sorting) {
        val matchedSignature = this.getEquivalentSignatures()
                .filter(c -> Arrays.equals(c.getContent(), config.getSignature().getContent()))
                .findFirst().get();

        val translatedSorting = new ArrayList<Cycle>();
        var pi = config.getPi();
        var signaturePi = matchedSignature.pi;

        for (val move : sorting) {
            if (matchedSignature.isMirror()) {
                translatedSorting.add(Cycle.of(
                        signaturePi.get(Math.abs(pi.indexOf(move.get(0)) - pi.size()) - 1),
                        signaturePi.get(Math.abs(pi.indexOf(move.get(1)) - pi.size()) - 1),
                        signaturePi.get(Math.abs(pi.indexOf(move.get(2)) - pi.size()) - 1)).getInverse());
            } else {
                translatedSorting.add(Cycle.of(
                        signaturePi.get(pi.indexOf(move.get(0))),
                        signaturePi.get(pi.indexOf(move.get(1))),
                        signaturePi.get(pi.indexOf(move.get(2)))));
            }
            pi = applyTranspositionOptimized(pi, move);
            signaturePi = applyTranspositionOptimized(signaturePi, translatedSorting.get(translatedSorting.size() - 1));
        }

        return translatedSorting;
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

        val other = (Configuration) obj;

        if (this.signature.content.length != other.signature.content.length ||
                this.spi.size() != other.spi.size()) {
            return false;
        }

        return getCanonical().signature.equals(other.getCanonical().signature);
    }

    public Set<Integer> getOpenGates() {
        return CommonOperations.getOpenGates(spi, pi);
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
            val other = (Signature) o;
            for (var i = 0; i < other.content.length; i++) {
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
                val bas = new ByteArrayOutputStream();
                val ds = new DataOutputStream(bas);
                for (val f : content)
                    ds.writeFloat(f);
                val bytes = bas.toByteArray();
                val crc32 = new CRC32();
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

    public static void main(String[] args) {
        var spi = new MulticyclePermutation("(0 11 1)(2 13 9)(3 8 6)(4 14 12)(5 10 7)");
        var pi = CANONICAL_PI[15];
        var sigma = spi.times(pi);

        System.out.println(new Configuration(spi, pi));

        for (int i = 1; i < pi.size() - 1; i++) {
            System.out.print(i + "-");
            pi = pi.conjugateBy(sigma).asNCycle();
            spi = sigma.times(pi.getInverse());
            System.out.println(new Configuration(spi, pi));
        }
    }
}