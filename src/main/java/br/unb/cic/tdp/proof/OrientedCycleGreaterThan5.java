package br.unb.cic.tdp.proof;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.paukov.combinatorics.Factory;
import org.paukov.combinatorics.Generator;
import org.paukov.combinatorics.ICombinatoricsVector;

import com.google.common.collect.Lists;
import com.google.common.primitives.Bytes;

import br.unb.cic.tdp.Util;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.permutation.MulticyclePermutation.ByteArrayRepresentation;

public class OrientedCycleGreaterThan5 {
	public static final byte a = 0, b = 1, c = 2, d = 3, e = 4, f = 5, g = 6, h = 7, i = 8;

	private static Set<ByteArrayRepresentation> cache = new HashSet<>();

	public static List<Case> get11_8Cases() {
		Cycle orientedCycle = new Cycle(a, d, e, b, f, c, g);
		return generate(orientedCycle, new byte[] { a, b, c });
	}

	private static List<Case> generate(Cycle orientedCycle, byte[] orientedTriple) {
		List<Case> result = new ArrayList<>();

		MulticyclePermutation sigmaPiInverse = new MulticyclePermutation(orientedCycle);

		List<Byte> symbols = Lists.newArrayList();
		symbols.addAll(Bytes.asList(orientedCycle.getSymbols()));

		ICombinatoricsVector<Byte> initialVector = Factory.createVector(Bytes.asList(orientedCycle.getSymbols()));
		Generator<Byte> gen = Factory.createPermutationGenerator(initialVector);

		for (ICombinatoricsVector<Byte> permutation : gen) {
			Cycle pi = new Cycle(Bytes.toArray(permutation.getVector()));
			if (pi.areSymbolsInCyclicOrder(orientedTriple)) {
				List<byte[]> rhos = Util.findSequence(pi.getSymbols(), sigmaPiInverse, new Stack<>(),
						sigmaPiInverse.getNumberOfEvenCycles(), 1.375F);

				if (rhos != null && !rhos.isEmpty()) {
					// if rhos.size() == 1, then there is a 2-move on pi
					if (rhos.size() > 1) {
						byte[] __pi = Arrays.copyOf(pi.getSymbols(), pi.size());
						MulticyclePermutation __sigmaPiInverse = Util.canonicalize(sigmaPiInverse, __pi, rhos);
						ByteArrayRepresentation byteArrayRepresentation = __sigmaPiInverse.byteArrayRepresentation();
						if (!cache.contains(byteArrayRepresentation)) {
							cache.add(byteArrayRepresentation);
							Case _case = new Case(__pi, __sigmaPiInverse, rhos);
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
