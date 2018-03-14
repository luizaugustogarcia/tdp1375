package br.unb.cic.tdp.proof;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

public class Cases_3_2 {

	public static List<Case> get3_2Cases() {
		List<Case> result = new ArrayList<>();
		result.add(new Case(new byte[] { 0, 1, 2, 3, 4, 5 }, new MulticyclePermutation("(0,4,2)(1,5,3)"),
				Arrays.asList(new byte[] { 0, 2, 4 }, new byte[] { 3, 1, 5 }, new byte[] { 2, 4, 0 })));
		result.addAll(generate(new MulticyclePermutation("(0,1,2)(3,4,5)(6,7,8)")));
		return result;
	}

	private static List<Case> generate(MulticyclePermutation sigmaPiInverse) {
		List<Case> result = new ArrayList<>();

		List<Byte> symbols = Lists.newArrayList();
		sigmaPiInverse.stream().forEach(c -> symbols.addAll(Bytes.asList(c.getSymbols())));

		ICombinatoricsVector<Byte> initialVector = Factory.createVector(symbols);
		Generator<Byte> gen = Factory.createPermutationGenerator(initialVector);

		Set<ByteArrayRepresentation> cache = new HashSet<>();

		for (ICombinatoricsVector<Byte> permutation : gen) {
			Cycle pi = new Cycle(Bytes.toArray(permutation.getVector()));

			boolean valid = true;
			for (Cycle cycle : sigmaPiInverse) {
				if (!pi.getInverse().areSymbolsInCyclicOrder(cycle.getSymbols())) {
					valid = false;
					break;
				}
			}

			if (valid) {
				Map<Cycle, Integer> openGates = Util.openGatesPerCycle(sigmaPiInverse, pi.getInverse());
				if (openGates.values().stream().mapToInt(j -> j.intValue()).sum() <= 2) {
					List<byte[]> rhos = Util.findSequence(pi.getSymbols(), sigmaPiInverse, new Stack<>(), 3, 1.5F);
					if (rhos != null) {
						byte[] _pi = Arrays.copyOf(pi.getSymbols(), pi.size());
						MulticyclePermutation _sigmaPiInverse = Util.canonicalize(sigmaPiInverse, _pi, rhos);
						ByteArrayRepresentation byteArrayRepresentation = _sigmaPiInverse.byteArrayRepresentation();
						if (!cache.contains(byteArrayRepresentation)) {
							cache.add(byteArrayRepresentation);
							result.add(new Case(_pi, _sigmaPiInverse, rhos));
						}
					} else
						throw new RuntimeException("ERROR");
				}
			}
		}

		return result;
	}
}
