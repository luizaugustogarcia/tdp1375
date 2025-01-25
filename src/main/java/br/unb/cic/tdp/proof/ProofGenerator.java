package br.unb.cic.tdp.proof;

import static br.unb.cic.tdp.base.CommonOperations.generateAllTranspositions;
import static br.unb.cic.tdp.permutation.PermutationGroups.computeProduct;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.stream.Collectors.toList;

import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.primitives.Ints;
import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.proof.seq11_8.Combinations;
import br.unb.cic.tdp.proof.seq11_8.Extensions;
import br.unb.cic.tdp.proof.util.MoveTreeNode;
import br.unb.cic.tdp.util.Pair;
import cern.colt.list.IntArrayList;
import lombok.SneakyThrows;
import lombok.val;

public class ProofGenerator {

    static final Multimap<Integer, Pair<Configuration, List<Cycle>>> ehSortings = HashMultimap.create();

    static final int[][] _4_3 = new int[][] { { 0, 2, 2, 2 } };

    static final int[][] _8_6 = new int[][] {
            { 0, 2, 2, 0, 2, 2, 2, 2 },
            { 0, 2, 0, 2, 2, 2, 2, 2 },
            { 0, 0, 2, 2, 2, 2, 2, 2 } };

    static final int[][] _11_8 = new int[][] {
            { 0, 2, 2, 0, 2, 2, 2, 0, 2, 2, 2 },
            { 0, 2, 2, 0, 2, 2, 0, 2, 2, 2, 2 },
            { 0, 2, 2, 0, 2, 0, 2, 2, 2, 2, 2 },
            { 0, 2, 2, 0, 0, 2, 2, 2, 2, 2, 2 },
            { 0, 2, 0, 2, 2, 2, 2, 0, 2, 2, 2 },
            { 0, 2, 0, 2, 2, 2, 0, 2, 2, 2, 2 },
            { 0, 2, 0, 2, 2, 0, 2, 2, 2, 2, 2 },
            { 0, 2, 0, 2, 0, 2, 2, 2, 2, 2, 2 },
            { 0, 2, 0, 0, 2, 2, 2, 2, 2, 2, 2 },
            { 0, 0, 2, 2, 2, 2, 2, 0, 2, 2, 2 },
            { 0, 0, 2, 2, 2, 2, 0, 2, 2, 2, 2 },
            { 0, 0, 2, 2, 2, 0, 2, 2, 2, 2, 2 },
            { 0, 0, 2, 2, 0, 2, 2, 2, 2, 2, 2 },
            { 0, 0, 2, 0, 2, 2, 2, 2, 2, 2, 2 },
            { 0, 0, 0, 2, 2, 2, 2, 2, 2, 2, 2 }
    };

    static final MoveTreeNode _4_3_SEQS = new MoveTreeNode(0, new MoveTreeNode[0], null);
    static final MoveTreeNode _8_6_SEQS = new MoveTreeNode(0, new MoveTreeNode[0], null);
    public static final MoveTreeNode _11_8_SEQS = new MoveTreeNode(0, new MoveTreeNode[0], null);

    static {
        toTrie(_4_3, _4_3_SEQS);
        toTrie(_8_6, _8_6_SEQS);
        toTrie(_11_8, _11_8_SEQS);
    }

    public static void toTrie(final int[][] seqs, MoveTreeNode root) {
        val rootPrime = root;
        for (int[] seq : seqs) {
            root = rootPrime;
            for (int j = 1; j < seq.length; j++) {
                val move = seq[j];
                if (Arrays.stream(root.children).noneMatch(m -> m.mu == move)) {
                    if (root.children.length == 0) {
                        root.children = new MoveTreeNode[1];
                        root.children[0] = new MoveTreeNode(move, new MoveTreeNode[0], root);
                    } else {
                        val children = new MoveTreeNode[2];
                        children[0] = root.children[0];
                        children[1] = new MoveTreeNode(move, new MoveTreeNode[0], root);
                        root.children = children;
                    }
                }
                root = Arrays.stream(root.children).filter(m -> m.mu == move).findFirst().get();
            }
        }
    }

