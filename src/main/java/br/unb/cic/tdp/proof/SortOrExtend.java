package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.base.GPUSortingSearch;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.permutation.OneLinePermutation;
import com.google.common.primitives.Ints;
import lombok.AllArgsConstructor;
import lombok.val;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import java.io.File;
import java.io.FileWriter;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.RecursiveAction;
import java.util.stream.Collectors;

import static br.unb.cic.tdp.permutation.PermutationGroups.computeProduct;

@AllArgsConstructor
public abstract class SortOrExtend extends RecursiveAction {
    private static final SortingCoordinator<Configuration, Boolean> coordinator = new SortingCoordinator<>();

    private static GPUSortingSearch gpuSortingSearch;
    private static float minRate;
    private static int maxDepth;

    public static void init(
            final int devicesCount,
            final int slotsPerDevice,
            final long queueBytesBudget,
            final int dedupTableSize,
            final float maxRatio,
            final int maxDepth
    ) {
        SortOrExtend.gpuSortingSearch = new GPUSortingSearch(devicesCount, slotsPerDevice, queueBytesBudget, dedupTableSize);
        SortOrExtend.minRate = 2 / maxRatio;
        SortOrExtend.maxDepth = maxDepth;
    }

    protected final Configuration configuration;
    protected final String outputDir;

    @Override
    protected void compute() {

        val canonical = configuration.getCanonical();

        val result = coordinator.tryCompute(canonical, () -> {
            val sortingFile = new File(outputDir + "/" + canonical.getSpi() + ".html");
            if (sortingFile.exists()) {
                return Optional.of(Boolean.TRUE);
            }

            val badCase = new File(outputDir + "/bad-cases/" + canonical.getSpi());

            if (!badCase.exists()) {
                val sorting = searchForSorting(canonical);
                if (sorting.isPresent()) {
                    try (val file = new RandomAccessFile(outputDir + "/" + canonical.getSpi() + ".html", "rw")) {
                        try (val writer = new FileWriter(file.getFD())) {
                            renderSorting(canonical, sorting.get(), writer);
                            return Optional.of(Boolean.TRUE);
                        }
                    }
                } else {
                    try (val writer = new FileWriter(outputDir + "/bad-cases/" + canonical.getSpi())) {
                        // mark as bad case
                    }
                }
            }

            return Optional.empty();
        });

        // Only extend if we performed the computation and found no sorting
        // If another thread is already computing, exit early without extending
        if (result.isNotFound()) {
            extend(configuration);
        }
    }

    private Optional<List<Cycle>> searchForSorting(final Configuration configuration) {
        val pi = MulticyclePermutation.CANONICAL_PI_BYTE[configuration.getPi().size()];
        val spi = configuration.getSpi();
        val upperBound = Math.floor(spi.get3Norm() * (2 / minRate));
        val oneLinePermutation = new OneLinePermutation(spi.getOneLineNotation());
        val evenCycles = spi.getNumberOfEvenCycles();

        var sorting = gpuSortingSearch.search(pi, oneLinePermutation, evenCycles, minRate, 4, false);

        if (sorting.isEmpty() && upperBound > 4) {
            sorting = gpuSortingSearch.search(pi, oneLinePermutation, evenCycles, minRate, (int) Math.min(upperBound, 8), false);
        }

        if (sorting.isEmpty() && upperBound > 8) {
            sorting = gpuSortingSearch.search(pi, oneLinePermutation, evenCycles, minRate, (int) Math.min(upperBound, maxDepth), false);
        }

        if (!sorting.isEmpty()) {
            return Optional.of(sorting.stream().map(move -> Cycle.of(move[0], move[1], move[2])).toList());
        }

        return Optional.empty();
    }

    private static void renderSorting(final Configuration canonicalConfig, final List<Cycle> sorting, final Writer writer) {
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

    protected abstract void extend(final Configuration configuration);
}
