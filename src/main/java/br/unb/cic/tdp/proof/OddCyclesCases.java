package br.unb.cic.tdp.proof;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.paukov.combinatorics.Factory;
import org.paukov.combinatorics.Generator;
import org.paukov.combinatorics.ICombinatoricsVector;

import com.google.common.collect.Lists;
import com.google.common.primitives.Bytes;

import br.unb.cic.tdp.Util;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.permutation.PermutationGroups;
import br.unb.cic.tdp.permutation.MulticyclePermutation.ByteArrayRepresentation;

public class OddCyclesCases {

	public static List<Case> get1_1Cases() {
		List<Case> result = new ArrayList<>();
		result.addAll(generate(new MulticyclePermutation("(0,1)(2,3)"), 1));
		return result;
	}

	public static List<Case> get2_2Cases() {
		List<Case> result = new ArrayList<>();
		result.addAll(generate(new MulticyclePermutation("(0,1)(2,3)"), 2));
		result.addAll(generate(new MulticyclePermutation("(0,1)(2,3)(4,5)(6,7)"), 2));
		return result;
	}

	private static List<Case> generate(MulticyclePermutation sigmaPiInverse, int moves) {
		List<Case> result = new ArrayList<>();

		List<Byte> symbols = Lists.newArrayList();
		sigmaPiInverse.stream().forEach(c -> symbols.addAll(Bytes.asList(c.getSymbols())));

		ICombinatoricsVector<Byte> initialVector = Factory.createVector(symbols);
		Generator<Byte> gen = Factory.createPermutationGenerator(initialVector);

		Set<ByteArrayRepresentation> cache = new HashSet<>();

		permutation: for (ICombinatoricsVector<Byte> permutation : gen) {
			Cycle pi = new Cycle(Bytes.toArray(permutation.getVector()));

			for (byte[] rho1 : Util.searchAllApp3Cycles(pi.getSymbols())) {
				MulticyclePermutation _sigmaPiInverse = PermutationGroups.computeProduct(sigmaPiInverse,
						new Cycle(rho1).getInverse());
				Cycle _pi = new Cycle(Util.applyTransposition(rho1, pi.getSymbols()));
				if (_sigmaPiInverse.getNumberOfEvenCycles() - sigmaPiInverse.getNumberOfEvenCycles() == 2) {
					if (moves == 1) {
						byte[] __pi = Arrays.copyOf(pi.getSymbols(), pi.size());
						MulticyclePermutation __sigmaPiInverse = Util.canonicalize(sigmaPiInverse, __pi,
								Arrays.asList(rho1));
						ByteArrayRepresentation byteArrayRepresentation = __sigmaPiInverse.byteArrayRepresentation();
						if (!cache.contains(byteArrayRepresentation)) {
							cache.add(byteArrayRepresentation);
							result.add(new Case(__pi, __sigmaPiInverse, Arrays.asList(rho1)));
						}
						continue permutation;
					} else
						for (byte[] rho2 : Util.searchAllApp3Cycles(_pi.getSymbols())) {
							if (PermutationGroups.computeProduct(_sigmaPiInverse, new Cycle(rho2).getInverse())
									.getNumberOfEvenCycles() - _sigmaPiInverse.getNumberOfEvenCycles() == 2) {
								byte[] __pi = Arrays.copyOf(pi.getSymbols(), pi.size());
								MulticyclePermutation __sigmaPiInverse = Util.canonicalize(sigmaPiInverse, __pi,
										Arrays.asList(rho1, rho2));
								ByteArrayRepresentation byteArrayRepresentation = __sigmaPiInverse
										.byteArrayRepresentation();
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
