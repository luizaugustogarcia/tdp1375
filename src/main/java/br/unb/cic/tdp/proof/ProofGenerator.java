package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.proof.seq11_8.Combinations;
import br.unb.cic.tdp.proof.seq11_8.Extensions;
import br.unb.cic.tdp.proof.seq11_8.OrientedCycleGreaterOrEquals7;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.primitives.Ints;
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
import static br.unb.cic.tdp.permutation.PermutationGroups.computeProduct;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class ProofGenerator {

    static Multimap<Integer, Pair<Configuration, List<Cycle>>> ehSortings = HashMultimap.create();

    static final int[][] _11_8seqs;

    static {
        _11_8seqs = new int[][]{
                {0, 2, 2, 2},
                {0, 2, 2, 0, 2, 2, 2, 2},
                {0, 2, 0, 2, 2, 2, 2, 2},
                {0, 0, 2, 2, 2, 2, 2, 2},
                {0, 0, 0, 2, 2, 2, 2, 2, 2, 2, 2},
                {0, 0, 2, 0, 2, 2, 2, 2, 2, 2, 2},
                {0, 0, 2, 2, 0, 2, 2, 2, 2, 2, 2},
                {0, 0, 2, 2, 2, 0, 2, 2, 2, 2, 2},
                {0, 0, 2, 2, 2, 2, 0, 2, 2, 2, 2},
                {0, 0, 2, 2, 2, 2, 2, 0, 2, 2, 2},
                {0, 2, 0, 0, 2, 2, 2, 2, 2, 2, 2},
                {0, 2, 0, 2, 0, 2, 2, 2, 2, 2, 2},
                {0, 2, 0, 2, 2, 0, 2, 2, 2, 2, 2},
                {0, 2, 0, 2, 2, 2, 0, 2, 2, 2, 2},
                {0, 2, 0, 2, 2, 2, 2, 0, 2, 2, 2},
                {0, 2, 2, 0, 0, 2, 2, 2, 2, 2, 2},
                {0, 2, 2, 0, 2, 0, 2, 2, 2, 2, 2},
                {0, 2, 2, 0, 2, 2, 0, 2, 2, 2, 2},
                {0, 2, 2, 0, 2, 2, 2, 0, 2, 2, 2}
        };
    }

    // mvn exec:java -Dexec.mainClass="br.unb.cic.tdp.proof.ProofGenerator" -Dexec.args=".\\proof\\ 8"
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

        //final Map<String, String> env = new HashMap<>();
        //final String[] fileUri = ProofGenerator.class.getClassLoader()
        //        .getResource("eh-sortings").toURI().toString().split("!");
        //final FileSystem fs = FileSystems.newFileSystem(URI.create(fileUri[0]), env);
        //final Path path = fs.getPath(fileUri[1]);

        final Path path = Paths.get(ProofGenerator.class.getClassLoader()
                .getResource("eh-sortings").toURI());
        loadEhSortings(path);

        if (args.length > 1)
            numberOfThreads = Integer.parseInt(args[1]);

        Extensions.generate(args[0]);
        Combinations.generate(args[0]);
    }

    private static void loadEhSortings(final Path file) throws IOException {
        Files.lines(file).forEach(line -> {
            final var lineSplit = line.trim().split("->");
            if (lineSplit.length > 1) {
                final var spi = new MulticyclePermutation(lineSplit[0].split("#")[1]);
                final var config = new Configuration(spi);
                ehSortings.put(config.hashCode(),
                        new Pair<>(config, Arrays.stream(lineSplit[1].split(";")).map(Cycle::create).collect(Collectors.toList())));
            }
        });
    }

    public static Optional<List<Cycle>> searchForSorting(final Configuration config) {
        var pair = ehSortings.get(config.hashCode())
                .stream().filter(p -> p.getFirst().equals(config)).findFirst();
        if (pair.isPresent()) {
            return Optional.of(config.translatedSorting(pair.get().getFirst(), pair.get().getSecond()));
        }

        for (final var seq : _11_8seqs) {
            if ((seq.length == 4 && config.getSpi().get3Norm() >= 3) ||
                    (seq.length == 8 && config.getSpi().get3Norm() >= 6) ||
                    (seq.length == 11 && config.getSpi().get3Norm() >= 8)) {
                final var sorting = new Stack<Cycle>();
                searchForSortingSeq(config.getSpi(), config.getPi(), seq, sorting);
                if (!sorting.isEmpty()) {
                    return Optional.of(sorting);
                }
            }
        }

        return Optional.empty();
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
}
