package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.util.Pair;
import lombok.SneakyThrows;
import lombok.val;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;

import static br.unb.cic.tdp.base.CommonOperations.cycleIndex;
import static br.unb.cic.tdp.base.Configuration.signature;
import static br.unb.cic.tdp.proof.SortOrExtend.*;
import static java.util.stream.Stream.concat;

public class TwoCycles {

    @SneakyThrows
    public static void main(String[] args) {
        val pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
        pool.execute(new TwoCyclesSortOrExtend(new Configuration("(0 1)"), new MySQLProofStorage("192.168.68.114", "luiz", "luiz"), 1.6));
        pool.shutdown();
        // boundless
        pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }

    private static class TwoCyclesSortOrExtend extends AbstractSortOrExtend {

        public TwoCyclesSortOrExtend(final Configuration configuration, final ProofStorage storage, final double minRate) {
            super(configuration, storage, minRate);
        }

        @Override
        protected void extend(Configuration configuration) {
            concat(type1Extensions(configuration).stream(),
                    concat(type2Extensions(configuration).stream(),
                            type3Extensions(configuration).stream()))
                    .map(Pair::getSecond)
                    .map(extension -> new TwoCyclesSortOrExtend(extension, storage, minRate))
                    .forEach(ForkJoinTask::fork);
        }

        /*
         * Type 1 extension.
         */
        private static List<Pair<String, Configuration>> type1Extensions(final Configuration configuration) {
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

            for (var i = 0; i < n; i++) {
                if (configuration.getOpenGates().contains(i)) {
                    for (var b = 0; b < n; b++) {
                        if (i != b) {
                            val newCycle = "(" + n + " " + (n + 1) + ")";
                            val extendedPi = unorientedExtension(configuration.getPi().getSymbols(), n, i, b).elements();
                            val extension = new Configuration(new MulticyclePermutation(configuration.getSpi() + newCycle), Cycle.of(extendedPi));
                            result.add(new Pair<>(String.format("a=%d b=%d", i, b), extension));
                        }
                    }
                }
            }

            return result;
        }

        /*
         * Type 2 extension.
         */
        private static List<Pair<String, Configuration>> type2Extensions(final Configuration config) {
            if (!config.isFull()) {
                return Collections.emptyList();
            }

            val result = new ArrayList<Pair<String, Configuration>>();

            val n = config.getPi().getSymbols().length;

            for (var a = 0; a < n; a++) {
                for (var b = a; b < n; b++) {
                    for (var c = b; c < n; c++) {
                        if (!(a == b)) {
                            val newCycle = "(" + n + " " + (n + 2) + " " + (n + 1) + ")";
                            val extendedPi = unorientedExtension(config.getPi().getSymbols(), n, a, b, c).elements();
                            val extension = new Configuration(new MulticyclePermutation(config.getSpi() + newCycle), Cycle.of(extendedPi));
                            result.add(new Pair<>(String.format("a=%d b=%d c=%d", a, b, c), extension));
                        }
                    }
                }
            }

            for (var a = 0; a < n; a++) {
                for (var b = a; b < n; b++) {
                    if (a != b) {
                        val newCycle = "(" + n + " " + (n + 1) + ")";
                        val extendedPi = unorientedExtension(config.getPi().getSymbols(), n, a, b).elements();
                        val extension = new Configuration(new MulticyclePermutation(config.getSpi() + newCycle), Cycle.of(extendedPi));
                        result.add(new Pair<>(String.format("a=%d b=%d", a, b), extension));
                    }
                }
            }

            return result;
        }
    }

    /*
     * Type 3 extension.
     */
    private static List<Pair<String, Configuration>> type3Extensions(final Configuration config) {
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
            if (cycle.size() > 2) { // only 3-cycles or longer are extended
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
}