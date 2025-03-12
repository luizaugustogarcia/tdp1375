package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import com.google.common.primitives.Ints;
import lombok.val;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static br.unb.cic.tdp.permutation.PermutationGroups.computeProduct;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class ProofGenerator {
    /*
	\section*{Improved Upper Bound via an Average Bond Increase of 1.6 per Transposition}

	Recall that the identity permutation has $n-1$ bonds, while a worst-case permutation (e.g., the reverse permutation or other bondless configuration) starts with few or no bonds. In a sorting process by block moves, each transposition increases the total number of bonds.

	\subsection*{Original Argument}
	In the work by Eriksson et al., it is shown that for any permutation $\pi\in S_n$, one can perform a fixed sequence of 2 moves that increases the number of bonds by 3. This implies an average increase of
	\[
	\frac{3}{2} = 1.5 \quad \text{bonds per move}.
	\]
	Thus, since the identity has $n-1$ bonds, one deduces that
	\[
	d(n) \le 2 + d(n-3).
	\]

	\subsection*{Improved Bound with an Average Increase of 1.6 Bonds per Move}
	Suppose now that you have demonstrated that, for all $n\ge 15$, one can find sequences of block moves (not necessarily fixed two-move sequences) that yield an average bond increase of at least $1.6$ per move in the worst-case. In other words, if $k$ moves are performed, then the total increase in bonds is at least
	\[
	1.6k.
	\]
	****Since we must accumulate at least $n-1$ bonds to reach the sorted (identity) permutation**** (it does not need to accumulate n+1 (considering the zero)!!! the only permutation having k >= n-1 bonds is the identity!!!, we require:
	\[
	1.6k \ge n-1.
	\]
	Solving for $k$, we have:
	\[
	k \ge \frac{n-1}{1.6} = \frac{5(n-1)}{8}.
	\]
	Since the number of moves must be an integer, it follows that the worst-case number of moves required satisfies
	\[
	d(n) \le \left\lfloor \frac{5(n-1)}{8} \right\rfloor = \left\lfloor \frac{5n-5}{8} \right\rfloor.
	\]

	\subsection*{Conclusion}
	Thus, by showing that every transposition contributes on average at least 1.6 bonds, one immediately obtains the improved upper bound:
	\[
	\boxed{d(n) \le \left\lfloor \frac{5n-5}{8} \right\rfloor, \quad \text{for } n\ge 15.}
	\]
	This improvement over the previous bound stems directly from the enhanced average bond gain per move.
	     */
    public static void main(String[] args) throws Throwable {
        Velocity.setProperty("resource.loader", "class");
        Velocity.setProperty("parser.pool.size", Runtime.getRuntime().availableProcessors());
        Velocity.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        Velocity.init();

        val outputDir = args[0];
        Files.createDirectories(Paths.get(outputDir));

        Files.copy(ProofGenerator.class.getClassLoader().getResourceAsStream("index.html"),
                Paths.get(outputDir + "/index.html"), REPLACE_EXISTING);
        Files.copy(ProofGenerator.class.getClassLoader().getResourceAsStream("explain.html"),
                Paths.get(outputDir + "/explain.html"), REPLACE_EXISTING);
        Files.copy(ProofGenerator.class.getClassLoader().getResourceAsStream("draw-config.js"),
                Paths.get(outputDir + "/draw-config.js"), REPLACE_EXISTING);

        val stopWatch = StopWatch.createStarted();
        val minRate = Double.parseDouble(args[1]);
        System.out.println("Min rate: " + minRate);
        Extensions.generate(outputDir, minRate);
        stopWatch.stop();
        System.out.println(stopWatch.getTime(TimeUnit.MINUTES));
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

    public static void renderSorting(final Configuration canonicalConfig,
                                     final List<Cycle> sorting,
                                     final Writer writer) {
        val context = new VelocityContext();
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
        for (val move : sorting) {
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