    // mvn exec:java -Dexec.mainClass="br.unb.cic.tdp.proof.ProofGenerator" -Dexec.args=".\\proof\\"
    public static void main(String[] args) throws Throwable {
        Velocity.setProperty("resource.loader", "class");
        Velocity.setProperty("parser.pool.size", Runtime.getRuntime().availableProcessors());
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
        //loadEhSortings(path);

        Extensions.generate(args[0]);
        //Combinations.generate(args[0]);
    }

    @SneakyThrows
    private static void loadEhSortings(final Path file) {
        Files.lines(file).forEach(line -> {
            val lineSplit = line.trim().split("->");
            if (lineSplit.length > 1) {
                val spi = new MulticyclePermutation(lineSplit[0].split("#")[1].replace(" ", ","));
                val config = new Configuration(spi);
                ehSortings.put(config.hashCode(),
                        new Pair<>(config, Arrays.stream(lineSplit[1].split(";"))
                                .map(s -> s.replace(" ", ",")).map(Cycle::of).collect(Collectors.toList())));
            }
        });
    }

//    public static void main(String[] args) {
//        Configuration configuration = new Configuration(new MulticyclePermutation("(0 9 6 12 7)(1 11 10 8 3)(2 5 4)"));
//        System.out.println(searchForSorting(configuration, configuration.getSpi(), configuration.getPi(), new Stack<>()));
//    }

    public static Optional<List<Cycle>> searchForSorting(final Configuration initialConfiguration, final MulticyclePermutation spi,
            final Cycle pi, final Stack<Cycle> stack) {
        var fixedSymbols = spi.stream().filter(c -> c.size() == 1).map(c -> c.get(0)).collect(Collectors.toSet());

        for (var cycle : initialConfiguration.getSpi()) {
            // each cycle in the configuration should retain at most one fixed symbol
            fixedSymbols.stream().filter(cycle::contains).findFirst().ifPresent(fixedSymbols::remove);
        }

        var minRate = 1.500000000001;
        double rate = (fixedSymbols.size()) / (double) stack.size();
        if (!fixedSymbols.isEmpty()) {
            if (rate >= minRate) {
                return Optional.of(stack);
            }
        }

        var symbolsLeft = spi.stream().filter(c -> c.size() > 1).mapToInt(Cycle::size).sum();
        var movesLeft = Math.ceil((double) symbolsLeft / 3); // each move left can fix at most 3 symbols
        var totalMoves = stack.size() + movesLeft;
        var totalRate = (initialConfiguration.getSpi().getNumberOfSymbols() - initialConfiguration.getSpi().size()) / totalMoves;
        if (totalRate < minRate) {
            return Optional.empty();
        }

        Optional<List<Cycle>> sorting = Optional.empty();
        for (var m : generateAllTranspositions(spi, pi).collect(toList())) {
            stack.push(m);
            sorting =
                    searchForSorting(initialConfiguration, spi.times(m.getInverse()), m.times(pi).asNCycle(), stack);
            if (sorting.isPresent()) {
                return sorting;
            }
            stack.pop();
        }

        return sorting;
    }

    public static Optional<List<Cycle>> searchForSorting(final Configuration configuration) {
        return searchForSorting(configuration, configuration.getSpi(), configuration.getPi(), new Stack<>());
    }

    public static Cycle removeExtraSymbols(final Set<Integer> symbols, final Cycle pi) {
        val newPi = new IntArrayList(symbols.size());
        for (val symbol : pi.getSymbols()) {
            if (symbols.contains(symbol))
                newPi.add(symbol);
        }
        return Cycle.of(newPi);
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

        val spis = new ArrayList<MulticyclePermutation>();
        val jsSpis = new ArrayList<String>();
        val jsPis = new ArrayList<String>();
        var spi = canonicalConfig.getSpi();
        var pi = canonicalConfig.getPi();
        for (final Cycle move : sorting) {
            spis.add(spi = computeProduct(spi, move.getInverse()));
            jsSpis.add(permutationToJsArray(spi));
            jsPis.add(cycleToJsArray(pi = computeProduct(move, pi).asNCycle()));
        }
        context.put("spis", spis);
        context.put("jsSpis", jsSpis);
        context.put("jsPis", jsPis);

        val template = Velocity.getTemplate("templates/sorting.html");
        template.merge(context, writer);
    }
}
