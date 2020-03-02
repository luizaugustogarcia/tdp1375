package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.permutation.PermutationGroups;
import cern.colt.list.ByteArrayList;
import cern.colt.list.FloatArrayList;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.primitives.Floats;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static br.unb.cic.tdp.CommonOperations.*;
import static br.unb.cic.tdp.permutation.PermutationGroups.computeProduct;
import static java.util.stream.Collectors.toCollection;

public class ProofGenerator {

    private static PrintStream out;
    private static Set<Configuration> visitedConfigs = new HashSet<>();

    // mvn exec:java -Dexec.mainClass="br.unb.cic.tdp.proof.ProofVerifier" -Dexec.args=".\\proof\\"
    public static void main(String[] args) throws Exception {
        out = new PrintStream(new File("output.txt"));

        final var knownSortings = loadKnownSortings(args[0]);

        // interleaving pair
        verify(new Configuration(new MulticyclePermutation("(0,4,2)(1,5,3)"), new Cycle("0,1,2,3,4,5")),
                knownSortings, 0);

        // intersecting pair
        verify(new Configuration(new MulticyclePermutation("(0,3,1)(2,5,4)"), new Cycle("0,1,2,3,4,5")),
                knownSortings, 0);
    }

    private static Set<Configuration> loadKnownSortings(final String proofFolder) {
        return Stream.of("(0,3,1)(2,5,4)", //
                "(0,4,2)(1,5,3)",  //
                "bad-small-(0,10,2)(1,5,3)(4,8,6)(7,11,9)", //
                "bad-small-(0,16,2)(1,5,3)(4,8,6)(7,11,9)(10,14,12)(13,17,15)", //
                "bad-small-(0,4,2)(1,14,12)(3,7,5)(6,10,8)(9,13,11)", //
                "bad-small-(0,4,2)(1,5,3)", "bad-small-(0,7,5)(1,11,9)(2,6,4)(3,10,8)")
                .flatMap(file -> toStreamOfLines(proofFolder + file))
                .map(line -> readConfiguration(line))
                .collect(Collectors.toSet());
    }

    private static Configuration readConfiguration(final String line) {
        final var lineSplit = line.trim().split("=>");
        final var spi = new MulticyclePermutation(lineSplit[0]);
        final var pi = CANONICAL_PI[spi.getNumberOfSymbols()];
        return new Configuration(spi, pi);
    }

