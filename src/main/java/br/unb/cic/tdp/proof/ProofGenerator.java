package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.base.Simplification;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import cern.colt.list.ByteArrayList;
import cern.colt.list.FloatArrayList;
import com.google.common.base.Preconditions;
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
import static br.unb.cic.tdp.base.Configuration.isOpenGate;
import static br.unb.cic.tdp.base.Configuration.signature;
import static br.unb.cic.tdp.base.Configuration.*;
import static br.unb.cic.tdp.base.Simplification.*;
import static br.unb.cic.tdp.permutation.PermutationGroups.computeProduct;

public class ProofGenerator {

    private static Set<Configuration> visitedConfigs = new HashSet<>();

    // mvn exec:java -Dexec.mainClass="br.unb.cic.tdp.proof.ProofVerifier" -Dexec.args=".\\proof\\"
    public static void main(String[] args) throws Exception {
        final var knownSortings = loadKnownSortings(args[0]);

        // oriented 5-cycle (extensions not leading to new oriented cycles)
        verify(new Pair<>(null, new Configuration(new MulticyclePermutation("(0,3,1,4,2)"), new Cycle("0,1,2,3,4"))),
                knownSortings, 0);

        // interleaving pair
        verify(new Pair<>(null, new Configuration(new MulticyclePermutation("(0,4,2)(1,5,3)"), new Cycle("0,1,2,3,4,5"))),
                knownSortings, 0);

        // intersecting pair
        verify(new Pair<>(null, new Configuration(new MulticyclePermutation("(0,3,1)(2,5,4)"), new Cycle("0,1,2,3,4,5"))),
                knownSortings, 0);
    }

    private static Pair<Map<Configuration, List<Cycle>>,
            Map<Integer, List<Configuration>>> loadKnownSortings(final String file) throws IOException {
        final var knownSortings = new HashMap<Configuration, List<Cycle>>();

        Files.lines(Paths.get(file)).forEach(line -> {
            final var lineSplit = line.trim().split("->");
            if (lineSplit.length > 1) {
                final var spi = new MulticyclePermutation(lineSplit[0].split("#")[1]);
                knownSortings.put(new Configuration(spi, CANONICAL_PI[spi.getNumberOfSymbols()]),
                        Arrays.stream(lineSplit[1].split(";")).map(Cycle::new).collect(Collectors.toList()));
            }
        });

        return new Pair<>(knownSortings, knownSortings.keySet().stream()
                .collect(Collectors.groupingBy(Configuration::hashCode)));
    }

