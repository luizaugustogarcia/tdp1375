package br.unb.cic.tdp.base;

import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Getter
public class PivotedConfiguration {

    private final Configuration configuration;

    private final TreeSet<Integer> pivots;

    public static PivotedConfiguration of(final Configuration configuration, final TreeSet<Integer> pivots) {
        return new PivotedConfiguration(configuration, pivots);
    }

    public static PivotedConfiguration of(final String spi, final int... pivots) {
        return new PivotedConfiguration(new Configuration(spi), Arrays.stream(pivots).boxed().collect(Collectors.toCollection(TreeSet::new)));
    }

    public PivotedConfiguration getCanonical() {
        var canonical = this;
        var canonicalStr = this.toString();

        for (int i = 0; i < this.getConfiguration().getPi().size(); i++) {
            val rotation = rotate(i, this.getConfiguration().getSpi(), this.getPivots());
            val rotationStr = rotation.toString();
            if (rotationStr.compareTo(canonicalStr) < 0) {
                canonical = rotation;
                canonicalStr = rotationStr;
            }
            val reflection = mirror(rotation.getConfiguration().getSpi(), rotation.getPivots());
            val reflectionStr = reflection.toString();
            if (reflectionStr.compareTo(canonicalStr) < 0) {
                canonical = reflection;
                canonicalStr = reflectionStr;
            }
        }
        return canonical;
    }

    public List<PivotedConfiguration> getEquivalent() {
        val equivalent = new ArrayList<PivotedConfiguration>();
        for (int i = 0; i < this.getConfiguration().getPi().size(); i++) {
            val rotation = rotate(i, this.getConfiguration().getSpi(), this.getPivots());
            equivalent.add(rotation);
            // TODO: it is unclear why reflection does not preserve pivoting under the application of moves
            //val reflection = mirror(rotation.getConfiguration().getSpi(), rotation.getPivots());
            //equivalent.add(reflection);
        }
        return equivalent;
    }

    public String toString() {
        return "%s#%s".formatted(this.getConfiguration().getSpi().toString(), this.getPivots());
    }

    private PivotedConfiguration rotate(final int i, MulticyclePermutation spi, TreeSet<Integer> pivots) {
        var conjugator = CommonOperations.CANONICAL_PI[spi.getNumberOfSymbols()].getInverse();

        for (int j = 0; j < i; j++) {
            spi = spi.conjugateBy(conjugator);
        }

        for (int j = 0; j < i; j++) {
            pivots = pivots.stream().map(conjugator::image).collect(Collectors.toCollection(TreeSet::new));
        }

        return of(new Configuration(spi), pivots);
    }

    private PivotedConfiguration mirror(final MulticyclePermutation spi, final Set<Integer> pivots) {
        val pi = CommonOperations.CANONICAL_PI[spi.getNumberOfSymbols()];
        val conjugator = new MulticyclePermutation();
        for (var i = 0; i < pi.size() / 2; i++) {
            conjugator.add(Cycle.of(pi.get(i), pi.get(pi.size() - 1 - i)));
        }
        return of(new Configuration(spi.getInverse().conjugateBy(conjugator)), pivots.stream()
                .map(conjugator::image)
                .collect(Collectors.toCollection(TreeSet::new)));
    }
}
