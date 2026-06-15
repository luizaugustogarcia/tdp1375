package br.unb.cic.tdp.proof.seq11_8;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.proof.SortOrExtend;
import br.unb.cic.tdp.util.Pair;
import cern.colt.list.FloatArrayList;
import com.google.common.base.Preconditions;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.Stream;

import static br.unb.cic.tdp.base.CommonOperations.cycleIndex;
import static br.unb.cic.tdp.base.Configuration.ofSignature;
import static br.unb.cic.tdp.base.Configuration.signature;
import static br.unb.cic.tdp.proof.SortOrExtend.permutationToJsArray;

public class Extensions {

    @SneakyThrows
    public static void generate(final String outputDir) {
        Files.createDirectories(Paths.get(outputDir + "/dfs/"));
        Files.createDirectories(Paths.get(outputDir + "/dfs/bad-cases/"));

//        try (var pool = new ForkJoinPool()) {
//            // oriented 5-cycle
//            pool.execute(new SortOrExtendExtensions(new Configuration(new MulticyclePermutation("(0,3,1,4,2)")), outputDir + "/dfs/"));
//            // interleaving pair
//            pool.execute(new SortOrExtendExtensions(new Configuration(new MulticyclePermutation("(0,4,2)(1,5,3)")), outputDir + "/dfs/"));
//            // intersecting pair
//            pool.execute(new SortOrExtendExtensions(new Configuration(new MulticyclePermutation("(0,3,1)(2,5,4)")), outputDir + "/dfs/"));
//        }

        try (val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())) {
            Files.list(Paths.get(outputDir + "/dfs/bad-cases/"))
                    .map(Path::toFile)
                    .forEach(file -> executor.submit(() -> makeHtmlNavigation(new Configuration(new MulticyclePermutation(file.getName())), outputDir)));
        }
    }

    @Getter
    @AllArgsConstructor
    public static class ExtensionData {
        private final String info;
        private final boolean hasSorting;
        private final int hashCode;
        private final int threeNorm;
        private final String signature;
        private final String jsSpi;
        private final String spi;
        private final String linkTarget;
    }

    private static List<ExtensionData> buildExtensionData(final List<Pair<String, Configuration>> extensions, final String outputDir) {
        val result = new ArrayList<ExtensionData>();
        for (val extension : extensions) {
            val configuration = extension.getSecond();
            val canonical = extension.getSecond().getCanonical();

            val badCaseFile = new File(outputDir + "/dfs/bad-cases/" + canonical.getSpi());
            val hasSorting = !badCaseFile.exists();

            result.add(new ExtensionData(
                    extension.getFirst(),
                    hasSorting,
                    configuration.hashCode(),
                    configuration.getSpi().get3Norm(),
                    configuration.getSignature().toString(),
                    permutationToJsArray(configuration.getSpi()),
                    configuration.getSpi().toString(),
                    canonical.getSpi().toString()
            ));
        }
        return result;
    }

    /*
     * Type 1 extension.
     */
    private static List<Pair<String, Configuration>> type1Extensions(final Configuration config) {
        val result = new ArrayList<Pair<String, Configuration>>();

        val newCycleLabel = config.getSpi().size() + 1;

        val signature = signature(config.getSpi(), config.getPi());

        for (int i = 0; i < signature.length; i++) {
            if (config.getOpenGates().contains(i)) {
                for (int b = 0; b < signature.length; b++) {
                    for (int c = b; c < signature.length; c++) {
                        if (!(i == b && b == c)) {
                            result.add(new Pair<>(String.format("a=%d b=%d c=%d", i, b, c),
                                    ofSignature(unorientedExtension(signature, newCycleLabel, i, b, c).elements())));
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

        val result = new ArrayList<Pair<String, Configuration>>();

        val newCycleLabel = config.getSpi().size() + 1;

        val signature = signature(config.getSpi(), config.getPi());

        for (int a = 0; a < signature.length; a++) {
            for (int b = a; b < signature.length; b++) {
                for (int c = b; c < signature.length; c++) {
                    if (!(a == b && b == c)) {
                        result.add(new Pair<>(String.format("a=%d b=%d c=%d", a, b, c),
                                ofSignature(unorientedExtension(signature, newCycleLabel, a, b, c).elements())));
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
        val result = new ArrayList<Pair<String, Configuration>>();

        val signature = signature(config.getSpi(), config.getPi());
        val cyclesSizes = new HashMap<Integer, Integer>();
        val indexesByLabel = new HashMap<Integer, List<Integer>>();
        for (int i = 0; i < signature.length; i++) {
            cyclesSizes.putIfAbsent((int) Math.floor(signature[i]), 0);
            cyclesSizes.computeIfPresent((int) Math.floor(signature[i]), (k, v) -> v + 1);
            indexesByLabel.computeIfAbsent((int) Math.floor(signature[i]), s -> new ArrayList<>());
            int finalI = i;
            indexesByLabel.computeIfPresent((int) Math.floor(signature[i]), (k, v) -> {
                v.add(finalI);
                return v;
            });
        }

        val cycleIndex = cycleIndex(config.getSpi(), config.getPi());
        val cyclesByLabel = new HashMap<Integer, Cycle>();
        for (int i = 0; i < signature.length; i++) {
            final int _i = i;
            cyclesByLabel.computeIfAbsent((int) Math.floor(signature[i]), k -> cycleIndex[config.getPi().get(_i)]);
        }

        for (int label = 1; label <= config.getSpi().size(); label++) {
            if (!isOriented(signature, label)) {
                for (int a = 0; a < signature.length; a++) {
                    for (int b = a; b < signature.length; b++) {
                        float[] extendedSignature = unorientedExtension(signature, label, a, b).elements();

                        Configuration extension = ofSignature(extendedSignature);

                        if (remainsUnoriented(indexesByLabel.get(label), a, b)) {
                            if (extension.getOpenGates().size() <= 2) {
                                result.add(new Pair<>(String.format("a=%d b=%d, extended cycle: %s", a, b, cyclesByLabel.get(label)), extension));
                            }
                        } else if (cyclesSizes.get(label) == 3) {
                            val extensionPrime = extend(cyclesByLabel, label, signature, a, b);
                            val fractions = new float[]{0.01F, 0.03F, 0.05F, 0.02F, 0.04F};
                            for (int i = 0; i < fractions.length; i++) {
                                fractions[i] += label;
                            }

                            if (areSymbolsInCyclicOrder(extensionPrime, fractions)) { // otherwise, it accepts a 2-move
                                result.add(new Pair<>(String.format("a=%d b=%d, extended cycle: %s, turn oriented", a, b,
                                        cyclesByLabel.get(label)), Configuration.ofSignature(extensionPrime)));
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    public static boolean areSymbolsInCyclicOrder(final float[] elements, final float[] other) {
        int next = 0;

        outer:
        for (int i = 0; i < elements.length; i++) {
            if (elements[i] == other[next]) {
                for (int j = 0; j <= elements.length; j++) {
                    int index = (i + j) % elements.length;
                    if (Math.floor(elements[index]) == Math.floor(other[next % other.length])) {
                        if (elements[index] == other[next % other.length]) {
                            next++;
                            if (next > other.length) {
                                break outer;
                            }
                        } else {
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    private static float[] extend(final Map<Integer, Cycle> cyclesByLabel, final int label, float[] signature,
                                  final int a, final int b) {
        final Cycle cycle = cyclesByLabel.get(label).startingBy(cyclesByLabel.get(label).getMaxSymbol());

        float[] copiedsignature = new float[signature.length];
        System.arraycopy(signature, 0, copiedsignature, 0, signature.length);

        for (int i = cycle.getSymbols().length - 1; i >= 0; i--) {
            copiedsignature[cycle.getSymbols()[i]] += 0.01 * (i + 1);
        }

        float next = 0.05f;
        val positions = new int[]{a, b};
        val extension = new FloatArrayList(copiedsignature);
        int inserted = 0;
        for (int position : positions) {
            extension.beforeInsert(position + inserted, label + next);
            next -= 0.01f;
            inserted++;
        }
        extension.trimToSize();

        return extension.elements();
    }

    private static boolean remainsUnoriented(final List<Integer> indexes, final int... newIndices) {
        val intervals = new HashSet<Pair<Integer, Integer>>();

        for (val index : newIndices) {
            for (int i = 0; i < indexes.size(); i++) {
                int left = indexes.get(i);
                int right = indexes.get((i + 1) % indexes.size());
                if ((left < index && index <= right) ||
                        (right < left && (left < index || index <= right))) {
                    intervals.add(new Pair<>(left, right));
                }
            }
        }

        return intervals.size() == 1;
    }

    private static boolean isOriented(float[] signature, int label) {
        for (float s : signature) {
            if (s % 1 > 0 && Math.floor(s) == label) {
                return true;
            }
        }
        return false;
    }

    private static FloatArrayList unorientedExtension(final float[] signature, final int label, final int... positions) {
        Preconditions.checkArgument(1 < positions.length && positions.length <= 3);
        Arrays.sort(positions);
        val extension = new FloatArrayList(signature);
        for (int i = 0; i < positions.length; i++) {
            extension.beforeInsert(positions[i] + i, label);
        }
        extension.trimToSize();
        return extension;
    }

    @SneakyThrows
    private static void makeHtmlNavigation(final Configuration configuration, final String outputDir) {
        val file = new File(outputDir + "/dfs/" + configuration.getSpi() + ".html");

        val context = new VelocityContext();
        context.put("jsSpi", permutationToJsArray(configuration.getSpi()));
        context.put("spi", configuration.getSpi().toString());
        context.put("hashCode", configuration.hashCode());
        context.put("openGates", configuration.getOpenGates().toString());
        context.put("signature", configuration.getSignature().toString());
        context.put("threeNorm", configuration.getSpi().get3Norm());
        context.put("type1Extensions", buildExtensionData(type1Extensions(configuration), outputDir));
        context.put("type2Extensions", buildExtensionData(type2Extensions(configuration), outputDir));
        context.put("type3Extensions", buildExtensionData(type3Extensions(configuration), outputDir));

        val template = Velocity.getTemplate("templates/extensions.html");
        try (val writer = new FileWriter(file)) {
            template.merge(context, writer);
        }
    }

    static class SortOrExtendExtensions extends SortOrExtend {

        public SortOrExtendExtensions(final Configuration configuration, final String outputDir) {
            super(configuration, outputDir);
        }

        @Override
        protected void extend(Configuration canonical) {
            Stream.concat(Stream.concat(type1Extensions(canonical).stream(), type2Extensions(canonical).stream()), type3Extensions(canonical).stream())
                    .map(extension -> new SortOrExtendExtensions(extension.getSecond(), outputDir)).
                    forEach(ForkJoinTask::fork);
        }
    }
}
