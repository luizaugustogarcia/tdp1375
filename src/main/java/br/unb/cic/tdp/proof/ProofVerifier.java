package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.CommonOperations;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import cern.colt.list.ByteArrayList;

import java.util.*;
import java.util.stream.Collectors;

import static br.unb.cic.tdp.CommonOperations.CANONICAL_PI;

public class ProofVerifier {

    private static final Configuration INVALID_CONFIGURATION = new Configuration(new Cycle("0,1,2,3,4,5,6,7,8,9,10,11"),
            new MulticyclePermutation("(0,9,7)(1,6,3)(2,11,4)(5,10,8)"));

    public static void main(String[] args) {
        verify(new Configuration(new Cycle("0,1,2,3,4,5"), new MulticyclePermutation("(0,3,1)(2,5,4)")), new HashSet<>());
    }

    public static void verify(final Configuration configuration, final Set<Configuration> desimplifications) {
        final var spi = configuration.getSpi();

        if (configuration.equals(INVALID_CONFIGURATION) || desimplifications.contains(configuration)) {
            return;
        } else {
            // TODO log the full configurations
        }

        if (spi.get3Norm() == 9) {
            throw new RuntimeException("ERROR");
        }

        for (final var extension : type1Extensions(configuration)) {
            verify(extension, desimplifications);
        }

        for (final var extension : type2Extensions(configuration)) {
            verify(extension, desimplifications);
        }

        for (final var extension : type3Extensions(configuration)) {
            verify(extension, desimplifications);
        }
    }

    private static Set<Configuration> type1Extensions(final Configuration configuration) {
        final var pi = configuration.getPi();
        final var spi = configuration.getSpi();

        final var newCycleLabel = (byte) (spi.size() + 1);

        final var result = new HashSet<Configuration>();

        final var signature = CommonOperations.signature(spi, pi);
        for (int i = 0; i < pi.size(); i++) {
            if (signature[i] == signature[(i + 1) % pi.size()]) {
                final var a = (i + 1) % pi.size();
                for (int b = 0; b < pi.size(); b++) {
                    for (int c = b; c < pi.size(); c++) {
                        if (!(a == b && b == c)) {
                            final var extension = extend(signature, newCycleLabel, a, b, c);

                            final var cyclesByLabel = new HashMap<Byte, ByteArrayList>();
                            for (int j = extension.size() - 1; j >= 0; j--) {
                                final var label = extension.get(j);
                                cyclesByLabel.computeIfAbsent(label, l -> new ByteArrayList());
                                cyclesByLabel.get(label).add((byte) j);
                            }

                            final var _spi = cyclesByLabel.values().stream().map(Cycle::new)
                                    .collect(Collectors.toCollection(MulticyclePermutation::new));

                            result.add(new Configuration(CANONICAL_PI[_spi.getNumberOfSymbols()], _spi));
                        }
                    }
                }
            }
        }

        return result;
    }

    private static List<Configuration> type3Extensions(final Configuration configuration) {
        return Collections.emptyList();
    }

    private static List<Configuration> type2Extensions(final Configuration configuration) {
        return Collections.emptyList();
    }

    private static ByteArrayList extend(final byte[] signature, final byte nextLabel, final int... positions) {
        Arrays.sort(positions);
        final var extension = new ByteArrayList(signature);
        extension.beforeInsert(positions[0], nextLabel);
        extension.beforeInsert(positions[1] + 1, nextLabel);
        extension.beforeInsert(positions[2] + 2, nextLabel);
        return extension;
    }
}