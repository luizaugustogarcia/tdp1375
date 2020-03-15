package br.unb.cic.tdp.base;

import java.util.*;
import java.util.stream.Collectors;

public class Simplification {

    public static byte[] signature(final List<List<Float>> spi) {
        final var labelMap = new HashMap<List<Float>, Byte>();

        final var floatCyclesIndex = new TreeMap<Float, List<Float>>();
        for (final var cycle : spi) {
            for (final float symbol : cycle) {
                floatCyclesIndex.put(symbol, cycle);
            }
        }

        final var pi = floatCyclesIndex.keySet().stream().collect(Collectors.toList());

        final var signature = new byte[floatCyclesIndex.size()];

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

    private static boolean isSimple(final List<List<Float>> spi) {
        return spi.stream().noneMatch(c -> c.size() > 3);
    }

    /*

    // Requires a configuration whose \pi is canonical
    public static Set<Configuration> simplifications(final Configuration config, final Set<Configuration> collected) {
        if (isSimple(config.getSpi())) {
            collected.add(config);
            return collected;
        }

        final List<List<Float>> _spi = config.getSpi().stream()
                .map(ProofGenerator::toFloatsList)
                .collect(Collectors.toList());

        for (final var cycle : _spi) {
            if (cycle.size() > 3) {
                for (int i = 0; i < cycle.size(); i++) {
                    simplifications(simplify(_spi, cycle, i), collected);
                }
            }
        }

        return collected;
    }

    // Only works if \spi is related to a canonical \pi
    private static Configuration simplify(final List<List<Float>> spi, final List<Float> longCycle, final Integer startIndex) {
        final var _spi = new ArrayList<>(spi);
        final var newCycles = new ArrayList<List<Float>>();

        _spi.remove(longCycle);

        for (int j = 0; j < Math.ceil((double) longCycle.size() / 3); j++) {
            final var segment = new ArrayList<Float>();
            for (int k = 0; k < (j == 0 ? 3 : 2); k++) {
                segment.add(longCycle.get((k + startIndex + j * 3) % longCycle.size()));
            }
            if (j != 0) {
                segment.add(0, longCycle.get((startIndex + j * 3 - 1) % longCycle.size()) + 0.01F);
            }
            newCycles.add(new ArrayList<>(segment));
        }

        _spi.addAll(newCycles);

        final var _pi = _spi.stream().flatMap(Collection::stream)
                .sorted().collect(Collectors.toList());
        final var __pi = new ByteArrayList(_pi.size());


        final var substitution = new HashMap<Float, Byte>();
        for (int j = 0; j < _pi.size(); j++) {
            substitution.put(_pi.get(j), (byte) j);
            __pi.add((byte) j);
        }

        return new Configuration(_spi.stream().map(c -> {
            final var byteList = new ByteArrayList();
            for (final var symbol : c) {
                byteList.add(substitution.get(symbol));
            }
            return new Cycle(byteList);
        }).collect(toCollection(MulticyclePermutation::new)), new Cycle(__pi));
    }

    private static List<Float> toFloatsList(final Cycle cycle) {
        final var floatList = new ArrayList<Float>(cycle.size());
        for (final var symbol : cycle.getSymbols()) {
            floatList.add((float) symbol);
        }
        return floatList;
    }

    private static boolean isSimple(final MulticyclePermutation spi) {
        return spi.stream().noneMatch(c -> c.size() > 3);
    }
*/

}
