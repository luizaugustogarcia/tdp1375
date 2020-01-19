package br.unb.cic.tdp.proof;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.paukov.combinatorics.Factory;

import com.google.common.primitives.Bytes;

import br.unb.cic.tdp.Util;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.permutation.PermutationGroups;
import br.unb.cic.tdp.permutation.MulticyclePermutation.CyclicRepresentation;

public class OddCyclesCases {

	/**
	 * Generate the 2-moves (i.e. (1,1)-sequences) to aply when we have two odd
	 * cycles in \spi$
	 *
	 * @return a list of cases.
	 */
	public static List<Case> generate1_1Cases() {
		final var result = new ArrayList<Case>();
		result.addAll(generate(new MulticyclePermutation("(0,1)(2,3)"), 1));
		return result;
	}

	/**
	 * Generate the (2,2)-sequences of 3-cycles to apply when we have two or four
	 * odd cycles in \spi$
	 *
	 * @return a list of generate cases.
	 */
	public static List<Case> generate2_2Cases() {
		final var result = new ArrayList<Case>();
		result.addAll(generate(new MulticyclePermutation("(0,1)(2,3)"), 2));
		result.addAll(generate(new MulticyclePermutation("(0,1)(2,3)(4,5)(6,7)"), 2));
		return result;
	}

	private static List<Case> generate(final MulticyclePermutation sigmaPiInverse, final int moves) {
		final var result = new ArrayList<Case>();

		final var symbols = new ArrayList<Byte>();
		sigmaPiInverse.stream().forEach(c -> symbols.addAll(Bytes.asList(c.getSymbols())));

		final var initialVector = Factory.createVector(symbols);
		final var gen = Factory.createPermutationGenerator(initialVector);

		final var cache = new HashSet<CyclicRepresentation>();

		permutation: for (final var permutation : gen) {
			final var pi = new Cycle(Bytes.toArray(permutation.getVector())).getInverse();

			for (final var rho1 : Util.searchAllApp3Cycles(pi.getSymbols())) {
				final var _sigmaPiInverse = PermutationGroups.computeProduct(sigmaPiInverse, new Cycle(rho1).getInverse());
				final var _pi = new Cycle(Util.applyTransposition(rho1, pi.getSymbols()));

				if (_sigmaPiInverse.getNumberOfEvenCycles() - sigmaPiInverse.getNumberOfEvenCycles() == 2) {
					if (moves == 1) {
						final var __pi = Arrays.copyOf(pi.getSymbols(), pi.size());
						final var __sigmaPiInverse = Util.canonicalize(sigmaPiInverse, __pi, Arrays.asList(rho1));
						final var byteArrayRepresentation = __sigmaPiInverse.cyclicRepresentation();

						if (!cache.contains(byteArrayRepresentation)) {
							cache.add(byteArrayRepresentation);
							result.add(new Case(__pi, __sigmaPiInverse, Arrays.asList(rho1)));
						}

						continue permutation;
					} else
						for (final var rho2 : Util.searchAllApp3Cycles(_pi.getSymbols())) {
							if (PermutationGroups.computeProduct(_sigmaPiInverse, new Cycle(rho2).getInverse())
									.getNumberOfEvenCycles() - _sigmaPiInverse.getNumberOfEvenCycles() == 2) {
								final var __pi = Arrays.copyOf(pi.getSymbols(), pi.size());
								final var __sigmaPiInverse = Util.canonicalize(sigmaPiInverse, __pi, Arrays.asList(rho1, rho2));
								final var byteArrayRepresentation = __sigmaPiInverse.cyclicRepresentation();

								if (!cache.contains(byteArrayRepresentation)) {
									cache.add(byteArrayRepresentation);
									result.add(new Case(__pi, __sigmaPiInverse, Arrays.asList(rho1, rho2)));
								}

								continue permutation;
							}
						}
				}
			}
		}

		return result;
	}
}
