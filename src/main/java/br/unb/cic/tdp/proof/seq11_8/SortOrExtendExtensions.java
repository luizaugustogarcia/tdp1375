package br.unb.cic.tdp.proof.seq11_8;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.util.Pair;
import cern.colt.list.FloatArrayList;
import lombok.val;

import java.util.*;
import java.util.concurrent.ForkJoinTask;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static br.unb.cic.tdp.base.CommonOperations.cycleIndex;
import static br.unb.cic.tdp.base.Configuration.ofSignature;
import static br.unb.cic.tdp.base.Configuration.signature;

class SortOrExtendExtensions extends SortOrExtend {

    public SortOrExtendExtensions(final Configuration extendedFrom,
                                  final Configuration configuration,
                                  final Predicate<Configuration> shouldStop,
                                  final Predicate<Configuration> isValidExtension,
                                  final ProofStorage storage) {
        super(extendedFrom, configuration, shouldStop, isValidExtension, storage);
    }

    /*
     * Type 1 extension.
     */
    static List<Pair<String, Configuration>> type1Extensions(final Configuration config) {
        val result = new ArrayList<Pair<String, Configuration>>();

        val newCycleLabel = config.getSpi().size() + 1;

        val signature = signature(config.getSpi(), config.getPi());

        for (int i = 0; i < signature.length; i++) {
            if (config.getOpenGates().contains(i)) {
                for (int b = 0; b < signature.length; b++) {
                    for (int c = b; c < signature.length; c++) {
                        if (!(i == b && b == c)) {
                            result.add(new Pair<>(String.format("a=%d b=%d c=%d", i, b, c),
                                    ofSignature(unorientedExtension(signature, newCycleLabel, i, b, c).elements())));
                        }
                    }
                }
            }
        }

        return result;
    }

    /*
     * Type 2 extension.
     */
    static List<Pair<String, Configuration>> type2Extensions(final Configuration config) {
        if (!config.isFull()) {
            return Collections.emptyList();
        }

        val result = new ArrayList<Pair<String, Configuration>>();

        val newCycleLabel = config.getSpi().size() + 1;

        val signature = signature(config.getSpi(), config.getPi());

        for (int a = 0; a < signature.length; a++) {
            for (int b = a; b < signature.length; b++) {
                for (int c = b; c < signature.length; c++) {
                    if (!(a == b && b == c)) {
                        result.add(new Pair<>(String.format("a=%d b=%d c=%d", a, b, c),
                                ofSignature(unorientedExtension(signature, newCycleLabel, a, b, c).elements())));
                    }
                }
            }
        }

        return result;
    }

    /*
     * Type 3 extension.
     */
    static List<Pair<String, Configuration>> type3Extensions(final Configuration config) {
        val openGates = config.getOpenGates().size();

        val result = new ArrayList<Pair<String, Configuration>>();

        val signature = signature(config.getSpi(), config.getPi());
        val cyclesSizes = new HashMap<Integer, Integer>();
        val indexesByLabel = new HashMap<Integer, List<Integer>>();
        for (int i = 0; i < signature.length; i++) {
            cyclesSizes.putIfAbsent((int) Math.floor(signature[i]), 0);
            cyclesSizes.computeIfPresent((int) Math.floor(signature[i]), (k, v) -> v + 1);
            indexesByLabel.computeIfAbsent((int) Math.floor(signature[i]), s -> new ArrayList<>());
            int finalI = i;
            indexesByLabel.computeIfPresent((int) Math.floor(signature[i]), (k, v) -> {
                v.add(finalI);
                return v;
            });
        }

        val cycleIndex = cycleIndex(config.getSpi(), config.getPi());
        val cyclesByLabel = new HashMap<Integer, Cycle>();
        for (int i = 0; i < signature.length; i++) {
            final int _i = i;
            cyclesByLabel.computeIfAbsent((int) Math.floor(signature[i]), k -> cycleIndex[config.getPi().get(_i)]);
        }

        for (int label = 1; label <= config.getSpi().size(); label++) {
            var nextLabel = label + 0.01F;
            val firstIndex = indexesByLabel.get(label).get(0);
            if (Math.floor(signature[firstIndex]) == signature[firstIndex]) {
                // unoriented cycle
                val indexes = indexesByLabel.get(label);
                for (int i = 0; i < indexes.size(); i++) {
                    signature[indexes.get(i)] += 0.01F * (indexes.size() - i);
                    if (signature[indexes.get(i)] + 0.01 > nextLabel) {
                        nextLabel = signature[indexes.get(i)] + 0.01F;
                    }
                }
            } else {
                // oriented
                nextLabel = (indexesByLabel.get(label).size() + 1) * 0.01F + label;
            }

            for (int a = 0; a < signature.length; a++) {
                val extendedSignature = insertAtPosition(signature, nextLabel, a);
                for (int b = 0; b < extendedSignature.length; b++) {
                    val extendedSignaturePrime = insertAtPosition(extendedSignature, nextLabel + 0.01F, b);
                    val extension = ofSignature(extendedSignaturePrime);
                    if (closesOneOpenGate(openGates, extension) || extension.getOpenGates().size() <= 2) {
                        result.add(new Pair<>(String.format("a=%d, b=%d, extended cycle: %s", a, b, cyclesByLabel.get(label)), extension));
                    }
                }
            }
        }

        return result;
    }

    private static boolean closesOneOpenGate(final int openGates, final Configuration extension) {
        return openGates > 0 && extension.getOpenGates().size() == openGates - 1;
    }

    public static float[] insertAtPosition(float[] array, float value, int index) {
        val newArray = new float[array.length + 1];
        System.arraycopy(array, 0, newArray, 0, index);
        newArray[index] = value;
        System.arraycopy(array, index, newArray, index + 1, array.length - index);
        return newArray;
    }

    @Override
    protected void extend(final Configuration canonical) {
        Stream.concat(Stream.concat(type1Extensions(canonical).stream(), type2Extensions(canonical).stream()),
                        type3Extensions(canonical).stream())
                .filter(pair -> pair.getSecond().getOpenGates().size() <= 2)
                .map(extension -> new SortOrExtendExtensions(canonical, extension.getSecond(), shouldStop, isValidExtension, storage)).
                forEach(ForkJoinTask::fork);
    }

    private static FloatArrayList unorientedExtension(final float[] signature, final int label, final int... positions) {
        Arrays.sort(positions);
        val extension = new FloatArrayList(signature);
        for (int i = 0; i < positions.length; i++) {
            extension.beforeInsert(positions[i] + i, label);
        }
        extension.trimToSize();
        return extension;
    }
}
