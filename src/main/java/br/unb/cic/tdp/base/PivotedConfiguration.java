package br.unb.cic.tdp.base;

import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.util.ShortArrayPacker;
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
import static br.unb.cic.tdp.base.CommonOperations.twoLinesNotation;

@RequiredArgsConstructor
@Getter
public class PivotedConfiguration {

    private final Configuration configuration;

    private final TreeSet<Integer> pivots;

    private final short[] spiTwoLinesNotation;

    public byte[] packedSpi;

    public byte[] packedPivots;

    public static PivotedConfiguration of(final Configuration configuration, final TreeSet<Integer> pivots) {
        return new PivotedConfiguration(configuration, pivots, twoLinesNotation(configuration.getSpi()));
    }

    public static PivotedConfiguration of(final Configuration configuration, final TreeSet<Integer> pivots, final short[] twoLinesNotation) {
        return new PivotedConfiguration(configuration, pivots, twoLinesNotation);
    }

    public static PivotedConfiguration of(final String spi, final int... pivots) {
        val config = new Configuration(spi);
        return new PivotedConfiguration(
                config,
                Arrays.stream(pivots).boxed().collect(Collectors.toCollection(TreeSet::new)),
                twoLinesNotation(config.getSpi())
        );
    }

    public PivotedConfiguration getCanonical() {
        val minRotation = leastLexRotationWithBooth(this.spiTwoLinesNotation);
        return of(new Configuration(toCycles(minRotation.rotation)), pivots.stream()
                .map(p -> mod(p + (-1 * minRotation.r), this.spiTwoLinesNotation.length))
                .collect(Collectors.toCollection(TreeSet::new)), minRotation.rotation);
    }

    public List<PivotedConfiguration> getEquivalent() {
        val equivalent = new ArrayList<PivotedConfiguration>();
        val spi = twoLinesNotation(this.getConfiguration().getSpi());
        for (int i = 0; i < this.getConfiguration().getPi().size(); i++) {
            val rotation = rotate(i, spi, this.getPivots());
            equivalent.add(rotation);
            // TODO reflection?
        }
        return equivalent;
    }

    public String toString() {
        return "%s#%s".formatted(this.getConfiguration().getSpi().toString(), this.getPivots());
    }

    private PivotedConfiguration rotate(final int i, final short[] spi, final TreeSet<Integer> pivots) {
        int numberOfSymbols = spi.length;
        val conjugated = new short[numberOfSymbols];

        for (int x = 0; x < numberOfSymbols; x++) {
            val y = spi[x];
            conjugated[mod(x + (-1 * i), numberOfSymbols)] = (short) mod(y + (-1 * i), numberOfSymbols);
        }

        return of(new Configuration(toCycles(conjugated)), pivots.stream()
                .map(p -> mod(p + (-1 * i), numberOfSymbols))
                .collect(Collectors.toCollection(TreeSet::new)));
    }

    public byte[] getPackedSpi() {
        if (this.packedSpi != null) return this.packedSpi;
        return this.packedSpi = ShortArrayPacker.encode(this.spiTwoLinesNotation);
    }

    public byte[] getPackedPivots() {
        if (this.packedPivots != null) return this.packedPivots;
        val pivotsArray = this.pivots.stream().mapToInt(Integer::intValue).toArray();
        val shortPivots = new short[pivotsArray.length];
        for (int i = 0; i < pivotsArray.length; i++) {
            shortPivots[i] = (short) pivotsArray[i];
        }
        return packedPivots = ShortArrayPacker.encode(shortPivots);
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

    public static final class MinRotation {
        public final int r;
        public final short[] rotation;

        public MinRotation(int r, short[] rotation) {
            this.r = r;
            this.rotation = rotation;
        }
    }

    public static MinRotation leastLexRotationWithBooth(short[] spi) {
        final int n = spi.length;
        if (n <= 1) {
            // r = 0; rotation = spi itself
            return new MinRotation(0, spi.clone());
        }

        // Booth-style scan with on-the-fly symbol computation:
        int i = 0, j = 1, k = 0;
        while (i < n && j < n && k < n) {
            int a = sym(spi, n, i, k);   // value at relative position k under shift i
            int b = sym(spi, n, j, k);   // value at relative position k under shift j
            if (a == b) {
                k++;
                continue;
            }
            if (a < b) {
                // shift j is worse; skip its first k+1 chars
                j += k + 1;
                if (j == i) j++;
            } else {
                // shift i is worse; skip its first k+1/chars
                i += k + 1;
                if (i == j) i++;
            }
            k = 0;
        }
        int r = Math.min(i, j) % n;

        // Build the rotation for that r using your definition.
        short[] rotation = new short[n];
        for (int x = 0; x < n; x++) {
            int y = spi[x];
            // rotation[mod(x - r, n)] = mod(y - r, n);
            int dst = mod(x - r, n);
            rotation[dst] = (short) mod(y - r, n);
        }
        return new MinRotation(r, rotation);
    }

    // On-the-fly symbol for Booth comparison: sym(r,k) = mod(spi[(r+k)%n] - r, n)
    private static int sym(short[] spi, int n, int r, int k) {
        int idx = r + k;
        if (idx >= n) idx -= n; // faster than %
        return mod(((int) spi[idx]) - r, n);
    }
}
