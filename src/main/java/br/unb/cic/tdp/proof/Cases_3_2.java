package br.unb.cic.tdp.proof;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import org.paukov.combinatorics.Factory;

import com.google.common.primitives.Bytes;

import br.unb.cic.tdp.Util;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;

class Cases_3_2 {

	/**
	 * Generate the (3,2)-sequences to apply when we have either two interleaving
	 * pairs or three intersecting 3-cycles in \spi.
	 *
	 * @return a list of cases.
	 */
	static List<Case> generate() {
		final var result = new ArrayList<Case>();
		result.add(new Case(new byte[] { 0, 1, 2, 3, 4, 5 }, new MulticyclePermutation("(0,4,2)(1,5,3)"),
				Arrays.asList(new byte[] { 0, 2, 4 }, new byte[] { 3, 1, 5 }, new byte[] { 2, 4, 0 })));
		result.addAll(generate(new MulticyclePermutation("(0,1,2)(3,4,5)(6,7,8)")));
		return result;
	}

	private static List<Case> generate(final MulticyclePermutation spi) {
		final var result = new ArrayList<Case>();

		final var initialVector = Factory.createVector(spi.getSymbols());
		final var gen = Factory.createPermutationGenerator(initialVector);

		final var verifiedConfigurations = new HashSet<Configuration>();

		for (final var permutation : gen) {
			var pi = new Cycle(Bytes.toArray(permutation.getVector()));

			var valid = true;
			for (final var cycle : spi) {
				if (!pi.getInverse().areSymbolsInCyclicOrder(cycle.getSymbols())) {
					valid = false;
					break;
				}
			}

			if (valid) {
				final var openGates = Util.openGatesPerCycle(spi, pi.getInverse());
				if (openGates.values().stream().mapToInt(j -> j.intValue()).sum() <= 2) {
					final var rhos = Util.findSortingSequence(pi.getSymbols(), spi, new Stack<>(), 3, 1.5F);

					if (rhos != null) {
						final var configuration = new Configuration(pi, spi);
						if (!verifiedConfigurations.contains(configuration)) {
							result.add(new Case(pi.getSymbols(), spi, rhos));
							verifiedConfigurations.add(configuration);
						}
					} else
						throw new RuntimeException("ERROR");
				}
			}
		}

		return result;
	}
}
