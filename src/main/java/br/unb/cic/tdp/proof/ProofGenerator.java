package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.CommonOperations;
import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.proof.seq11_8.Combinations;
import br.unb.cic.tdp.proof.seq11_8.Extensions;
import br.unb.cic.tdp.proof.seq11_8.OrientedCycleGreaterOrEquals7;
import cern.colt.list.IntArrayList;
import cern.colt.list.FloatArrayList;
import com.google.common.primitives.Floats;
import com.google.common.primitives.Ints;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.math3.util.Pair;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import java.io.IOException;
import java.io.Writer;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

import static br.unb.cic.tdp.base.CommonOperations.*;
import static br.unb.cic.tdp.base.Configuration.ofSignature;
import static br.unb.cic.tdp.permutation.PermutationGroups.computeProduct;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class ProofGenerator {

    // mvn exec:java -Dexec.mainClass="br.unb.cic.tdp.proof.ProofGenerator" -Dexec.args=".\\proof\\ true 8"
    public static void main(String[] args) throws IOException, URISyntaxException {
        Velocity.setProperty("resource.loader", "class");
        Velocity.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        Velocity.init();

        Files.createDirectories(Paths.get(args[0]));

        Files.copy(ProofGenerator.class.getClassLoader().getResourceAsStream("index.html"),
                Paths.get(args[0] + "/index.html"), REPLACE_EXISTING);
        Files.copy(ProofGenerator.class.getClassLoader().getResourceAsStream("explain.html"),
                Paths.get(args[0] + "/explain.html"), REPLACE_EXISTING);
        Files.copy(ProofGenerator.class.getClassLoader().getResourceAsStream("draw-config.js"),
                Paths.get(args[0] + "/draw-config.js"), REPLACE_EXISTING);

        OrientedCycleGreaterOrEquals7.generate(args[0]);

        final Map<String, String> env = new HashMap<>();
        final String[] fileUri = ProofGenerator.class.getClassLoader()
                .getResource("known-sortings").toURI().toString().split("!");
        //final FileSystem fs = FileSystems.newFileSystem(URI.create(fileUri[0]), env);
        //final Path path = fs.getPath(fileUri[1]);

        final Path path = Paths.get(ProofGenerator.class.getClassLoader()
                .getResource("known-sortings").toURI());

        // Known sortings found by Elias and Hartman
        final var knownSortings = loadKnownSortings(path);

        final var shouldAlsoUseBranchAndBound = args.length > 1 && args[1].equalsIgnoreCase("true");
        if (args.length > 2)
            numberOfThreads = Integer.parseInt(args[2]);

        Extensions.generate(knownSortings, shouldAlsoUseBranchAndBound, args[0]);
        Combinations.generate(knownSortings, shouldAlsoUseBranchAndBound, args[0]);
    }

    private static Pair<Map<Configuration, List<Cycle>>,
            Map<Integer, List<Configuration>>> loadKnownSortings(final Path file) throws IOException {
        final var knownSortings = new HashMap<Configuration, List<Cycle>>();

        Files.lines(file).forEach(line -> {
            final var lineSplit = line.trim().split("->");
            if (lineSplit.length > 1) {
                final var spi = new MulticyclePermutation(lineSplit[0].split("#")[1]);
                knownSortings.put(new Configuration(spi),
                        Arrays.stream(lineSplit[1].split(";")).map(s -> Cycle.create(s)).collect(Collectors.toList()));
            }
        });

        return new Pair<>(knownSortings, knownSortings.keySet().stream()
                .collect(Collectors.groupingBy(Configuration::hashCode)));
    }

    public static Optional<List<Cycle>> searchForSorting(final Configuration config, final Pair<Map<Configuration, List<Cycle>>,
            Map<Integer, List<Configuration>>> knownSortings, final boolean shouldAlsoUseBranchAndBound) {
        if (knownSortings.getFirst().containsKey(config)) {
            // can be empty if it was branch and bound sorted
            if (knownSortings.getFirst().get(config).isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(config.translatedSorting(knownSortings.getSecond().get(config.hashCode()).stream()
                    .filter(c -> c.equals(config)).findFirst().get(), knownSortings.getFirst().get(config)));
        }

        var sorting = simplifications(toListOfListOfFloats(config.getSpi()), new HashSet<>())
                .stream().map(s -> searchForSorting(config, s, knownSortings)).filter(Objects::nonNull).findFirst();

        if (sorting.isPresent()) {
            return Optional.of(sorting.get());
        }

        if (shouldAlsoUseBranchAndBound && config.getSpi().stream().anyMatch(Cycle::isLong)) {
            final var _sorting = searchFor11_8SeqParallel(config.getSpi(), config.getPi());
            knownSortings.getFirst().put(config, _sorting);
            knownSortings.getSecond().computeIfAbsent(config.hashCode(), key -> new ArrayList<>());
            knownSortings.getSecond().computeIfPresent(config.hashCode(), (key, value) -> {
                value.add(config);
                return value;
            });

            if (!_sorting.isEmpty())
                return Optional.of(_sorting);
        }

        return Optional.empty();
    }

    private static List<Cycle> searchForSorting(final Configuration config, final List<List<Float>> simplificationSpi,
                                                final Pair<Map<Configuration, List<Cycle>>,
                                                        Map<Integer, List<Configuration>>> knownSortings) {
        final var simplifiedConfig = ofSignature(signature(simplificationSpi));
        final var simplificationPi = simplificationSpi.stream().flatMap(Collection::stream)
                .sorted().collect(Collectors.toList());

        List<Cycle> sorting;
        if (knownSortings.getFirst().containsKey(config)) {
            final var equivalentConfig = knownSortings.getSecond().get(simplifiedConfig.hashCode())
                    .stream().filter(c -> c.equals(simplifiedConfig)).findFirst().get();
            final var equivalentSorting = knownSortings.getFirst().get(equivalentConfig);
            final var simplificationSorting = simplificationSorting(equivalentConfig, equivalentSorting,
                    simplifiedConfig, simplificationPi);

            sorting = mimicSorting(simplificationPi, simplificationSorting);
        } else {
            final var _sorting = searchForSortingSubConfiguration(simplifiedConfig, knownSortings);
            if (_sorting.isPresent()) {
                sorting = mimicSorting(simplificationPi,
                        simplificationSorting(simplifiedConfig, _sorting.get(), simplificationPi));
            } else {
                sorting = null;
            }
        }

        if (sorting != null) {
            assert is11_8(config.getSpi(), config.getPi(), sorting) : "ERROR";
        }

        return sorting;
    }

    private static Optional<List<Cycle>> searchForSortingSubConfiguration(final Configuration config,
                                                                          final Pair<Map<Configuration, List<Cycle>>,
                                                                          Map<Integer, List<Configuration>>> knownSortings) {
        for (int i = 3; i <= config.getSpi().size(); i++) {
            for (final var mu : combinations(config.getSpi(), i)) {
                final var _config = toConfiguration(mu.getVector(), config.getPi());
                if (_config.getNumberOfOpenGates() <= 2) {
                    if (knownSortings.getFirst().containsKey(_config)) {
                        final var equivalentConfig = knownSortings.getSecond().get(_config.hashCode())
                                .stream().filter(c -> c.equals(_config)).findFirst().get();

                        final var cycleIndex = cycleIndex(mu.getVector(), config.getPi());

                        final var _pi = new IntArrayList(equivalentConfig.getPi().size());
                        for (var j = 0; j < config.getPi().size(); j++) {
                            if (cycleIndex[config.getPi().getSymbols()[j]] != null)
                                _pi.add(config.getPi().getSymbols()[j]);
                        }

                        final var signature = new float[_pi.elements().length];
                        final Map<Cycle, Integer> labels = new HashMap<>();
                        for (int k = 0; k < _pi.elements().length; k++) {
                            labels.computeIfAbsent(cycleIndex[_pi.elements()[k]], c -> (int) (labels.size() + 1));
                            signature[k] = labels.get(cycleIndex[_pi.elements()[k]]);
                        }

                        final var matchedSignature = equivalentConfig.getEquivalentSignatures()
                                .stream().filter(s -> Arrays.equals(s.getContent(), signature)).findFirst().get();

                        var pi = matchedSignature.getPi();
                        var cPi = Cycle.create(_pi);

                        final var result = new ArrayList<Cycle>();
                        for (final var move : equivalentConfig.equivalentSorting(matchedSignature,
                                knownSortings.getFirst().get(equivalentConfig))) {
                            final var _move = Cycle.create(cPi.get(pi.indexOf(move.get(0))),
                                    cPi.get(pi.indexOf(move.get(1))), cPi.get(pi.indexOf(move.get(2))));
                            result.add(_move);

                            pi = CommonOperations.applyTransposition(pi, move);
                            cPi = CommonOperations.applyTransposition(cPi, _move);
                        }

                        return Optional.of(result);
                    }
                }
            }
        }

        return Optional.empty();
    }

    private static Configuration toConfiguration(final List<Cycle> spi, final Cycle pi) {
        final var cycleIndex = cycleIndex(spi, pi);

        final var labelMap = new HashMap<Cycle, Integer>();

        for (final var cycle : spi) {
            labelMap.computeIfAbsent(cycle, c -> (int) (labelMap.size() + 1));
        }

        final var _pi = new FloatArrayList(spi.stream().mapToInt(Cycle::size).sum());
        for (int i = 0; i < pi.size(); i++) {
            if (cycleIndex[pi.get(i)] != null)
                _pi.add(labelMap.get(cycleIndex[pi.get(i)]));
        }

        return ofSignature(_pi.elements());
    }

    public static String permutationToJsArray(final MulticyclePermutation permutation) {
        return "[" + permutation
                .stream().map(c -> "[" + Ints.asList(c.getSymbols()).stream()
                        .map(s -> Integer.toString(s))
                        .collect(Collectors.joining(",")) + "]")
                .collect(Collectors.joining(",")) + "]";
    }

    private static String cycleToJsArray(final Cycle cycle) {
        return "[" + Ints.asList(cycle.getSymbols()).stream()
                .map(s -> Integer.toString(s))
                .collect(Collectors.joining(",")) + "]";
    }

    public static void renderSorting(final Configuration canonicalConfig, final List<Cycle> sorting, final Writer writer) {
        VelocityContext context = new VelocityContext();

        context.put("spi", canonicalConfig.getSpi());
        context.put("piSize", canonicalConfig.getPi().size());
        context.put("jsSpi", permutationToJsArray(canonicalConfig.getSpi()));
        context.put("jsPi", cycleToJsArray(canonicalConfig.getPi()));
        context.put("sorting", sorting);

        final var spis = new ArrayList<MulticyclePermutation>();
        final var jsSpis = new ArrayList<String>();
        final var jsPis = new ArrayList<String>();
        var spi = canonicalConfig.getSpi();
        var pi = canonicalConfig.getPi();
        for (int i = 0; i < sorting.size(); i++) {
            final var move = sorting.get(i);
            spis.add(spi = computeProduct(spi, move.getInverse()));
            jsSpis.add(permutationToJsArray(spi));
            jsPis.add(cycleToJsArray(pi = computeProduct(move, pi).asNCycle()));
        }
        context.put("spis", spis);
        context.put("jsSpis", jsSpis);
        context.put("jsPis", jsPis);

        final var template = Velocity.getTemplate("templates/sorting.html");
        template.merge(context, writer);
    }

    private static float[] signature(final List<List<Float>> spi) {
        final var labelMap = new HashMap<List<Float>, Integer>();

        final var floatCyclesIndex = new TreeMap<Float, List<Float>>();
        for (final var cycle : spi) {
            for (final float symbol : cycle) {
                floatCyclesIndex.put(symbol, cycle);
            }
        }

        final var pi = floatCyclesIndex.keySet().stream().collect(Collectors.toList());

        final var signature = new float[floatCyclesIndex.size()];

        for (var i = 0; i < signature.length; i++) {
            final var symbol = pi.get(i);
            final var cycle = floatCyclesIndex.get(symbol);
            labelMap.computeIfAbsent(cycle, c -> (int) (labelMap.size() + 1));
            signature[i] = labelMap.get(cycle);
        }

        return signature;
    }

    private static List<Cycle> mimicSorting(final List<Float> simplificationPi,
                                           final List<List<Float>> simplificationSorting) {
        final var sorting = new ArrayList<Cycle>();

        final var omegas = new ArrayList<List<Float>>();
        omegas.add(simplificationPi);

        for (final var move : simplificationSorting) {
            omegas.add(applyTransposition(omegas.get(omegas.size() - 1), move));
        }

        final var remove = new HashSet<Float>();
        final var replaceBy = new HashMap<Float, Float>();
        float previousSymbol = -1;
        for (int i = 0; i < simplificationPi.size(); i++) {
            final var s = simplificationPi.get(i);
            if ((float) Math.floor(s) == (float) Math.floor(previousSymbol)) {
                remove.add(previousSymbol);
                replaceBy.put(s, (float) Math.floor(s));
            }
            previousSymbol = s;
        }

        for (int i = 1; i < omegas.size(); i++) {
            var temp = new ArrayList<>(omegas.get(i - 1));
            temp.removeAll(remove);
            temp.replaceAll(s -> replaceBy.getOrDefault(s, s));
            final var pi = Cycle.create(Ints.toArray(temp));

            temp = new ArrayList<>(omegas.get(i));
            temp.removeAll(remove);
            temp.replaceAll(s -> replaceBy.getOrDefault(s, s));
            final var _pi = Cycle.create(Ints.toArray(temp));

            final var move = computeProduct(false, _pi, pi.getInverse());
            if (!move.isIdentity()) {
                sorting.add(move.asNCycle());
            }
        }

        return sorting;
    }

    private static List<List<Float>> simplificationSorting(final Configuration equivalentConfig,
                                                          final List<Cycle> sorting,
                                                          final Configuration simplifiedConfig,
                                                          final List<Float> simplificationPi) {
        final var result = new ArrayList<List<Float>>();

        var sPi = simplifiedConfig.getPi();
        var fPi = simplificationPi;

        for (final var move : simplifiedConfig.translatedSorting(equivalentConfig, sorting)) {
            final var finalSPi = sPi;
            final var finalFPi = fPi;

            result.add(Ints.asList(move.getSymbols()).stream()
                    .map(s -> finalFPi.get(finalSPi.indexOf(s))).collect(Collectors.toList()));

            sPi = CommonOperations.applyTransposition(sPi, move);
            fPi = applyTransposition(fPi, result.get(result.size() - 1));
        }

        return result;
    }

    private static List<List<Float>> simplificationSorting(final Configuration simplifiedConfig,
                                                          final List<Cycle> sorting, final List<Float> simplificationPi) {
        var pi = simplifiedConfig.getPi();
        var _simplificationPi = simplificationPi;

        final var simplificationSorting = new ArrayList<List<Float>>();

        for (final var move : sorting) {
            final var finalPi = pi;
            final var fSimplificationPi = _simplificationPi;

            simplificationSorting.add(Ints.asList(move.getSymbols()).stream()
                    .map(s -> fSimplificationPi.get(finalPi.indexOf(s))).collect(Collectors.toList()));

            pi = CommonOperations.applyTransposition(pi, move);
            _simplificationPi = applyTransposition(_simplificationPi,
                    simplificationSorting.get(simplificationSorting.size() - 1));
        }

        return simplificationSorting;
    }

    private static List<Float> applyTransposition(final List<Float> pi, final List<Float> move) {
        final var a = move.get(0);
        final var b = move.get(1);
        final var c = move.get(2);

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

    // Requires a configuration whose \pi is canonical
    public static Set<List<List<Float>>> simplifications(final List<List<Float>> spi,
                                                         final Set<List<List<Float>>> collected) {
        if (isSimple(spi)) {
            collected.add(spi);
            return collected;
        }

        for (final var cycle : spi) {
            if (cycle.size() > 3) {
                for (int i = 0; i < cycle.size(); i++) {
                    simplifications(simplify(spi, cycle, i), collected);
                }
            }
        }

        return collected;
    }

    // Only works if \spi is related to a canonical \pi
    private static List<List<Float>> simplify(final List<List<Float>> spi, final List<Float> longCycle,
                                              final Integer breakIndex) {
        final var _spi = new ArrayList<>(spi);

        _spi.remove(longCycle);

        final var segment = new ArrayList<Float>();
        segment.add(longCycle.get(breakIndex));
        segment.add(longCycle.get((breakIndex + 1) % longCycle.size()));
        segment.add(longCycle.get((breakIndex + 2) % longCycle.size()));

        final var remaining = new ArrayList<Float>();
        remaining.add(segment.get(0) + 0.01F);

        for (int i = 0; i < longCycle.size() - 3; i++) {
            remaining.add(longCycle.get((breakIndex + i + 3) % longCycle.size()));
        }

        _spi.add(segment);
        _spi.add(remaining);

        return _spi;
    }

    private static boolean isSimple(final List<List<Float>> spi) {
        return spi.stream().noneMatch(c -> c.size() > 3);
    }
}
