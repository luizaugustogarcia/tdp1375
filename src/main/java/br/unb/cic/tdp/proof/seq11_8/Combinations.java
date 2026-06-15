package br.unb.cic.tdp.proof.seq11_8;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.proof.SortOrExtend;
import br.unb.cic.tdp.util.Pair;
import cern.colt.list.FloatArrayList;
import com.google.common.primitives.Floats;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

import static br.unb.cic.tdp.proof.SortOrExtend.permutationToJsArray;

public class Combinations {

    private static final Configuration ORIENTED_5_CYCLE = new Configuration(
            new MulticyclePermutation("(0,3,1,4,2)"));
    public static final Configuration INTERLEAVING_PAIR = new Configuration(
            new MulticyclePermutation("(0,4,2)(1,5,3)"));
    private static final Configuration NECKLACE_SIZE_4 = new Configuration(
            new MulticyclePermutation("(0,10,2)(1,5,3)(4,8,6)(7,11,9)"));
    private static final Configuration TWISTED_NECKLACE_SIZE_4 = new Configuration(
            new MulticyclePermutation("(0,7,5)(1,11,9)(2,6,4)(3,10,8)"));
    private static final Configuration NECKLACE_SIZE_5 = new Configuration(
            new MulticyclePermutation("(0,4,2)(1,14,12)(3,7,5)(6,10,8)(9,13,11)"));
    private static final Configuration NECKLACE_SIZE_6 = new Configuration(
            new MulticyclePermutation("(0,16,2)(1,5,3)(4,8,6)(7,11,9)(10,14,12)(13,17,15)"));

    public static final Configuration[] BAD_SMALL_COMPONENTS =
            new Configuration[]{ORIENTED_5_CYCLE, INTERLEAVING_PAIR, NECKLACE_SIZE_4,
                    TWISTED_NECKLACE_SIZE_4, NECKLACE_SIZE_5, NECKLACE_SIZE_6};

    @SneakyThrows
    public static void generate(final String outputDir) {
        Files.createDirectories(Paths.get(outputDir + "/comb/"));
        Files.createDirectories(Paths.get(outputDir + "/comb/bad-cases/"));

        try (var pool = new ForkJoinPool()) {
            for (val configuration : BAD_SMALL_COMPONENTS) {
                pool.execute(new SortOrExtendCombinations(configuration, outputDir + "/comb/"));
            }
        }

        try (var pool = new ForkJoinPool()) {
            Arrays.stream(BAD_SMALL_COMPONENTS).forEach(c -> pool.execute(new MakeHtmlNavigation(c, outputDir)));
        }
    }

    private static List<Pair<String, Configuration>> extend(final Configuration config) {
        val result = new ArrayList<Pair<String, Configuration>>();
        for (val badSmallComponent : BAD_SMALL_COMPONENTS) {
            for (int i = 0; i < config.getPi().size(); i++) {
                val badSmallComponentSignature = badSmallComponent.getSignature().getContent().clone();
                for (int j = 0; j < badSmallComponentSignature.length; j++) {
                    badSmallComponentSignature[j] += config.getSpi().size();
                }
                val signature = new FloatArrayList(config.getSignature().getContent().clone());
                signature.beforeInsertAllOf(i, Floats.asList(badSmallComponentSignature));
                signature.trimToSize();

                final String info;
                if (badSmallComponent == ORIENTED_5_CYCLE)
                    info = "bad oriented 5-cycle";
                else if (badSmallComponent == INTERLEAVING_PAIR)
                    info = "unoriented interleaving pair";
                else if (badSmallComponent == NECKLACE_SIZE_4)
                    info = "unoriented necklace of size 4";
                else if (badSmallComponent == TWISTED_NECKLACE_SIZE_4)
                    info = "unoriented twisted necklace of size 4";
                else if (badSmallComponent == NECKLACE_SIZE_5)
                    info = "unoriented necklace of size 5";
                else
                    info = "unoriented necklace of size 6";

                result.add(new Pair<>("pos=" + i + " add " + info, Configuration.ofSignature(signature.elements())));
            }
        }
        return result;
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

    @AllArgsConstructor
    static class MakeHtmlNavigation extends RecursiveAction {
        final Configuration configuration;
        final String outputDir;

        @SneakyThrows
        @Override
        protected void compute() {
            val file = new File(outputDir + "/comb/" + configuration.getSpi() + ".html");
            if (file.exists())
                return;

            val extensions = buildExtensionData(configuration, outputDir);

            val context = new VelocityContext();
            context.put("jsSpi", permutationToJsArray(configuration.getSpi()));
            context.put("spi", configuration.getSpi().toString());
            context.put("hashCode", configuration.hashCode());
            context.put("signature", configuration.getSignature().toString());
            context.put("threeNorm", configuration.getSpi().get3Norm());
            context.put("extensions", extensions);

            val template = Velocity.getTemplate("templates/combinations.html");
            try (val writer = new FileWriter(file)) {
                template.merge(context, writer);
            }

            // fork tasks for bad extensions
            for (val extension : extend(configuration)) {
                val canonical = extension.getSecond().getCanonical();
                val badCaseFile = new File(outputDir + "/comb/bad-cases/" + canonical.getSpi());
                if (badCaseFile.exists()) {
                    new MakeHtmlNavigation(extension.getSecond(), outputDir).fork();
                }
            }
        }

        private List<ExtensionData> buildExtensionData(final Configuration configuration, final String outputDir) {
            val result = new ArrayList<ExtensionData>();
            for (val extension : extend(configuration)) {
                val canonical = extension.getSecond().getCanonical();
                val badCaseFile = new File(outputDir + "/comb/bad-cases/" + canonical.getSpi());
                val hasSorting = !badCaseFile.exists();

                result.add(new ExtensionData(
                        extension.getFirst(),
                        hasSorting,
                        extension.getSecond().hashCode(),
                        extension.getSecond().getSpi().get3Norm(),
                        extension.getSecond().getSignature().toString(),
                        permutationToJsArray(extension.getSecond().getSpi()),
                        extension.getSecond().getSpi().toString(),
                        canonical.getSpi().toString()
                ));
            }
            return result;
        }
    }

    static class SortOrExtendCombinations extends SortOrExtend {

        public SortOrExtendCombinations(final Configuration configuration, final String outputDir) {
            super(configuration, outputDir);
        }

        @Override
        protected void extend(Configuration canonical) {
            if (configuration.get3Norm() > 7) {
                System.out.println("BAD: Combination does not allow (11/8): " + canonical.getSpi());
            }

            extendCombinations(configuration).stream().map(extension -> new SortOrExtendCombinations(extension, outputDir)).forEach(ForkJoinTask::fork);
        }

        private List<Configuration> extendCombinations(final Configuration config) {
            val result = new ArrayList<Configuration>();
            for (val badSmallComponent : BAD_SMALL_COMPONENTS) {
                for (int i = 0; i < config.getPi().size(); i++) {
                    val badSmallComponentSignature = badSmallComponent.getSignature().getContent().clone();
                    for (int j = 0; j < badSmallComponentSignature.length; j++) {
                        badSmallComponentSignature[j] += config.getSpi().size();
                    }
                    val signature = new FloatArrayList(config.getSignature().getContent().clone());
                    signature.beforeInsertAllOf(i, Floats.asList(badSmallComponentSignature));
                    signature.trimToSize();

                    result.add(Configuration.ofSignature(signature.elements()));
                }
            }
            return result;
        }
    }
}
