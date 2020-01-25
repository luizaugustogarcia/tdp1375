package br.unb.cic.tdp.proof;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;
import java.util.TreeSet;

import org.paukov.combinatorics.Factory;

import com.google.common.primitives.Bytes;

import br.unb.cic.tdp.Util;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;

class OrientedCycleGreaterThan5 {

	/**
	 * Generate (4,3)-sequences to apply when there is a cycle in \spi with lenght
	 * greater or equals to 7 that doesn't allow the application of a 2-move.
	 *
	 * @return a list of cases.
	 */
	static List<Case> generate() {
		final var orientedCycle = new Cycle("0,3,4,1,5,2,6");
		return generate(orientedCycle, new byte[] { 0, 1, 2 });
	}

	private static List<Case> generate(Cycle orientedCycle, byte[] orientedTriple) {
		final var verifiedConfigurations = new HashSet<String>();

		final var result = new ArrayList<Case>();

		final var spi = new MulticyclePermutation(orientedCycle);

		final var initialVector = Factory.createVector(Bytes.asList(orientedCycle.getSymbols()));
		final var gen = Factory.createPermutationGenerator(initialVector);

		for (final var permutation : gen) {
			var pi = new Cycle(Bytes.toArray(permutation.getVector()));

			if (pi.areSymbolsInCyclicOrder(orientedTriple)) {
				final var rhos = Util.findSortingSequence(pi.getSymbols(), spi, new Stack<>(), spi.getNumberOfEvenCycles(),
						1.375F);

				if (rhos != null && !rhos.isEmpty()) {
					// If rhos.size() == 1, then there is a 2-move. We are only interested on the
					// cases which do not allow the application of a 2-move
					if (rhos.size() > 1) {
						final var signatures = new TreeSet<String>();
						for (final var symbol : pi.getSymbols()) {
							final var cr = Util.canonicalize(spi, pi.getStartingBy(symbol).getSymbols());
							signatures.add(cr.getValue0().toString());
						}

						if (!verifiedConfigurations.contains(signatures.toString())) {
							result.add(new Case(pi.getSymbols(), spi, rhos));
							verifiedConfigurations.add(signatures.toString());
						}
					}
				} else {
					throw new RuntimeException("ERROR");
				}
			}
		}

		return result;
	}
}
