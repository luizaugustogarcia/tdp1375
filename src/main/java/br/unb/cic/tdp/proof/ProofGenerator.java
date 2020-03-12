package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.CommonOperations;
import br.unb.cic.tdp.base.UnorientedConfiguration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.permutation.PermutationGroups;
import cern.colt.list.ByteArrayList;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Floats;
import org.apache.commons.lang.ArrayUtils;
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

import static br.unb.cic.tdp.base.CommonOperations.*;
import static br.unb.cic.tdp.base.Simplification.signature;
import static br.unb.cic.tdp.base.Simplification.simplifications;
import static br.unb.cic.tdp.base.UnorientedConfiguration.fromSignature;
import static br.unb.cic.tdp.base.UnorientedConfiguration.signature;
import static br.unb.cic.tdp.permutation.PermutationGroups.computeProduct;
import static java.util.stream.Collectors.toCollection;

public class ProofGenerator {

    private static PrintStream out;
    private static Set<UnorientedConfiguration> visitedConfigs = new HashSet<>();

    // mvn exec:java -Dexec.mainClass="br.unb.cic.tdp.proof.ProofVerifier" -Dexec.args=".\\proof\\"
    public static void main(String[] args) throws Exception {
        out = new PrintStream(new File("output.txt"));

        final var knownSortings = loadKnownSortings(args[0]);

        // interleaving pair
        verify(new UnorientedConfiguration(new MulticyclePermutation("(0,4,2)(1,5,3)"), new Cycle("0,1,2,3,4,5")),
                knownSortings, 0);

        // intersecting pair
        verify(new UnorientedConfiguration(new MulticyclePermutation("(0,3,1)(2,5,4)"), new Cycle("0,1,2,3,4,5")),
                knownSortings, 0);
    }

    private static Map<UnorientedConfiguration, List<Cycle>> loadKnownSortings(final String file) throws IOException {
        final var knownSortings = new HashMap<UnorientedConfiguration, List<Cycle>>();

        Files.lines(Paths.get(file)).forEach(line -> {
            final var lineSplit = line.trim().split("->");
            if (lineSplit.length > 1) {
                final var spi = new MulticyclePermutation(lineSplit[0].split("#")[1]);
                knownSortings.put(new UnorientedConfiguration(spi, CANONICAL_PI[spi.getNumberOfSymbols()]),
                        Arrays.stream(lineSplit[1].split(";")).map(Cycle::new).collect(Collectors.toList()));
            }
        });

        return knownSortings;
    }

