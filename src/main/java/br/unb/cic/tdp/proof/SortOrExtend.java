package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.util.Pair;
import cern.colt.list.FloatArrayList;
import lombok.val;

import java.util.*;
import java.util.concurrent.ForkJoinTask;

import static br.unb.cic.tdp.base.CommonOperations.cycleIndex;
import static br.unb.cic.tdp.base.Configuration.ofSignature;
import static br.unb.cic.tdp.base.Configuration.signature;
import static java.util.stream.Stream.concat;

class SortOrExtend extends AbstractSortOrExtend {

    public SortOrExtend(final Configuration configuration,
                        final ProofStorage storage,
                        final double minRate) {
        super(configuration, storage, minRate);
    }

    /*
     * Type 1 extension.
     */
    static List<Pair<String, Configuration>> type1Extensions(final Configuration configuration) {
        val result = new ArrayList<Pair<String, Configuration>>();

        val newLabel = configuration.getSpi().size() + 1;

        val signature = signature(configuration.getSpi(), configuration.getPi());

        for (var i = 0; i < signature.length; i++) {
            if (configuration.getOpenGates().contains(i)) {
                for (var b = 0; b < signature.length; b++) {
                    for (var c = b; c < signature.length; c++) {
                        if (!(i == b && b == c)) {
                            result.add(new Pair<>(String.format("a=%d b=%d c=%d", i, b, c),
                                    ofSignature(unorientedExtension(signature, newLabel, i, b, c).elements())));
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

        val newLabel = config.getSpi().size() + 1;

        val signature = signature(config.getSpi(), config.getPi());

        for (var a = 0; a < signature.length; a++) {
            for (var b = a; b < signature.length; b++) {
                for (var c = b; c < signature.length; c++) {
                    if (!(a == b && b == c)) {
                        result.add(new Pair<>(String.format("a=%d b=%d c=%d", a, b, c),
                                ofSignature(unorientedExtension(signature, newLabel, a, b, c).elements())));
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
        for (var i = 0; i < signature.length; i++) {
            cyclesSizes.putIfAbsent((int) Math.floor(signature[i]), 0);
            cyclesSizes.computeIfPresent((int) Math.floor(signature[i]), (k, v) -> v + 1);
            indexesByLabel.computeIfAbsent((int) Math.floor(signature[i]), s -> new ArrayList<>());
            val finalI = i;
            indexesByLabel.computeIfPresent((int) Math.floor(signature[i]), (k, v) -> {
                v.add(finalI);
                return v;
            });
        }

        val cycleIndex = cycleIndex(config.getSpi(), config.getPi());
        val cyclesByLabel = new HashMap<Integer, Cycle>();
        for (var i = 0; i < signature.length; i++) {
            val _i = i;
            cyclesByLabel.computeIfAbsent((int) Math.floor(signature[i]), k -> cycleIndex[config.getPi().get(_i)]);
        }

        if (config.getSpi().toString().equals("(0 1 3 2)")) {
            System.out.println();
        }

        for (var label = 1; label <= config.getSpi().size(); label++) {
            var newLabel = roundTwoDecimalPlaces(label + 0.01F);
            val firstIndex = indexesByLabel.get(label).get(0);
            var isOriented = false;
            if (Math.floor(signature[firstIndex]) == signature[firstIndex]) {
                // unoriented cycle
                val indexes = indexesByLabel.get(label);
                for (var i = 0; i < indexes.size(); i++) {
                    signature[indexes.get(i)] += roundTwoDecimalPlaces(0.01F * (indexes.size() - i));
                    if (roundTwoDecimalPlaces(signature[indexes.get(i)] + 0.01F) > newLabel) {
                        newLabel = roundTwoDecimalPlaces(signature[indexes.get(i)] + 0.01F);
                    }
                }
            } else {
                // oriented
                newLabel = roundTwoDecimalPlaces((indexesByLabel.get(label).size() + 1) * 0.01F + label);
                isOriented = true;
            }

            val size = indexesByLabel.get(label).size();

            for (var i = 0; i < size; i++) {
                // rotate the labels
                val rotatedSignature = signature.clone();
                for (int j = 0; j < rotatedSignature.length; j++) {
                    if ((int) rotatedSignature[j] == label) {
                        var index = (int) (roundTwoDecimalPlaces(rotatedSignature[j] * 100F)) - (100 * label);
                        index = (index + i) % size;
                        rotatedSignature[j] = roundTwoDecimalPlaces(label + ((index + 1) / 100F));
                    }
                }

                for (var a = 0; a < rotatedSignature.length; a++) {
                    val extension = ofSignature(insertAtPosition(rotatedSignature, newLabel, a));
                    if (closesOneOpenGate(openGates, extension) || (openGates == 0 && extension.getOpenGates().size() <= 1)) {
                        result.add(new Pair<>(String.format("a=%d, r=%d, extended cycle: %s", a, i, cyclesByLabel.get(label)), extension));
                    }
                }
            }
        }

        return result;
    }

    public static float roundTwoDecimalPlaces(final float value) {
        return Math.round(value * 100.0f) / 100.0f;
    }

    private static boolean closesOneOpenGate(final int openGates, final Configuration extension) {
        return openGates > 0 && extension.getOpenGates().size() < openGates;
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
        concat(concat(type1Extensions(canonical).stream(), type2Extensions(canonical).stream()), type3Extensions(canonical).stream())
                .filter(pair -> pair.getSecond().getOpenGates().size() <= 2)
                .map(extension -> new SortOrExtend(extension.getSecond(), storage, minRate)).
                forEach(ForkJoinTask::fork);
    }

    public static FloatArrayList unorientedExtension(final float[] signature, final int label, final int... positions) {
        Arrays.sort(positions);
        val extension = new FloatArrayList(signature);
        for (var i = 0; i < positions.length; i++) {
            extension.beforeInsert(positions[i] + i, label);
        }
        extension.trimToSize();
        return extension;
    }
}
