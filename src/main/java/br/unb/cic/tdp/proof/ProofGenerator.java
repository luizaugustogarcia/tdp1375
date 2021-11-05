package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.proof.seq11_8.Combinations;
import br.unb.cic.tdp.proof.seq11_8.Extensions;
import br.unb.cic.tdp.proof.seq11_8.OrientedCycleGreaterOrEquals7;
import cern.colt.list.IntArrayList;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.primitives.Ints;
import org.apache.commons.math3.util.Pair;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

import static br.unb.cic.tdp.base.CommonOperations.*;
import static br.unb.cic.tdp.permutation.PermutationGroups.computeProduct;
import static br.unb.cic.tdp.proof.seq11_8.Combinations.*;
import static br.unb.cic.tdp.proof.seq11_8.Combinations.NECKLACE_SIZE_6;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class ProofGenerator {

    static Multimap<Integer, Pair<Configuration, List<Cycle>>> ehSortings = HashMultimap.create();

    private static final Configuration[] BAD_SMALL_COMPONENTS = new Configuration[]{
            ORIENTED_5_CYCLE, INTERLEAVING_PAIR,
            NECKLACE_SIZE_4, TWISTED_NECKLACE_SIZE_4,
            NECKLACE_SIZE_5, NECKLACE_SIZE_6,
            // invalid config
            new Configuration(new MulticyclePermutation("(0,9,2)(1,11,4)(3,8,6)(5,10,7)"))};

    static {
        // just to prevent concurrency issues
        for (final var badSmallComponent: BAD_SMALL_COMPONENTS) {
            badSmallComponent.hashCode();
        }
    }

    static final int[][] _11_8_SEQS;

    static {
        _11_8_SEQS = new int[][]{
                {0, 2, 2, 2},
                {0, 2, 2, 0, 2, 2, 2, 2},
                {0, 2, 0, 2, 2, 2, 2, 2},
                {0, 0, 2, 2, 2, 2, 2, 2},
                {0, 2, 2, 0, 2, 2, 2, 0, 2, 2, 2},
                {0, 2, 2, 0, 2, 2, 0, 2, 2, 2, 2},
                {0, 2, 2, 0, 2, 0, 2, 2, 2, 2, 2},
                {0, 2, 2, 0, 0, 2, 2, 2, 2, 2, 2},
                {0, 2, 0, 2, 2, 2, 2, 0, 2, 2, 2},
                {0, 2, 0, 2, 2, 2, 0, 2, 2, 2, 2},
                {0, 2, 0, 2, 2, 0, 2, 2, 2, 2, 2},
                {0, 2, 0, 2, 0, 2, 2, 2, 2, 2, 2},
                {0, 2, 0, 0, 2, 2, 2, 2, 2, 2, 2},
                {0, 0, 2, 2, 2, 2, 2, 0, 2, 2, 2},
                {0, 0, 2, 2, 2, 2, 0, 2, 2, 2, 2},
                {0, 0, 2, 2, 2, 0, 2, 2, 2, 2, 2},
                {0, 0, 2, 2, 0, 2, 2, 2, 2, 2, 2},
                {0, 0, 2, 0, 2, 2, 2, 2, 2, 2, 2},
                {0, 0, 0, 2, 2, 2, 2, 2, 2, 2, 2}
        };
    }

    // mvn exec:java -Dexec.mainClass="br.unb.cic.tdp.proof.ProofGenerator" -Dexec.args=".\\proof\\"
    public static void main(String[] args) throws Throwable {
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

        var path = Paths.get(ProofGenerator.class.getClassLoader()
                .getResource("eh-sortings.txt").toURI());
        loadEhSortings(path);

        Extensions.generate(args[0]);
        Combinations.generate(args[0]);
    }

    private static void loadEhSortings(final Path file) throws IOException {
        Files.lines(file).forEach(line -> {
            final var lineSplit = line.trim().split("->");
            if (lineSplit.length > 1) {
                final var spi = new MulticyclePermutation(lineSplit[0].split("#")[1].replace(" ", ","));
                final var config = new Configuration(spi);
                ehSortings.put(config.hashCode(),
                        new Pair<>(config, Arrays.stream(lineSplit[1].split(";"))
                                .map(s -> s.replace(" ", ",")).map(Cycle::create).collect(Collectors.toList())));
            }
        });
    }

    public static Optional<List<Cycle>> searchForSorting(final Configuration config) {
        if (isSimple(config)) {
            var pair = ehSortings.get(config.hashCode())
                    .stream().filter(p -> p.getFirst().equals(config)).findFirst();
            if (pair.isPresent()) {
                return Optional.of(config.translatedSorting(pair.get().getFirst(), pair.get().getSecond()));
            } else {
                return Optional.empty();
            }
        }

        for (final var seq : _11_8_SEQS) {
            if ((seq.length == 4 && config.getSpi().get3Norm() >= 3) ||
                    (seq.length == 8 && config.getSpi().get3Norm() >= 6) ||
                    (seq.length == 11 && config.getSpi().get3Norm() >= 8)) {
                Thread.currentThread().setName(config.getSpi().toString() + "-" + Arrays.toString(seq));
                final var sorting = new Stack<Cycle>();
                searchForSortingSeq(config.getSpi(), config.getPi(), seq, sorting);
                if (!sorting.isEmpty()) {
                    return Optional.of(sorting);
                }
            }
        }

        return Optional.empty();
    }

    public static Optional<List<Cycle>> searchForSortingSmallComponents(final Configuration config) {
        final var smallComponents = getComponents(config.getSpi(), config.getPi());
        for (int i = 2; i <= smallComponents.size(); i++) {
            for (final var components : combinations(smallComponents, i)) {
                final var spi = new MulticyclePermutation(components.getVector().stream().flatMap(Collection::stream).collect(Collectors.toList()));
                final var subConfig = new Configuration(spi, removeExtraSymbols(spi.getSymbols(), config.getPi()));
                var sorting = searchForSorting(subConfig);
                if (sorting.isPresent())
                    return sorting;
            }
        }

        return Optional.empty();
    }

    public static Cycle removeExtraSymbols(final Set<Integer> symbols, final Cycle pi) {
        final var newPi = new IntArrayList(symbols.size());
        for (final var symbol: pi.getSymbols()) {
            if (symbols.contains(symbol))
                newPi.add(symbol);
        }
        return Cycle.create(newPi);
    }

    private static boolean isSimple(final Configuration config) {
        return config.getSpi().stream().allMatch(c -> c.size() == 3);
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