    public static void verify(final Pair<String, Configuration> config,
                              final Pair<Map<Configuration, List<Cycle>>,
                                      Map<Integer, List<Configuration>>> knownSortings,
                              final int depth) throws IOException {
        if (visitedConfigs.contains(config.getSecond())) {
            return;
        }

        visitedConfigs.add(config.getSecond());

        final var sorting = getSorting(config.getSecond(), knownSortings);
        if (sorting != null) {
            final var out = new PrintStream(new File("proof\\" + config.getSecond().getCanonical().getSpi() + ".html"));
            out.println("<HTML><HEAD><TITLE></TITLE></HEAD><BODY><PRE>");
            out.println(config.getSecond().getSpi());
            out.println("HAS (11/8)-SEQUENCE");
            var spi = config.getSecond().getSpi();
            for (int i = 0; i < sorting.size(); i++) {
                final var rho = sorting.get(i);
                out.println(String.format("<p>%d: %s", i + 1, rho));
                out.println(spi = computeProduct(spi, rho.getInverse()));
            }
            out.println("</PRE></BODY></HTML>");
            return;
        }

        final var out = new PrintStream(new File("proof\\" + config.getSecond().getCanonical().getSpi() + ".html"));
        out.println("<HTML><HEAD><TITLE></TITLE></HEAD><BODY><PRE>");

        out.println(config.getSecond().getSpi());

        if (config.getSecond().isFull()) {
            if (isValid(config.getSecond())) {
                out.println("BAD SMALL COMPONENT");
            } else {
                out.println("INVALID CONFIGURATION");
            }
        }

        out.println("<p>THE EXTENSIONS ARE: ");

        if (config.getSecond().get3Norm() > 8) {
            throw new RuntimeException("ERROR");
        }

        for (final var extension : type1Extensions(config.getSecond())) {
            out.println("<p>" + extension.getFirst());
            out.println((getSorting(extension.getSecond(), knownSortings) != null ? "GOOD" : "BAD") + " EXTENSION");
            out.println(String.format("View extension <a href=\"%s.html\">%s</a>",
                    extension.getSecond().getCanonical().getSpi(), extension.getSecond().getCanonical().getSpi()));
            verify(extension, knownSortings, depth + 1);
        }

        for (final var extension : type2Extensions(config.getSecond())) {
            out.println("<p>" + extension.getFirst());
            out.println((getSorting(extension.getSecond(), knownSortings) != null ? "GOOD" : "BAD") + " EXTENSION");
            out.println(String.format("View extension <a href=\"%s.html\">%s</a>",
                    extension.getSecond().getCanonical().getSpi(), extension.getSecond().getCanonical().getSpi()));
            verify(extension, knownSortings, depth + 1);
        }

        for (final var extension : type3Extensions(config.getSecond())) {
            out.println("<p>" + extension.getFirst());
            out.println((getSorting(extension.getSecond(), knownSortings) != null ? "GOOD" : "BAD") + " EXTENSION");
            out.println(String.format("View extension <a href=\"%s.html\">%s</a>",
                    extension.getSecond().getCanonical().getSpi(), extension.getSecond().getCanonical().getSpi()));
            verify(extension, knownSortings, depth + 1);
        }

        out.println("</PRE></BODY></HTML>");
    }

    private static List<Cycle> getSorting(final Configuration config, final Pair<Map<Configuration, List<Cycle>>,
            Map<Integer, List<Configuration>>> knownSortings) {
        final var sorting = simplifications(toListOfListOfFloats(config.getSpi()), new HashSet<>())
                .stream().map(s -> getSorting(config, s, knownSortings)).filter(Objects::nonNull).findFirst();
        return sorting.orElse(null);
    }