    public static void verify(final UnorientedConfiguration config, final Map<UnorientedConfiguration, List<Cycle>> knownSortings,
                              final int depth) {
        out.print("\n" + StringUtils.repeat("\t", depth) + config.hashCode() + "#" + config.getSpi().toString());

        if (visitedConfigs.contains(config)) {
            out.print(" -> ALREADY VISITED");
            return;
        }

        visitedConfigs.add(config);

        final var sorting = simplifications(toListOfListOfFloats(config.getSpi()), new HashSet<>())
                .stream().map(s -> getSorting(config, s, knownSortings)).filter(Objects::nonNull).findFirst();
        if (sorting.isPresent()) {
            out.print(" -> HAS SORTING: " + sorting.get());
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
    }

    private static List<Cycle> getSorting(final UnorientedConfiguration config, final List<List<Float>> simplificationSpi,
                                          final Map<UnorientedConfiguration, List<Cycle>> knownSortings) {
        final var simplifiedConfig = fromSignature(signature(simplificationSpi));

        if (knownSortings.containsKey(simplifiedConfig)) {
            final var simplificationPi = simplificationSpi.stream().flatMap(Collection::stream)
                    .sorted().collect(Collectors.toList());
            final var entry = knownSortings.entrySet().stream().filter(e -> e.getKey().equals(simplifiedConfig)).findFirst().get();
            final var simplificationSorting = getSimplificationSorting(simplificationPi,
                    simplifiedConfig, entry.getKey(), entry.getValue());
            final var sorting = mimicSorting(simplificationPi, simplificationSorting);
            if (!is11_8(config.getSpi(), sorting)) {
                throw new RuntimeException("ERROR");
            }
            return sorting;
        } else {
            final List<Cycle> sorting;
            if ((sorting = subConfigurationHasSorting(simplifiedConfig, knownSortings)) != null) {
                return sorting;
            }
        }

        return null;
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
            submittedTasks.add(completionService.submit(() -> searchForSortingSeq(CommonOperations.applyTransposition(pi, rho),
                    PermutationGroups.computeProduct(spi, rho.getInverse()), _partialSorting,
                    spi.getNumberOfEvenCycles(), 1.375F)));
        }

        executorService.shutdown();

        List<Cycle> sorting = Collections.emptyList();
        try {
            for (int i = 0; i < submittedTasks.size(); i++) {
                final var next = completionService.take();
                if (next.get().size() > 1 || next.get().size() == 1 && CommonOperations.is11_8(spi, pi, next.get())) {
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

    public static boolean is11_8(MulticyclePermutation spi, final List<Cycle> rhos) {
        final var before = spi.getNumberOfEvenCycles();
        for (final var rho : rhos) {
            spi = PermutationGroups.computeProduct(spi, rho.getInverse());
        }
        final var after = spi.getNumberOfEvenCycles();
        return after > before && (float) rhos.size() / ((after - before) / 2) <= ((float) 11 / 8);
    }

    private static List<Cycle> mimicSorting(final List<Float> simplificationPi,
                                            final List<List<Float>> simplificationSorting) {
        final var sorting = new ArrayList<Cycle>();

        final var omegas = new ArrayList<List<Float>>();
        omegas.add(simplificationPi);

        for (final var rho : simplificationSorting) {
            omegas.add(applyTransposition(omegas.get(omegas.size() - 1), rho));
        }

        for (int i = 1; i < omegas.size(); i++) {
            final var pi = removeFractions(omegas.get(i - 1));
            final var _pi = removeFractions(omegas.get(i));
            final var rho = computeProduct(false, _pi, pi.getInverse());
            if (!rho.isIdentity()) {
                sorting.add(rho.asNCycle());
            }
        }

        return sorting;
    }

    private static Cycle removeFractions(final List<Float> pi) {
        final var it = pi.iterator();
        while (it.hasNext()) {
            final var symbol = it.next();
            if (symbol % 1 > 0) {
                it.remove();
            }
        }
        return new Cycle(Bytes.toArray(pi));
    }

    public static List<Float> applyTransposition(final List<Float> pi, final List<Float> rho) {
        final var a = rho.get(0);
        final var b = rho.get(1);
        final var c = rho.get(2);

        final var indexes = new int[3];
        for (var i = 0; i < pi.size(); i++) {
            if (pi.get(i).floatValue() == a)
                indexes[0] = i;
            if (pi.get(i).floatValue() == b)
                indexes[1] = i;
            if (pi.get(i).floatValue() == c)
                indexes[2] = i;
        }

        Arrays.sort(indexes);

        final var _pi = ArrayUtils.toPrimitive(pi.toArray(new Float[0]), 0.0F);
        final var result = new float[pi.size()];
        System.arraycopy(_pi, 0, result, 0, indexes[0]);
        System.arraycopy(_pi, indexes[1], result, indexes[0], indexes[2] - indexes[1]);
        System.arraycopy(_pi, indexes[0], result, indexes[0] + (indexes[2] - indexes[1]), indexes[1] - indexes[0]);
        System.arraycopy(_pi, indexes[2], result, indexes[2], pi.size() - indexes[2]);

        return new ArrayList<>(Floats.asList(result));
    }

    private static List<List<Float>> getSimplificationSorting(final List<Float> simplificationPi,
                                                              final UnorientedConfiguration simplifiedConfig,
                                                              final UnorientedConfiguration equivalentConfig,
                                                              List<Cycle> equivalentConfigSorting) {
        final var matchedSignature = equivalentConfig.getEquivalentSignatures().stream()
                .filter(c -> Arrays.equals(c.getSignature(), simplifiedConfig.getSignature().getSignature()))
                .findFirst().get();

        var equivalentSorting = equivalentConfigSorting;

        // this is the first code to compute the rho's properly
        final var sigma = computeProduct(equivalentConfig.getSpi(), equivalentConfig.getPi());

        if (matchedSignature.isMirror()) {
            var mirroredSorting = new ArrayList<Cycle>();
            var pi = equivalentConfig.getPi();
            for (final var rho : equivalentConfigSorting) {
                var _pi = computeProduct(rho, pi).asNCycle();
                mirroredSorting.add(
                        computeProduct(false, _pi.conjugateBy(sigma).getInverse(),
                                pi.conjugateBy(sigma)).asNCycle());
                pi = _pi;
            }
            equivalentSorting = mirroredSorting;
        }

        final var sorting = new ArrayList<List<Float>>();

        var pi = matchedSignature.getPi();
        var sPi = simplifiedConfig.getPi();
        var fPi = simplificationPi;

        for (final var rho : equivalentSorting) {
            final var finalPi = pi;
            final var finalSPi = sPi;
            final var finalFPi = fPi;

            final var _rho = new Cycle(sPi.get(finalPi.indexOf(rho.get(0))),
                    sPi.get(finalPi.indexOf(rho.get(1))), sPi.get(finalPi.indexOf(rho.get(2))));

            sorting.add(Bytes.asList(_rho.getSymbols()).stream()
                    .map(s -> finalFPi.get(finalSPi.indexOf(s))).collect(Collectors.toList()));

            pi = computeProduct(rho, pi).asNCycle();
            sPi = computeProduct(_rho, sPi).asNCycle();
            fPi = applyTransposition(fPi, sorting.get(sorting.size() - 1));
        }

        return sorting;
    }

    private static List<List<Float>> toListOfListOfFloats(final MulticyclePermutation spi) {
        return spi.stream().map(ProofGenerator::toFloatsList).collect(Collectors.toList());
    }

    private static List<Float> toFloatsList(final Cycle cycle) {
        final var floatList = new ArrayList<Float>(cycle.size());
        for (final var symbol : cycle.getSymbols()) {
            floatList.add((float) symbol);
        }
        return floatList;
    }

    private static List<Cycle> subConfigurationHasSorting(final UnorientedConfiguration config,
                                                          final Map<UnorientedConfiguration, List<Cycle>> knownSortings) {
        for (int i = 2; i <= config.getSpi().size(); i++) {
            for (final var mu : combinations(config.getSpi(), i)) {
                final var _configs = toConfiguration(mu.getVector(), config.getPi());
                if (_configs.getNumberOfOpenGates() <= 2) {
                    if (knownSortings.containsKey(_configs)) {
                        return knownSortings.get(_configs);
                    }
                }
            }
        }

        return null;
    }

    private static UnorientedConfiguration toConfiguration(final List<Cycle> _spi, final Cycle pi) {
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

        return fromSignature(_pi.elements());
    }

    private static boolean isValid(final UnorientedConfiguration config) {
        final var sigma = computeProduct(config.getSpi(), config.getPi());
        return sigma.size() == 1 && sigma.stream().findFirst().get().size() == config.getPi().size();
    }

    private static UnorientedConfiguration toConfiguration(final ByteArrayList extension) {
        final var cyclesByLabel = new HashMap<Byte, ByteArrayList>();
        for (int j = extension.size() - 1; j >= 0; j--) {
            final var label = extension.get(j);
            cyclesByLabel.computeIfAbsent(label, l -> new ByteArrayList());
            cyclesByLabel.get(label).add((byte) j);
        }

        final var spi = cyclesByLabel.values().stream().map(Cycle::new)
                .collect(toCollection(MulticyclePermutation::new));

        return new UnorientedConfiguration(spi, CANONICAL_PI[spi.getNumberOfSymbols()]);
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

    private static List<UnorientedConfiguration> type1Extensions(final UnorientedConfiguration config) {
        final var result = new ArrayList<UnorientedConfiguration>();

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

    private static List<UnorientedConfiguration> type2Extensions(final UnorientedConfiguration config) {
        if (!config.isFull()) {
            return Collections.emptyList();
        }

        final var result = new ArrayList<UnorientedConfiguration>();

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

    private static List<UnorientedConfiguration> type3Extensions(final UnorientedConfiguration config) {
        final var result = new ArrayList<UnorientedConfiguration>();

        final var signature = signature(config.getSpi(), config.getPi());

        for (int label = 1; label <= config.getSpi().size(); label++) {
            for (int a = 0; a < signature.length; a++) {
                for (int b = a; b < signature.length; b++) {
                    final var extension = toConfiguration(extend(signature, (byte) label, a, b));
                    if (extension.getNumberOfOpenGates() <= 2) {
                        result.add(extension);
                    }
                }
            }
        }

        return result;
    }
}