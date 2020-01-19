package br.unb.cic.tdp.proof;

import java.util.ArrayList;
import java.util.Arrays;
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

public class Oriented5Cycle {

	private static Set<String> cache = new HashSet<>();

	public static List<Case> generate() {
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
						final var signature = new TreeSet<String>();
						for (final var symbol : pi.getSymbols()) {
							pi = pi.getStartingBy(symbol);
							final var __pi = Arrays.copyOf(pi.getSymbols(), pi.size());
							final var __spi = Util.canonicalize(spi, __pi, rhos);
							final var cyclicRepresentation = __spi.cyclicRepresentation();
							signature.add(cyclicRepresentation.toString());
						}

						if (!cache.contains(signature.toString())) {
							cache.add(signature.toString());
							final var __pi = Arrays.copyOf(pi.getSymbols(), pi.size());
							final var __sigmaPiInverse = Util.canonicalize(spi, __pi, rhos);
							final var _case = new Case(__pi, __sigmaPiInverse, rhos);
							result.add(_case);
						}
					}
				} else
					throw new RuntimeException("ERROR");
			}
		}

		return result;
	}
}
