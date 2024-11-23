package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.proof.seq11_8.Combinations;
import br.unb.cic.tdp.proof.seq11_8.Extensions;
import br.unb.cic.tdp.proof.util.ListOfCycles;
import br.unb.cic.tdp.proof.util.MoveTreeNode;
import br.unb.cic.tdp.proof.util.MovesStack;
import br.unb.cic.tdp.proof.util.SequenceSearcher;
import br.unb.cic.tdp.util.Pair;
import cern.colt.list.IntArrayList;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.primitives.Ints;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static br.unb.cic.tdp.base.CommonOperations.getComponents;
import static br.unb.cic.tdp.permutation.PermutationGroups.computeProduct;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.stream.Collectors.toList;

public class ProofGenerator {

    static final Multimap<Integer, Pair<Configuration, List<Cycle>>> ehSortings = HashMultimap.create();

    static final int[][] _4_3 = new int[][]{{0, 2, 2, 2}};

    static final int[][] _8_6 = new int[][]{
            {0, 2, 2, 0, 2, 2, 2, 2},
            {0, 2, 0, 2, 2, 2, 2, 2},
            {0, 0, 2, 2, 2, 2, 2, 2}};

    static final int[][] _11_8 = new int[][]{
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
        loadEhSortings(path);

        Extensions.generate(args[0]);
        Combinations.generate(args[0]);
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

    private static boolean isSimple(final Configuration config) {
        return config.getSpi().stream().allMatch(c -> c.size() == 3);
    }

    public static Optional<List<Cycle>> searchForSorting(final Configuration configuration) {
        if (isSimple(configuration)) {
            var candidates = ehSortings.get(configuration.hashCode());

            Optional<Pair<Configuration, List<Cycle>>> pair;
            if (candidates.size() == 1) {
                pair = candidates.stream().findFirst();
            } else {
                pair = candidates.stream().filter(p -> p.getFirst().equals(configuration)).findFirst();
            }

            if (pair.isPresent()) {
                return pair.map(p -> configuration.translatedSorting(p.getFirst(), p.getSecond()));
            }
        }

        val _3norm = configuration.getSpi().get3Norm();

        List<Cycle> sorting = Collections.emptyList();

        String threadName = Thread.currentThread().getName();

        if (_3norm >= 3) {
            Thread.currentThread().setName(configuration.hashCode() + "-" + configuration.getSpi() + "-4,3");
            sorting = searchSorting(configuration, _4_3_SEQS);
        }

        if (_3norm >= 6 && sorting.isEmpty()) {
            Thread.currentThread().setName(configuration.hashCode() + "-" + configuration.getSpi() + "-8,6");
            sorting = searchSorting(configuration, _8_6_SEQS);
        }

        if (_3norm >= 8 && sorting.isEmpty()) {
            Thread.currentThread().setName(configuration.hashCode() + "-" + configuration.getSpi() + "-11,8");
            sorting = searchSorting(configuration, _11_8_SEQS);
        }

        Thread.currentThread().setName(threadName);

        if (!sorting.isEmpty()) {
            return Optional.of(sorting);
        }

        if (configuration.isFull() && getComponents(configuration.getSpi(), configuration.getPi()).size() == 1) {
            System.out.println("Full configuration without (11/8): " + configuration.getCanonical().getSpi());
        }

        return Optional.empty();
    }

    public static List<Cycle> searchSorting(final Configuration configuration, final MoveTreeNode rootMove) {
        val spi = new ListOfCycles();
        configuration.getSpi().stream().map(Cycle::getSymbols).forEach(spi::add);

        val parity = new boolean[configuration.getPi().size()];
        int[][] spiIndex = new int[configuration.getPi().size()][];
        var current = spi.head;
        while (current != null) {
            val cycle = current.data;
            for (int i : cycle) {
                spiIndex[i] = cycle;
                parity[i] = (cycle.length & 1) == 1;
            }
            current = current.next;
        }

        val pi = configuration.getPi().getSymbols();

        val stack = new MovesStack(rootMove.getHeight());

        return new SequenceSearcher()
                .search(spi, parity, spiIndex, spiIndex.length, pi, stack, rootMove)
                .toList().stream().map(Cycle::of).collect(toList());
    }

    public static Cycle removeExtraSymbols(final Set<Integer> symbols, final Cycle pi) {
        val newPi = new IntArrayList(symbols.size());
        for (val symbol: pi.getSymbols()) {
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