    private static Stream<String> toStreamOfLines(final String file) {
        try {
            return Files.lines(Paths.get(file));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void verify(final Configuration config, final Set<Configuration> knownSortings,
                              final int depth) {
        out.print("\n" + StringUtils.repeat("\t", depth) + config.hashCode() + "#" + config.getSpi().toString());

        if (visitedConfigs.contains(config)) {
            out.print(" -> ALREADY VISITED");
            return;
        }

        if (toConfiguration(config).anyMatch(simplification -> hasKnownSorting(simplification, knownSortings)) ||
                isThere11_8Seq(config)) {
            out.print(" -> HAS SORTING");
            knownSortings.add(config);
            return;
        }

        if (config.isFull()) {
            if (isValid(config)) {
                out.print(" -> BAD SMALL COMPONENT");
            } else {
                out.print(" -> INVALID");
            }
        }

        final var spi = config.getSpi();

        if (spi.get3Norm() > 8) {
            out.print(" -> SORTING NOT FOUND");
            throw new RuntimeException("ERROR");
        }

        for (final var extension : type1Extensions(config)) {
            verify(extension, knownSortings, depth + 1);
        }

        for (final var extension : type2Extensions(config)) {
            verify(extension, knownSortings, depth + 1);
        }

        for (final var extension : type3Extensions(config)) {
            verify(extension, knownSortings, depth + 1);
        }

        visitedConfigs.add(config);
    }

    private static boolean isThere11_8Seq(final Configuration config) {
        return !searchFor11_8SortingSeq(config.getSpi(), config.getPi()).isEmpty();
    }

    private static boolean hasKnownSorting(final Configuration config, final Set<Configuration> knownSortings) {
        return knownSortings.contains(config) || subConfigurationHasSorting(config, knownSortings);
    }

    public static Stream<Configuration> toConfiguration(final Configuration config) {
        if (isSimple(config.getSpi())) {
            return Stream.of(config);
        }

        final List<List<Float>> _spi = config.getSpi().stream()
                .map(cycle -> toListOfFloatsReversed(cycle))
                .collect(Collectors.toList());

        return _spi.stream().filter(cycle -> cycle.size() > 3).flatMap(longCycle ->
                IntStream.range(0, longCycle.size()).boxed()
                        .map(index -> toConfiguration(_spi, longCycle, index)));
    }

    private static Configuration toConfiguration(final List<List<Float>> spi, final List<Float> longCycle, final Integer startIndex) {
        final var _spi = new ArrayList<>(spi);
        final var newCycles = new ArrayList<List<Float>>();

        _spi.remove(longCycle);

        for (int j = 0; j < Math.ceil((double) longCycle.size() / 3); j++) {
            final var head = new ArrayList<Float>();
            for (int k = 0; k < (j == 0 ? 3 : 2); k++) {
                head.add(longCycle.get((k + startIndex + j * 3) % longCycle.size()));
            }
            if (j != 0) {
                head.add(0, longCycle.get((startIndex + j * 3 - 1) % longCycle.size()) + 0.01F);
            }
            newCycles.add(new ArrayList<>(head));
        }

        _spi.addAll(newCycles);

        final var _pi = _spi.stream().flatMap(Collection::stream)
                .sorted().collect(Collectors.toList());
        final var __pi = new ByteArrayList(_pi.size());


        final var substitution = new HashMap<Float, Byte>();
        for (int j = 0; j < _pi.size(); j++) {
            substitution.put(_pi.get(j), (byte) j);
            __pi.add((byte) j);
        }

        return toConfiguration(_spi.stream().map(c -> {
            Collections.reverse(c);
            final var byteList = new ByteArrayList();
            for (final var symbol : c) {
                byteList.add(substitution.get(symbol));
            }
            return new Cycle(byteList);
        }).collect(Collectors.toList()), new Cycle(__pi));
    }

    private static List<Float> toListOfFloatsReversed(final Cycle cycle) {
        final var floatList = new FloatArrayList(cycle.size());
        for (final var symbol : cycle.getSymbols()) {
            floatList.add(symbol);
        }
        floatList.reverse();
        return new ArrayList<>(Floats.asList(floatList.elements()));
    }

    private static boolean isSimple(final MulticyclePermutation spi) {
        return spi.stream().noneMatch(c -> c.size() > 3);
    }

    private static boolean subConfigurationHasSorting(final Configuration config, final Set<Configuration> knownSortings) {
        for (int i = 2; i <= config.getSpi().size(); i++) {
            for (final var mu : combinations(config.getSpi(), i)) {
                final var _configuration = toConfiguration(mu.getVector(), config.getPi());
                if (_configuration.numberOfOpenGates() <= 2) {
                    if (knownSortings.contains(_configuration)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static Configuration toConfiguration(final List<Cycle> _spi, final Cycle pi) {
        final var cycleIndex = createCycleIndex(_spi, pi);

        final var labelMap = new HashMap<Cycle, Byte>();

        for (final var cycle : _spi) {
            labelMap.computeIfAbsent(cycle, c -> (byte) (labelMap.size() + 1));
        }

        final var _pi = new ByteArrayList(_spi.stream().mapToInt(Cycle::size).sum());
        for (int i = 0; i < pi.size(); i++) {
            if (cycleIndex[pi.get(i)] != null)
                _pi.add(labelMap.get(cycleIndex[pi.get(i)]));
        }

        return Configuration.fromSignature(_pi.elements());
    }

    private static boolean isValid(final Configuration config) {
        final var sigma = computeProduct(config.getSpi(), config.getPi());
        return sigma.size() == 1 && sigma.stream().findFirst().get().size() == config.getPi().size();
    }

    private static Configuration toConfiguration(final ByteArrayList extension) {
        final var cyclesByLabel = new HashMap<Byte, ByteArrayList>();
        for (int j = extension.size() - 1; j >= 0; j--) {
            final var label = extension.get(j);
            cyclesByLabel.computeIfAbsent(label, l -> new ByteArrayList());
            cyclesByLabel.get(label).add((byte) j);
        }

        final var spi = cyclesByLabel.values().stream().map(Cycle::new)
                .collect(toCollection(MulticyclePermutation::new));

        return new Configuration(spi, CANONICAL_PI[spi.getNumberOfSymbols()]);
    }

    private static ByteArrayList extend(final byte[] signature, final byte label, final int... positions) {
        Preconditions.checkArgument(1 < positions.length && positions.length <= 3);
        Arrays.sort(positions);
        final var extension = new ByteArrayList(signature);
        for (int i = 0; i < positions.length; i++) {
            extension.beforeInsert(positions[i] + i, label);
        }
        return extension;
    }

    public static List<Cycle> searchFor11_8SortingSeq(final MulticyclePermutation spi, final Cycle pi) {
        final var executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        final var completionService = new ExecutorCompletionService<List<Cycle>>(executorService);

        final var spiCycleIndex = createCycleIndex(spi, pi);
        final var iterator = generateAll0_2Moves(pi, spiCycleIndex).iterator();

        final var submittedTasks = new ArrayList<Future<List<Cycle>>>();
        while (iterator.hasNext()) {
            final var move = iterator.next();
            final var rho = move.getKey();
            final var _partialSorting = new Stack<Cycle>();
            _partialSorting.push(rho);
            submittedTasks.add(completionService.submit(() -> searchForSortingSeq(applyTransposition(pi, rho),
                    PermutationGroups.computeProduct(spi, rho.getInverse()), _partialSorting,
                    spi.getNumberOfEvenCycles(), 1.375F)));
        }

        executorService.shutdown();

        List<Cycle> sorting = Collections.emptyList();
        try {
            for (int i = 0; i < submittedTasks.size(); i++) {
                final var next = completionService.take();
                if (next.get().size() > 1 || next.get().size() == 1 && is11_8(spi, pi, next.get())) {
                    sorting = next.get();
                    break;
                }
            }
        } catch (final Exception e) {
            Throwables.propagate(e);
        }

        executorService.shutdownNow();

        return sorting;
    }

    private static Set<Configuration> type1Extensions(final Configuration config) {
        final var result = new HashSet<Configuration>();

        final var newCycleLabel = (byte) (config.getSpi().size() + 1);

        final var signature = signature(config.getSpi(), config.getPi());

        for (int i = 0; i < signature.length; i++) {
            if (signature[i] == signature[(i + 1) % signature.length]) {
                final var a = (i + 1) % signature.length;
                for (int b = 0; b < signature.length; b++) {
                    for (int c = b; c < signature.length; c++) {
                        if (!(a == b && b == c)) {
                            result.add(toConfiguration(extend(signature, newCycleLabel, a, b, c)));
                        }
                    }
                }
            }
        }

        return result;
    }

    private static Set<Configuration> type2Extensions(final Configuration config) {
        if (!config.isFull()) {
            return Collections.emptySet();
        }

        final var result = new HashSet<Configuration>();

        final var newCycleLabel = (byte) (config.getSpi().size() + 1);

        final var signature = signature(config.getSpi(), config.getPi());

        for (int a = 0; a < signature.length; a++) {
            for (int b = a; b < signature.length; b++) {
                for (int c = b; c < signature.length; c++) {
                    if (!(a == b && b == c)) {
                        result.add(toConfiguration(extend(signature, newCycleLabel, a, b, c)));
                    }
                }
            }
        }

        return result;
    }

    private static Set<Configuration> type3Extensions(final Configuration config) {
        final var result = new HashSet<Configuration>();

        final var signature = signature(config.getSpi(), config.getPi());

        for (int label = 1; label <= config.getSpi().size(); label++) {
            for (int a = 0; a < signature.length; a++) {
                for (int b = a; b < signature.length; b++) {
                    final var extension = toConfiguration(extend(signature, (byte) label, a, b));
                    if (extension.numberOfOpenGates() <= 2) {
                        result.add(extension);
                    }
                }
            }
        }

        return result;
    }
}