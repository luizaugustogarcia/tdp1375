package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.base.Simplification;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.proof.seq11_8.Combinations;
import br.unb.cic.tdp.proof.seq11_8.Extensions;
import br.unb.cic.tdp.proof.seq11_8.OrientedCycleGreaterOrEquals7;
import cern.colt.list.ByteArrayList;
import cern.colt.list.FloatArrayList;
import com.google.common.primitives.Bytes;
import org.apache.commons.math3.util.Pair;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

import static br.unb.cic.tdp.base.CommonOperations.applyTransposition;
import static br.unb.cic.tdp.base.CommonOperations.*;
import static br.unb.cic.tdp.base.Configuration.fromSignature;
import static br.unb.cic.tdp.base.Simplification.*;
import static br.unb.cic.tdp.permutation.PermutationGroups.computeProduct;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class ProofGenerator {

    // mvn exec:java -Dexec.mainClass="br.unb.cic.tdp.proof.ProofGenerator" -Dexec.args=".\\proof\\ false"
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
        final FileSystem fs = FileSystems.newFileSystem(URI.create(fileUri[0]), env);
        final Path path = fs.getPath(fileUri[1]);

        final var knownSortings = loadKnownSortings(path);

        //TwoOriented5Cycle.generate(knownSortings, args[0]);

        final var shouldAlsoUseBruteForce = args.length > 1 && args[1].equalsIgnoreCase("true");
        if (args.length > 2)
            numberOfCoresToUse = Integer.parseInt(args[2]);

        Extensions.generate(knownSortings, shouldAlsoUseBruteForce, args[0]);
        Combinations.generate(knownSortings, shouldAlsoUseBruteForce, args[0]);
    }

    public static Pair<Map<Configuration, List<Cycle>>,
            Map<Integer, List<Configuration>>> loadKnownSortings(final Path file) throws IOException {
        final var knownSortings = new HashMap<Configuration, List<Cycle>>();

        Files.lines(file).forEach(line -> {
            final var lineSplit = line.trim().split("->");
            if (lineSplit.length > 1) {
                final var spi = new MulticyclePermutation(lineSplit[0].split("#")[1]);
                knownSortings.put(new Configuration(spi),
                        Arrays.stream(lineSplit[1].split(";")).map(Cycle::new).collect(Collectors.toList()));
            }
        });

        return new Pair<>(knownSortings, knownSortings.keySet().stream()
                .collect(Collectors.groupingBy(Configuration::hashCode)));
    }

    public static List<Cycle> searchForSorting(final Configuration config, final Pair<Map<Configuration, List<Cycle>>,
            Map<Integer, List<Configuration>>> knownSortings, final boolean shouldAlsoUseBruteForce) {
        if (knownSortings.getFirst().containsKey(config)) {
            // can be empty if it was brute force sorted
            if (knownSortings.getFirst().get(config).isEmpty()) {
                return null;
            }

            return config.translatedSorting(knownSortings.getSecond().get(config.hashCode()).stream()
                    .filter(c -> c.equals(config)).findFirst().get(), knownSortings.getFirst().get(config));
        }

        var sorting = simplifications(toListOfListOfFloats(config.getSpi()), new HashSet<>())
                .stream().map(s -> searchForSorting(config, s, knownSortings)).filter(Objects::nonNull).findFirst();

        if (sorting.isPresent()) {
            return sorting.get();
        }

        if (shouldAlsoUseBruteForce && config.getSpi().stream().anyMatch(Cycle::isLong)) {
            // TODO remove
            System.out.println("brute force sorting " + config);

            final var _sorting = searchFor11_8SeqParallel(config.getSpi(), config.getPi());
            knownSortings.getFirst().put(config, _sorting);
            knownSortings.getSecond().computeIfAbsent(config.hashCode(), key -> new ArrayList<>());
            knownSortings.getSecond().computeIfPresent(config.hashCode(), (key, value) -> {
                value.add(config);
                return value;
            });

            // TODO remove
            System.out.println("brute force sorted " + config + "->" + _sorting);

            if (!_sorting.isEmpty())
                return _sorting;
        }

        return null;
    }

    private static List<Cycle> searchForSorting(final Configuration config, final List<List<Float>> simplificationSpi,
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
            final var simplificationSorting = simplificationSorting(equivalentConfig, equivalentSorting,
                    simplifiedConfig, simplificationPi);

            sorting = mimicSorting(simplificationPi, simplificationSorting);
        } else {
            sorting = searchForSortingSubConfiguration(simplifiedConfig, knownSortings);
            if (sorting != null) {
                sorting = mimicSorting(simplificationPi,
                        simplificationSorting(simplifiedConfig, sorting, simplificationPi));
            }
        }

        if (sorting != null) {
            assert is11_8(config.getSpi(), config.getPi(), sorting) : "ERROR";
        }

        return sorting;
    }

    private static List<Cycle> searchForSortingSubConfiguration(final Configuration config,
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
                                .stream().filter(s -> Arrays.equals(s.getContent(), signature)).findFirst().get();

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
        final var cycleIndex = cycleIndex(spi, pi);

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

    public static String permutationToJsArray(final MulticyclePermutation permutation) {
        return "[" + permutation
                .stream().map(c -> "[" + Bytes.asList(c.getSymbols()).stream()
                        .map(s -> Byte.toString(s))
                        .collect(Collectors.joining(",")) + "]")
                .collect(Collectors.joining(",")) + "]";
    }

    public static String cycleToJsArray(final Cycle cycle) {
        return "[" + Bytes.asList(cycle.getSymbols()).stream()
                .map(s -> Byte.toString(s))
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
            final var rho = sorting.get(i);
            spis.add(spi = computeProduct(spi, rho.getInverse()));
            jsSpis.add(permutationToJsArray(spi));
            jsPis.add(cycleToJsArray(pi = computeProduct(rho, pi).asNCycle()));
        }
        context.put("spis", spis);
        context.put("jsSpis", jsSpis);
        context.put("jsPis", jsPis);

        final var template = Velocity.getTemplate("templates/sorting.html");
        template.merge(context, writer);
    }
}
