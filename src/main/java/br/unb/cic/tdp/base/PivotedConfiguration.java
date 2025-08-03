package br.unb.cic.tdp.base;

import br.unb.cic.tdp.permutation.MulticyclePermutation;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
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
        // TODO cache
        var canonical = this;
        var canonicalStr = this.toString();

        for (int i = 0; i < this.getConfiguration().getPi().size(); i++) {
            val rotation = rotate(i, this.getConfiguration().getSpi(), this.getPivots());
            val rotationStr = rotation.toString();
            if (rotationStr.compareTo(canonicalStr) < 0) {
                canonical = rotation;
                canonicalStr = rotationStr;
            }
            // TODO reflection?
        }
        return canonical;
    }

    public List<PivotedConfiguration> getEquivalent() {
        val equivalent = new ArrayList<PivotedConfiguration>();
        for (int i = 0; i < this.getConfiguration().getPi().size(); i++) {
            val rotation = rotate(i, this.getConfiguration().getSpi(), this.getPivots());
            equivalent.add(rotation);
            // TODO reflection?
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
}