    /*
     * Type 1 extension.
     */
    private static List<Pair<String, Configuration>> type1Extensions(final Configuration config) {
        final var result = new ArrayList<Pair<String, Configuration>>();

        final var newCycleLabel = (byte) (config.getSpi().size() + 1);

        final var signature = signature(config.getSpi(), config.getPi());

        for (int i = 0; i < signature.length; i++) {
            if (isOpenGate(i + 1, signature)) {
                final var a = (i + 1) % signature.length;
                for (int b = 0; b < signature.length; b++) {
                    for (int c = b; c < signature.length; c++) {
                        if (!(a == b && b == c)) {
                            result.add(new Pair<>(String.format("Type 2, a=%d b=%d c=%d", a, b, c),
                                    fromSignature(extend(signature, newCycleLabel, a, b, c).elements())));
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
    private static List<Pair<String, Configuration>> type2Extensions(final Configuration config) {
        if (!config.isFull()) {
            return Collections.emptyList();
        }

        final var result = new ArrayList<Pair<String, Configuration>>();

        final var newCycleLabel = (byte) (config.getSpi().size() + 1);

        final var signature = signature(config.getSpi(), config.getPi());

        for (int a = 0; a < signature.length; a++) {
            for (int b = a; b < signature.length; b++) {
                for (int c = b; c < signature.length; c++) {
                    if (!(a == b && b == c)) {
                        result.add(new Pair<>(String.format("Type 2, a=%d b=%d c=%d", a, b, c),
                                fromSignature(extend(signature, newCycleLabel, a, b, c).elements())));
                    }
                }
            }
        }

        return result;
    }

    /*
     * Type 3 extension.
     */
    private static List<Pair<String, Configuration>> type3Extensions(final Configuration config) {
        final var result = new ArrayList<Pair<String, Configuration>>();

        final var signature = signature(config.getSpi(), config.getPi());

        for (int label = 1; label <= config.getSpi().size(); label++) {
            if (!isOriented(signature, label)) {
                for (int a = 0; a < signature.length; a++) {
                    for (int b = a; b < signature.length; b++) {
                        final var extension = fromSignature(extend(signature, (byte) label, a, b).elements());
                        if (extension.getNumberOfOpenGates() <= 2) {
                            result.add(new Pair<>(String.format("Type 3, a=%d b=%d", a, b), extension));
                        }
                    }
                }
            }
        }

        return result;
    }

    private static boolean isOriented(float[] signature, int label) {
        for (float s : signature) {
            if (s % 1 > 0 && Math.floor(s) == label) {
                return true;
            }
        }
        return false;
    }

    private static List<Cycle> getSorting(final Configuration config, final List<List<Float>> simplificationSpi,
                                          final Pair<Map<Configuration, List<Cycle>>,
                                                  Map<Integer, List<Configuration>>> knownSortings) {
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
                sorting = mimicSorting(simplificationPi,
                        simplificationSorting(simplifiedConfig, sorting, simplificationPi));
            }
        }

        if (sorting != null && !is11_8(config.getSpi(), config.getPi(), sorting)) {
            throw new RuntimeException("ERROR");
        }

        return sorting;
    }

    private static List<Cycle> subConfigurationHasSorting(final Configuration config,
                                                          final Pair<Map<Configuration, List<Cycle>>,
                                                                  Map<Integer, List<Configuration>>> knownSortings) {
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

                        final var signature = new float[_pi.elements().length];
                        final Map<Cycle, Byte> labels = new HashMap<>();
                        for (int k = 0; k < _pi.elements().length; k++) {
                            labels.computeIfAbsent(cycleIndex[_pi.elements()[k]], c -> (byte) (labels.size() + 1));
                            signature[k] = labels.get(cycleIndex[_pi.elements()[k]]);
                        }

                        final var matchedSignature = equivalentConfig.getEquivalentSignatures()
                                .stream().filter(s -> Arrays.equals(s.getSignature(), signature)).findFirst().get();

                        var pi = matchedSignature.getPi();
                        var cPi = new Cycle(_pi);

                        final var result = new ArrayList<Cycle>();
                        for (final var rho : equivalentConfig.equivalentSorting(matchedSignature,
                                knownSortings.getFirst().get(equivalentConfig))) {
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

    private static Configuration toConfiguration(final List<Cycle> spi, final Cycle pi) {
        final var cycleIndex = createCycleIndex(spi, pi);

        final var labelMap = new HashMap<Cycle, Byte>();

        for (final var cycle : spi) {
            labelMap.computeIfAbsent(cycle, c -> (byte) (labelMap.size() + 1));
        }

        final var _pi = new FloatArrayList(spi.stream().mapToInt(Cycle::size).sum());
        for (int i = 0; i < pi.size(); i++) {
            if (cycleIndex[pi.get(i)] != null)
                _pi.add(labelMap.get(cycleIndex[pi.get(i)]));
        }

        return fromSignature(_pi.elements());
    }

    private static boolean isValid(final Configuration config) {
        final var sigma = computeProduct(config.getSpi(), config.getPi());
        return sigma.size() == 1 && sigma.stream().findFirst().get().size() == config.getPi().size();
    }

    private static FloatArrayList extend(final float[] signature, final byte label, final int... positions) {
        Preconditions.checkArgument(1 < positions.length && positions.length <= 3);
        Arrays.sort(positions);
        final var extension = new FloatArrayList(signature);
        for (int i = 0; i < positions.length; i++) {
            extension.beforeInsert(positions[i] + i, label);
        }
        extension.trimToSize();
        return extension;
    }
}