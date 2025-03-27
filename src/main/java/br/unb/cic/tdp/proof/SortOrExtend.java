package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.util.Pair;
import cern.colt.list.IntArrayList;
import lombok.val;

import java.util.*;
import java.util.concurrent.ForkJoinTask;

import static br.unb.cic.tdp.base.CommonOperations.cycleIndex;
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

        val n = configuration.getPi().getSymbols().length;

        for (var i = 0; i < n; i++) {
            if (configuration.getOpenGates().contains(i)) {
                for (var b = 0; b < n; b++) {
                    for (var c = b; c < n; c++) {
                        if (!(i == b && b == c)) {
                            val newCycle = "(" + n + " " + (n + 2) + " " + (n + 1) + ")";
                            val extendedPi = unorientedExtension(configuration.getPi().getSymbols(), n, i, b, c).elements();
                            val extension = new Configuration(new MulticyclePermutation(configuration.getSpi() + newCycle), Cycle.of(extendedPi));
                            result.add(new Pair<>(String.format("a=%d b=%d c=%d", i, b, c), extension));
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

        val n = config.getPi().getSymbols().length;

        for (var a = 0; a < n; a++) {
            for (var b = a; b < n; b++) {
                for (var c = b; c < n; c++) {
                    if (!(a == b && b == c)) {
                        val newCycle = "(" + n + " " + (n + 2) + " " + (n + 1) + ")";
                        val extendedPi = unorientedExtension(config.getPi().getSymbols(), n, a, b, c).elements();
                        val extension = new Configuration(new MulticyclePermutation(config.getSpi() + newCycle), Cycle.of(extendedPi));
                        result.add(new Pair<>(String.format("a=%d b=%d c=%d", a, b, c), extension));
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

        val cycleIndex = cycleIndex(config.getSpi(), config.getPi());
        val cyclesByLabel = new HashMap<Integer, Cycle>();
        for (var i = 0; i < signature.length; i++) {
            val _i = i;
            cyclesByLabel.computeIfAbsent((int) Math.floor(signature[i]), k -> cycleIndex[config.getPi().get(_i)]);
        }

        val n = config.getPi().getSymbols().length;

        for (var label = 1; label <= config.getSpi().size(); label++) {
            val cycle = cyclesByLabel.get(label);
            if (cycle.size() == 3) { // only 3-cycles are extended
                val extendedSpi = config.getSpi().toString().replace(cycle.toString(), cycle.toString().replace(")", " " + n + ")"));

                for (var a = 0; a <= n; a++) {
                    val extendedPi = insertAtPosition(config.getPi().getSymbols(), n, a);
                    val extension = new Configuration(new MulticyclePermutation(extendedSpi), Cycle.of(extendedPi));
                    if (closesOneOpenGate(openGates, extension) || (openGates == 0 && extension.getOpenGates().size() <= 1)) {
                        result.add(new Pair<>(String.format("a=%d, extended cycle: %s", a, cycle), extension));
                    }
                }
            }
        }

        return result;
    }

    private static boolean closesOneOpenGate(final int openGates, final Configuration extension) {
        return openGates > 0 && extension.getOpenGates().size() < openGates;
    }

    public static int[] insertAtPosition(final int[] array, final int value, final int index) {
        val newArray = new int[array.length + 1];
        System.arraycopy(array, 0, newArray, 0, index);
        newArray[index] = value;
        System.arraycopy(array, index, newArray, index + 1, array.length - index);
        return newArray;
    }

    @Override
    protected void extend(final Configuration configuration) {
        concat(concat(type1Extensions(configuration).stream(), type2Extensions(configuration).stream()), type3Extensions(configuration).stream())
                .filter(pair -> pair.getSecond().getOpenGates().size() <= 2)
                .map(extension -> new SortOrExtend(extension.getSecond(), storage, minRate)).
                forEach(ForkJoinTask::fork);
    }

    public static IntArrayList unorientedExtension(final int[] pi, final int n, final int... positions) {
        Arrays.sort(positions);
        val extension = new IntArrayList(pi);
        for (var i = 0; i < positions.length; i++) {
            extension.beforeInsert(positions[i] + i, n + i);
        }
        extension.trimToSize();
        return extension;
    }
}
