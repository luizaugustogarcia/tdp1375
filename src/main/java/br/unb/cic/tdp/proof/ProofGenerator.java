package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.CommonOperations;
import br.unb.cic.tdp.base.Simplification;
import br.unb.cic.tdp.base.UnorientedConfiguration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import cern.colt.list.ByteArrayList;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Bytes;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.math3.util.Pair;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static br.unb.cic.tdp.base.CommonOperations.applyTransposition;
import static br.unb.cic.tdp.base.CommonOperations.*;
import static br.unb.cic.tdp.base.Simplification.*;
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

    private static Pair<Map<UnorientedConfiguration, List<Cycle>>, Map<Integer, List<UnorientedConfiguration>>> loadKnownSortings(
            final String file) throws IOException {
        final var knownSortings = new HashMap<UnorientedConfiguration, List<Cycle>>();

        Files.lines(Paths.get(file)).forEach(line -> {
            final var lineSplit = line.trim().split("->");
            if (lineSplit.length > 1) {
                final var spi = new MulticyclePermutation(lineSplit[0].split("#")[1]);
                knownSortings.put(new UnorientedConfiguration(spi, CANONICAL_PI[spi.getNumberOfSymbols()]),
                        Arrays.stream(lineSplit[1].split(";")).map(Cycle::new).collect(Collectors.toList()));
            }
        });

        return new Pair<>(knownSortings, knownSortings.keySet().stream().collect(Collectors.groupingBy(UnorientedConfiguration::hashCode)));
    }

    public static void verify(final UnorientedConfiguration config,
                              final Pair<Map<UnorientedConfiguration, List<Cycle>>, Map<Integer, List<UnorientedConfiguration>>> knownSortings,
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

    /*
     * Type 1 extension.
     */
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

    /*
     * Type 2 extension.
     */
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

    /*
     * Type 3 extension.
     */
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

    private static List<Cycle> getSorting(final UnorientedConfiguration config, final List<List<Float>> simplificationSpi,
                                          final Pair<Map<UnorientedConfiguration, List<Cycle>>, Map<Integer, List<UnorientedConfiguration>>> knownSortings) {
        final var simplifiedConfig = fromSignature(Simplification.signature(simplificationSpi));
        final var simplificationPi = simplificationSpi.stream().flatMap(Collection::stream)
                .sorted().collect(Collectors.toList());

        List<Cycle> sorting;
        if (knownSortings.getFirst().containsKey(config)) {
            final var equivalentConfig = knownSortings.getSecond().get(simplifiedConfig.hashCode())
                    .stream().filter(c -> c.equals(simplifiedConfig)).findFirst().get();
            final var equivalentSorting = knownSortings.getFirst().get(equivalentConfig);
            final var simplificationSorting = Simplification.simplificationSorting(equivalentConfig, equivalentSorting,
                    simplifiedConfig, simplificationPi);

            sorting = mimicSorting(simplificationPi, simplificationSorting);
        } else {
            sorting = subConfigurationHasSorting(simplifiedConfig, knownSortings);
            if (sorting != null) {
                sorting = mimicSorting(simplificationPi, simplificationSorting(simplifiedConfig, simplificationPi, sorting));
            }
        }

        if (sorting != null && !is11_8(config.getSpi(), config.getPi(), sorting)) {
            throw new RuntimeException("ERROR");
        }

        return sorting;
    }

    // TODO move to Simplification.java?
    private static List<List<Float>> simplificationSorting(final UnorientedConfiguration simplifiedConfig,
                                                           final List<Float> simplificationPi, final List<Cycle> sorting) {
        var pi = simplifiedConfig.getPi();
        var _simplificationPi = simplificationPi;

        final var simplificationSorting = new ArrayList<List<Float>>();

        for (final var rho : sorting) {
            final var finalPi = pi;
            final var fSimplificationPi = _simplificationPi;

            simplificationSorting.add(Bytes.asList(rho.getSymbols()).stream()
                    .map(s -> fSimplificationPi.get(finalPi.indexOf(s))).collect(Collectors.toList()));

            pi = CommonOperations.applyTransposition(pi, rho);
            _simplificationPi = Simplification.applyTransposition(_simplificationPi,
                    simplificationSorting.get(simplificationSorting.size() - 1));
        }

        return simplificationSorting;
    }

    private static List<Cycle> subConfigurationHasSorting(final UnorientedConfiguration config,
                                                          final Pair<Map<UnorientedConfiguration, List<Cycle>>,
                                                                  Map<Integer, List<UnorientedConfiguration>>> knownSortings) {
        for (int i = 3; i <= config.getSpi().size(); i++) {
            for (final var mu : combinations(config.getSpi(), i)) {
                final var _config = toConfiguration(mu.getVector(), config.getPi());
                if (_config.getNumberOfOpenGates() <= 2) {
                    if (knownSortings.getFirst().containsKey(_config)) {
                        final var equivalentConfig = knownSortings.getSecond().get(_config.hashCode())
                                .stream().filter(c -> c.equals(_config)).findFirst().get();

                        final var cycleIndex = createCycleIndex(mu.getVector(), config.getPi());

                        final var _pi = new ByteArrayList(equivalentConfig.getPi().size());
                        for (var j = 0; j < config.getPi().size(); j++) {
                            if (cycleIndex[config.getPi().getSymbols()[j]] != null)
                                _pi.add(config.getPi().getSymbols()[j]);
                        }

                        final var signature = new byte[_pi.elements().length];
                        final Map<Cycle, Byte> labels = new HashMap<>();
                        for (int k = 0; k < _pi.elements().length; k++) {
                            labels.computeIfAbsent(cycleIndex[_pi.elements()[k]], c -> (byte) (labels.size() + 1));
                            signature[k] = labels.get(cycleIndex[_pi.elements()[k]]);
                        }

                        final var matchedSignature = equivalentConfig.getEquivalentSignatures().stream().filter(s -> Arrays.equals(s.getSignature(), signature))
                                .findFirst().get();

                        var pi = matchedSignature.getPi();
                        var cPi = new Cycle(_pi);

                        final var result = new ArrayList<Cycle>();
                        for (final var rho : equivalentConfig.equivalentSorting(matchedSignature, knownSortings.getFirst().get(equivalentConfig))) {
                            final var _rho = new Cycle(cPi.get(pi.indexOf(rho.get(0))),
                                    cPi.get(pi.indexOf(rho.get(1))), cPi.get(pi.indexOf(rho.get(2))));
                            result.add(_rho);

                            pi = applyTransposition(pi, rho);
                            cPi = applyTransposition(cPi, _rho);
                        }
                        return result;
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
}