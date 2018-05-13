package br.unb.cic.tdp.proof;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.paukov.combinatorics.Factory;
import org.paukov.combinatorics.Generator;
import org.paukov.combinatorics.ICombinatoricsVector;

import com.google.common.collect.Lists;
import com.google.common.primitives.Bytes;

import br.unb.cic.tdp.Util;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.permutation.MulticyclePermutation.ByteArrayRepresentation;
import br.unb.cic.tdp.permutation.PermutationGroups;

public class Oriented5Cycle {
	public static final byte a = 0, b = 1, c = 2, d = 3, e = 4, f = 5, g = 6, h = 7, i = 8;

	private static Set<String> cache = new HashSet<>();

	public static void main(String[] args) throws IOException {
		String outputFile = args[0];

		try (FileWriter writer = new FileWriter(outputFile, true);) {
			try (BufferedWriter out = new BufferedWriter(writer)) {
				for (Case _case : generateCases()) {
					out.write(_case.getSigmaPiInverse() + ";" + _case.getRhos().stream().map(r -> Arrays.toString(r).intern())
							.collect(Collectors.joining("-")) + "\n");
				}
			}
		}
	}

	public static List<Case> generateCases() {
		Cycle orientedCycle = new Cycle(a, d, b, e, c);
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

			try {
				PermutationGroups.computeProduct(sigmaPiInverse, pi.getInverse()).asNCycle();
			} catch (Exception e) {
				continue;
			}

			if (pi.areSymbolsInCyclicOrder(orientedTriple)) {
				List<byte[]> rhos = Util.findSequence(pi.getSymbols(), sigmaPiInverse, new Stack<>(),
						sigmaPiInverse.getNumberOfEvenCycles(), 1.5F);

				if (rhos != null && !rhos.isEmpty()) {
					if (rhos.size() > 1) {


						TreeSet<String> signature = new TreeSet<>();
						for (byte symbol : pi.getSymbols()) {
							pi = pi.getStartingBy(symbol);
							byte[] __pi = Arrays.copyOf(pi.getSymbols(), pi.size());
							MulticyclePermutation __sigmaPiInverse = Util.canonicalize(sigmaPiInverse, __pi, rhos);
							ByteArrayRepresentation byteArrayRepresentation = __sigmaPiInverse
									.byteArrayRepresentation();
							signature.add(byteArrayRepresentation.toString());
						}

						if (!cache.contains(signature.toString())) {
							cache.add(signature.toString());
							byte[] __pi = Arrays.copyOf(pi.getSymbols(), pi.size());
							MulticyclePermutation __sigmaPiInverse = Util.canonicalize(sigmaPiInverse, __pi, rhos);
							Case _case = new Case(__pi, __sigmaPiInverse, rhos);
							result.add(_case);
						}					}
				} else
					throw new RuntimeException("ERROR");
			}
		}

		return result;
	}
}
