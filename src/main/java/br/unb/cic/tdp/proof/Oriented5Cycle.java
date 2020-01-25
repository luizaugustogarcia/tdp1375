package br.unb.cic.tdp.proof;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import org.paukov.combinatorics.Factory;

import com.google.common.primitives.Bytes;

import br.unb.cic.tdp.Util;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;

class Oriented5Cycle {

	private static Set<String> verifiedConfigurations = new HashSet<>();

	static List<Case> generate() {
		final var orientedCycle = new Cycle("(0,3,1,4,2)");
		return generate(orientedCycle, new byte[] { 0, 1, 2 });
	}

	private static List<Case> generate(final Cycle orientedCycle, final byte[] orientedTriple) {
		final var result = new ArrayList<Case>();

		final var spi = new MulticyclePermutation(orientedCycle);

		final var symbols = new ArrayList<Byte>();
		symbols.addAll(Bytes.asList(orientedCycle.getSymbols()));

		final var initialVector = Factory.createVector(Bytes.asList(orientedCycle.getSymbols()));
		final var gen = Factory.createPermutationGenerator(initialVector);

		for (final var permutation : gen) {
			var pi = new Cycle(Bytes.toArray(permutation.getVector()));

			// It is the case to avoid combinations originating 2-moves because the symbols
			// in the only cycle of spi are indeed the symbols of the actual 5-cycle
			if (pi.areSymbolsInCyclicOrder(orientedTriple) && Util.searchFor2Move(spi, pi) == null) {
				final var rhos = Util.findSortingSequence(pi.getSymbols(), spi, new Stack<>(), spi.getNumberOfEvenCycles(),
						1.5F);

				if (rhos != null && !rhos.isEmpty()) {
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
