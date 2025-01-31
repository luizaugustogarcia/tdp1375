package br.unb.cic.tdp.proof;

import static br.unb.cic.tdp.permutation.PermutationGroups.computeProduct;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.primitives.Ints;
import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
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

//    public static void main(String[] args) throws InterruptedException {
//        val l = List.of("(0 10 8 2)(1 9 4)(3 11 7 6 5)",
//                "(0 9 4)(1 6 3)(2 11 10 8 7 5)",
//                "(0 10 9 8 5 2)(1 7 4)(3 11 6)");
//
//        var pool = new ForkJoinPool();
//        l.stream().forEach(config -> {
//            pool.submit(() -> {
//                Configuration configuration = new Configuration(new MulticyclePermutation(config));
//                System.out.println(configuration.getSpi() + "-" +
//                        searchForSorting(configuration, configuration.getSpi().stream().map(c -> c.get(0)).collect(Collectors.toSet()),
//                                configuration.getSpi(), configuration.getPi().getSymbols(), new Stack<>()));
//            });
//        });
//        pool.shutdown();
//        pool.awaitTermination(1, TimeUnit.HOURS);
//    }

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

    private static double lowestRate = 2;

    public static Optional<List<Cycle>> searchForSorting(final Configuration initialConfiguration, final Set<Integer> notFixableSymbols,
            final MulticyclePermutation spi, final int[] pi, final Stack<Cycle> stack) {
        val fixedSymbols = spi.stream()
                .filter(c -> c.size() == 1 && !notFixableSymbols.contains(c.get(0)))
                .map(c -> c.get(0))
                .collect(Collectors.toSet());

        double minRate = 1.51;
        double rate = (fixedSymbols.size()) / (double) stack.size();
        if (!fixedSymbols.isEmpty()) {
            if (rate >= minRate) {
                if (rate < lowestRate) {
                    lowestRate = rate;
                    System.out.println("Lowest rate: " + lowestRate);
                }
                return Optional.of(stack);
            }
        }

        var movesLeft =
                Math.floor(spi.stream().filter(c -> c.size() > 1).mapToInt(Cycle::size).sum() / 3.0); // each move can add up to 3 bonds
        var totalMoves = stack.size() + movesLeft;
        var globalRate = (initialConfiguration.getSpi().getNumberOfSymbols() - initialConfiguration.getSpi().size()) / (double) totalMoves;
        if (globalRate < minRate) {
            return Optional.empty();
        }

        Optional<List<Cycle>> sorting = Optional.empty();
        val ci = cyclesIndex(spi, pi);
        for (int i = 0; i < pi.length - 2; i++) {
            if (!fixedSymbols.contains(pi[i]))
                for (int j = i + 1; j < pi.length - 1; j++) {
                    if (!fixedSymbols.contains(pi[j]))
                        for (int k = j + 1; k < pi.length; k++) {
                            if (!fixedSymbols.contains(pi[k])) {
                                int a = pi[i], b = pi[j], c = pi[k];

                                val is_2Move = ci[a] != ci[b] && ci[b] != ci[c] && ci[a] != ci[c];
                                if (is_2Move) {
                                    continue;
                                }

                                val m = Cycle.of(a, b, c);
                                stack.push(m);

                                sorting =
                                        searchForSorting(initialConfiguration, notFixableSymbols, spi.times(m.getInverse()),
                                                applyTranspositionOptimized(pi, m.getSymbols()),
                                                stack);
                                if (sorting.isPresent()) {
                                    return sorting;
                                }
                                stack.pop();
                            }
                        }
                }
        }

        return sorting;
    }

    private static Cycle[] cyclesIndex(final MulticyclePermutation spi, final int[] pi) {
        val index = new Cycle[pi.length];

        for (val cycle : spi) {
            for (val symbol : cycle.getSymbols()) {
                index[symbol] = cycle;
            }
        }

        return index;
    }

    public static int[] applyTranspositionOptimized(final int[] pi, final int[] move) {
        val a = move[0];
        val b = move[1];
        val c = move[2];

        val indexes = new int[3];
        for (var i = 0; i < pi.length; i++) {
            if (pi[i] == a)
                indexes[0] = i;
            if (pi[i] == b)
                indexes[1] = i;
            if (pi[i] == c)
                indexes[2] = i;
        }

        Arrays.sort(indexes);

        val result = new int[pi.length];
        System.arraycopy(pi, 0, result, 0, indexes[0]);
        System.arraycopy(pi, indexes[1], result, indexes[0], indexes[2] - indexes[1]);
        System.arraycopy(pi, indexes[0], result, indexes[0] + (indexes[2] - indexes[1]), indexes[1] - indexes[0]);
        System.arraycopy(pi, indexes[2], result, indexes[2], pi.length - indexes[2]);

        return result;
    }

    public static Optional<List<Cycle>> searchForSorting(final Configuration configuration) {
        return searchForSorting(configuration, configuration.getSpi().stream().map(Cycle::getMinSymbol).collect(Collectors.toSet()),
                configuration.getSpi(), configuration.getPi().getSymbols(), new Stack<>());
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

    public static void renderSorting(final Configuration extendedFrom, final Configuration canonicalConfig, final List<Cycle> sorting, final Writer writer) {
        VelocityContext context = new VelocityContext();

        context.put("extendedFrom", extendedFrom.getSpi());
        context.put("jsExtendedFrom", permutationToJsArray(extendedFrom.getSpi()));
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
