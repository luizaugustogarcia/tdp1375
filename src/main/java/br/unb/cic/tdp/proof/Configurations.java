package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.CommonOperations;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Configurations {

    private static final Cycle[] canonicalPis;

    static {
        canonicalPis = new Cycle[50];
        for (var i = 0; i < 50; i++) {
            final var pi = new byte[i + 1];
            for (var j = 0; j < i; j++) {
                pi[j] = (byte) j;
            }
            canonicalPis[i] = new Cycle(pi);
        }
    }

    public static void verify(final MulticyclePermutation spi, final Set<Configuration> ehDesimplifications) {
        if (spi.getNorm() > 8) {
            throw new RuntimeException("ERROR");
        }

        final var configuration = new Configuration(canonicalPis[spi.getNumberOfSymbols()], spi);
        if (ehDesimplifications.contains(configuration)) {
            return;
        } else {
            System.out.println("bad small component");
        }

        for (final var extension : type1Extensions(spi, null)) {
            verify(extension, ehDesimplifications);
        }

        for (final var extension : type2Extensions(spi)) {
            verify(extension, ehDesimplifications);
        }

        for (final var extension : type3Extensions(spi)) {
            verify(extension, ehDesimplifications);
        }
    }

    private static List<MulticyclePermutation> type3Extensions(final MulticyclePermutation spi) {
        return null;
    }

    private static List<MulticyclePermutation> type2Extensions(final MulticyclePermutation spi) {
        return null;
    }

    private static List<MulticyclePermutation> type1Extensions(final MulticyclePermutation spi, final Cycle pi) {
        return null;
    }
}