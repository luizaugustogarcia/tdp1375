package br.unb.cic.tdp.base;

import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import cern.colt.bitvector.BitVector;
import cern.colt.list.IntArrayList;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static br.unb.cic.tdp.base.CommonOperations.mod;

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

    private PivotedConfiguration rotate(final int i, final MulticyclePermutation spi, final TreeSet<Integer> pivots) {
        int numberOfSymbols = spi.getNumberOfSymbols();
        val conjugated = new short[numberOfSymbols];

        for (int x = 0; x < numberOfSymbols; x++) {
            val y = spi.image(x);
            conjugated[mod(x + (-1 * i), numberOfSymbols)] = (short) mod(y + (-1 * i), numberOfSymbols);
        }

        return of(new Configuration(toCycles(conjugated)), pivots.stream()
                .map(p -> mod(p + (-1 * i), numberOfSymbols))
                .collect(Collectors.toCollection(TreeSet::new)));
    }

    public static MulticyclePermutation toCycles(final short[] perm) {
        val n = perm.length;
        val visited = new BitVector(n);
        val cycles = new MulticyclePermutation();

        for (int i = 0; i < n; i++) {
            if (!visited.getQuick(i)) {
                val cycle = new IntArrayList();
                var current = i;

                while (!visited.getQuick(current)) {
                    visited.putQuick(current, true);
                    cycle.add(current);
                    current = perm[current];
                }

                val arr = new int[cycle.size()];
                for (int k = 0; k < cycle.size(); k++) {
                    arr[k] = (short) cycle.get(k);
                }
                cycles.add(Cycle.of(arr));
            }
        }

        return cycles;
    }
}
