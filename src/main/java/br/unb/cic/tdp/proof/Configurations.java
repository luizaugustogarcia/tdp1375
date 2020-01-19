package br.unb.cic.tdp.proof;

import java.util.List;
import java.util.Set;

import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.permutation.MulticyclePermutation.CyclicRepresentation;

public class Configurations {

  public static void verify(final MulticyclePermutation spi, final Set<CyclicRepresentation> desimplifications) {
    if (spi.getNorm() > 8) {
      throw new RuntimeException("ERROR");
    }

    if (desimplifications.contains(spi.cyclicRepresentation())) {
      return;
    }

    for (final var extension : type1Extensions(spi)) {
      verify(extension, desimplifications);
    }

    for (final var extension : type2Extensions(spi)) {
      verify(extension, desimplifications);
    }

    for (final var extension : type3Extensions(spi)) {
      verify(extension, desimplifications);
    }
  }

  private static List<MulticyclePermutation> type3Extensions(MulticyclePermutation spi) {
    return null;
  }

  private static List<MulticyclePermutation> type2Extensions(MulticyclePermutation spi) {
    return null;
  }

  private static List<MulticyclePermutation> type1Extensions(MulticyclePermutation spi) {
    return null;
  }
}