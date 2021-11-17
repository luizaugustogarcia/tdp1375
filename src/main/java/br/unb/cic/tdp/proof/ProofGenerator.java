package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.proof.seq11_8.Combinations;
import br.unb.cic.tdp.proof.seq11_8.Extensions;
import br.unb.cic.tdp.util.Pair;
import cern.colt.list.IntArrayList;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.primitives.Ints;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static br.unb.cic.tdp.base.CommonOperations.*;
import static br.unb.cic.tdp.permutation.PermutationGroups.computeProduct;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class ProofGenerator {

    static Multimap<Integer, Pair<Configuration, List<Cycle>>> ehSortings = HashMultimap.create();

    static final int[][] _4_3 = new int[][]{{0,2,2,2}};

    static final int[][] _8_6 = new int[][]{
            {0,2,2,0,2,2,2,2},
            {0,2,0,2,2,2,2,2},
            {0,0,2,2,2,2,2,2}};

    static final int[][] _11_8 = new int[][]{
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

    static final Move _4_3_SEQS = new Move(0, new LinkedList<>());
    static final Move _8_6_SEQS = new Move(0, new LinkedList<>());
    static final Move _11_8_SEQS = new Move(0, new LinkedList<>());

    static {
        toTrie(_4_3, _4_3_SEQS);
        toTrie(_8_6, _8_6_SEQS);
        toTrie(_11_8, _11_8_SEQS);
    }

    private static void toTrie(final int[][] seqs, Move root) {
        final var root_ = root;
        for (int[] seq : seqs) {
            root = root_;
            for (int j = 1; j < seq.length; j++) {
                final var move = seq[j];
                if (root.getChildren().stream().noneMatch(m -> m.mu == move)) {
                    root.getChildren().add(new Move(move, new LinkedList<>()));
                }
                root = root.getChildren().stream().filter(m -> m.getMu() == move).findFirst().get();
            }
        }
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
            var candidates = ehSortings.get(config.hashCode());

            Optional<Pair<Configuration, List<Cycle>>> pair;
            if (candidates.size() == 1) {
                pair = candidates.stream().findFirst();
            } else {
                pair = candidates.stream().filter(p -> p.getFirst().equals(config)).findFirst();
            }

            return pair.map(p -> config.translatedSorting(p.getFirst(), p.getSecond()));
        }

        final var _3norm = config.getSpi().get3Norm();

        final var sorting = new Stack<Cycle>();

        if (_3norm >= 3) {
            Thread.currentThread().setName(config.hashCode() + "-" + config.getSpi() + "-4,3");
            searchForSortingSeq(config.getSpi(), config.getPi(), sorting, _4_3_SEQS);
        }

        if (_3norm >= 6 && sorting.isEmpty()) {
            Thread.currentThread().setName(config.hashCode() + "-" + config.getSpi() + "-8,6");
            searchForSortingSeq(config.getSpi(), config.getPi(), sorting, _8_6_SEQS);
        }

        if (_3norm >= 8 && sorting.isEmpty()) {
            Thread.currentThread().setName(config.hashCode() + "-" + config.getSpi() + "-11,8");
            searchForSortingSeq(config.getSpi(), config.getPi(), sorting, _11_8_SEQS);
        }

        if (!sorting.isEmpty()) {
            return Optional.of(sorting);
        }

        if (config.isFull() && getComponents(config.getSpi(), config.getPi()).size() == 1) {
            System.out.println("Full configuration without (11/8): " + config.getCanonical().getSpi());
        }

        return Optional.empty();
    }

    // assumes that spi contains only even cycles
    public static List<Cycle> searchForSortingSeq(final MulticyclePermutation spi, final Cycle pi, final Stack<Cycle> moves, final Move root) {
        final Stream<Cycle> nextMoves;
        if (root.getMu() == 0) {
            nextMoves = generateAll0And2Moves(spi, pi).filter(p -> p.getSecond() == root.getMu()).map(Pair::getFirst);
        } else {
            nextMoves = generateAll2Moves(spi, pi).map(Pair::getFirst);
        }

        try {
            final var iterator = nextMoves.iterator();
            while (iterator.hasNext()) {
                final var move = iterator.next();
                moves.push(move);

                if (root.getChildren().isEmpty()) {
                    return moves;
                } else {
                    for (final var m : root.getChildren()) {
                        final var sorting =
                                searchForSortingSeq(computeProduct(true, pi.getMaxSymbol() + 1, spi, move.getInverse()), applyTransposition(pi, move), moves, m);
                        if (!sorting.isEmpty()) {
                            return moves;
                        }
                    }
                }
                moves.pop();
            }
        } catch(IllegalStateException e) {
            // means empty stream
            return Collections.emptyList();
        }

        return Collections.emptyList();
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
        for (final Cycle move : sorting) {
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


    @AllArgsConstructor
    @Getter
    public static class Move {
        private final int mu;
        private final List<Move> children;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Move m = (Move) o;
            return mu == m.mu;
        }

        @Override
        public String toString() {
            return Integer.toString(mu);
        }
    }
}
