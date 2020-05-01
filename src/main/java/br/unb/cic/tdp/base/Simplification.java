package br.unb.cic.tdp.base;

import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Floats;
import org.apache.commons.lang.ArrayUtils;

import java.util.*;
import java.util.stream.Collectors;

import static br.unb.cic.tdp.permutation.PermutationGroups.computeProduct;

public class Simplification {

    public static float[] signature(final List<List<Float>> spi) {
        final var labelMap = new HashMap<List<Float>, Byte>();

        final var floatCyclesIndex = new TreeMap<Float, List<Float>>();
        for (final var cycle : spi) {
            for (final float symbol : cycle) {
                floatCyclesIndex.put(symbol, cycle);
            }
        }

        final var pi = floatCyclesIndex.keySet().stream().collect(Collectors.toList());

        final var signature = new float[floatCyclesIndex.size()];

        for (var i = 0; i < signature.length; i++) {
            final var symbol = pi.get(i);
            final var cycle = floatCyclesIndex.get(symbol);
            labelMap.computeIfAbsent(cycle, c -> (byte) (labelMap.size() + 1));
            signature[i] = labelMap.get(cycle);
        }

        return signature;
    }

    // Requires a configuration whose \pi is canonical
    public static Set<List<List<Float>>> simplifications(final List<List<Float>> spi,
                                                         final Set<List<List<Float>>> collected) {
        if (isSimple(spi)) {
            collected.add(spi);
            return collected;
        }

        for (final var cycle : spi) {
            if (cycle.size() > 3) {
                for (int i = 0; i < cycle.size(); i++) {
                    simplifications(simplify(spi, cycle, i), collected);
                }
            }
        }

        return collected;
    }

    // Only works if \spi is related to a canonical \pi
    private static List<List<Float>> simplify(final List<List<Float>> spi, final List<Float> longCycle,
                                              final Integer breakIndex) {
        final var _spi = new ArrayList<>(spi);

        _spi.remove(longCycle);

        final var segment = new ArrayList<Float>();
        segment.add(longCycle.get(breakIndex));
        segment.add(longCycle.get((breakIndex + 1) % longCycle.size()));
        segment.add(longCycle.get((breakIndex + 2) % longCycle.size()));

        final var remaining = new ArrayList<Float>();
        remaining.add(segment.get(0) + 0.01F);

        for (int i = 0; i < longCycle.size() - 3; i++) {
            remaining.add(longCycle.get((breakIndex + i + 3) % longCycle.size()));
        }

        _spi.add(segment);
        _spi.add(remaining);

        return _spi;
    }

    public static boolean isSimple(final List<List<Float>> spi) {
        return spi.stream().noneMatch(c -> c.size() > 3);
    }

    public static List<Cycle> mimicSorting(final List<Float> simplificationPi,
                                           final List<List<Float>> simplificationSorting) {
        final var sorting = new ArrayList<Cycle>();

        final var omegas = new ArrayList<List<Float>>();
        omegas.add(simplificationPi);

        for (final var rho : simplificationSorting) {
            omegas.add(applyTransposition(omegas.get(omegas.size() - 1), rho));
        }

        final var remove = new HashSet<Float>();
        final var replaceBy = new HashMap<Float, Float>();
        float previousSymbol = -1;
        for (int i = 0; i < simplificationPi.size(); i++) {
            final var s = simplificationPi.get(i);
            if ((float) Math.floor(s) == (float) Math.floor(previousSymbol)) {
                remove.add(previousSymbol);
                replaceBy.put(s, (float) Math.floor(s));
            }
            previousSymbol = s;
        }

        for (int i = 1; i < omegas.size(); i++) {
            var temp = new ArrayList<>(omegas.get(i - 1));
            temp.removeAll(remove);
            temp.replaceAll(s -> replaceBy.getOrDefault(s, s));
            final var pi = new Cycle(Bytes.toArray(temp));

            temp = new ArrayList<>(omegas.get(i));
            temp.removeAll(remove);
            temp.replaceAll(s -> replaceBy.getOrDefault(s, s));
            final var _pi = new Cycle(Bytes.toArray(temp));

            final var rho = computeProduct(false, _pi, pi.getInverse());
            if (!rho.isIdentity()) {
                sorting.add(rho.asNCycle());
            }
        }

        return sorting;
    }

    public static List<Float> applyTransposition(final List<Float> pi, final List<Float> rho) {
        final var a = rho.get(0);
        final var b = rho.get(1);
        final var c = rho.get(2);

        final var indexes = new int[3];
        for (var i = 0; i < pi.size(); i++) {
            if (pi.get(i).floatValue() == a)
                indexes[0] = i;
            if (pi.get(i).floatValue() == b)
                indexes[1] = i;
            if (pi.get(i).floatValue() == c)
                indexes[2] = i;
        }

        Arrays.sort(indexes);

        final var _pi = ArrayUtils.toPrimitive(pi.toArray(new Float[0]), 0.0F);
        final var result = new float[pi.size()];
        System.arraycopy(_pi, 0, result, 0, indexes[0]);
        System.arraycopy(_pi, indexes[1], result, indexes[0], indexes[2] - indexes[1]);
        System.arraycopy(_pi, indexes[0], result, indexes[0] + (indexes[2] - indexes[1]), indexes[1] - indexes[0]);
        System.arraycopy(_pi, indexes[2], result, indexes[2], pi.size() - indexes[2]);

        return new ArrayList<>(Floats.asList(result));
    }

    public static List<List<Float>> simplificationSorting(final Configuration equivalentConfig,
                                                          final List<Cycle> sorting,
                                                          final Configuration simplifiedConfig,
                                                          final List<Float> simplificationPi) {
        final var result = new ArrayList<List<Float>>();

        var sPi = simplifiedConfig.getPi();
        var fPi = simplificationPi;

        for (final var rho : simplifiedConfig.translatedSorting(equivalentConfig, sorting)) {
            final var finalSPi = sPi;
            final var finalFPi = fPi;

            result.add(Bytes.asList(rho.getSymbols()).stream()
                    .map(s -> finalFPi.get(finalSPi.indexOf(s))).collect(Collectors.toList()));

            sPi = CommonOperations.applyTransposition(sPi, rho);
            fPi = applyTransposition(fPi, result.get(result.size() - 1));
        }

        return result;
    }

    public static List<List<Float>> simplificationSorting(final Configuration simplifiedConfig,
                                                          final List<Cycle> sorting, final List<Float> simplificationPi) {
        var pi = simplifiedConfig.getPi();
        var _simplificationPi = simplificationPi;

        final var simplificationSorting = new ArrayList<List<Float>>();

        for (final var rho : sorting) {
            final var finalPi = pi;
            final var fSimplificationPi = _simplificationPi;

            simplificationSorting.add(Bytes.asList(rho.getSymbols()).stream()
                    .map(s -> fSimplificationPi.get(finalPi.indexOf(s))).collect(Collectors.toList()));

            pi = CommonOperations.applyTransposition(pi, rho);
            _simplificationPi = Simplification.applyTransposition(_simplificationPi,
                    simplificationSorting.get(simplificationSorting.size() - 1));
        }

        return simplificationSorting;
    }

    public static List<List<Float>> toListOfListOfFloats(final MulticyclePermutation spi) {
        return spi.stream().map(Simplification::toFloatsList).collect(Collectors.toList());
    }

    public static List<Float> toFloatsList(final Cycle cycle) {
        final var floatList = new ArrayList<Float>(cycle.size());
        for (final var symbol : cycle.getSymbols()) {
            floatList.add((float) symbol);
        }
        return floatList;
    }
}
