package br.unb.cic.tdp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import org.javatuples.Triplet;
import org.paukov.combinatorics.Factory;
import org.paukov.combinatorics.Generator;
import org.paukov.combinatorics.ICombinatoricsVector;

import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.permutation.PermutationGroups;
import cern.colt.list.ByteArrayList;
import cern.colt.list.FloatArrayList;

public class Util {

	static Cycle simplify(Cycle pi) {
		FloatArrayList _pi = new FloatArrayList();
		for (int i = 0; i < pi.getSymbols().length; i++) {
			_pi.add(pi.getSymbols()[i]);
		}

		ByteArrayList sigma = new ByteArrayList();
		for (int i = 0; i < _pi.size(); i++) {
			sigma.add((byte) i);
		}

		MulticyclePermutation sigmaPiInverse = PermutationGroups.computeProduct(new Cycle(sigma), pi.getInverse());

		Cycle bigCycle;
		while ((bigCycle = sigmaPiInverse.stream().filter(c -> c.size() > 3).findFirst().orElse(null)) != null) {
			byte leftMostSymbol = leftMostSymbol(bigCycle, pi);
			float newSymbol = _pi.get(_pi.indexOf(leftMostSymbol) - 1) + 0.001F;
			_pi.beforeInsert(_pi.indexOf(bigCycle.pow(leftMostSymbol, -2)), newSymbol);

			FloatArrayList piCopy = new FloatArrayList(Arrays.copyOf(_pi.elements(), _pi.size()));
			piCopy.sort();

			ByteArrayList newPi = new ByteArrayList();
			for (int i = 0; i < piCopy.size(); i++) {
				newPi.add((byte) piCopy.indexOf(_pi.get(i)));
			}

			sigma = new ByteArrayList();
			for (int i = 0; i < newPi.size(); i++) {
				sigma.add((byte) i);
			}

			sigmaPiInverse = PermutationGroups.computeProduct(new Cycle(sigma), new Cycle(newPi).getInverse());

			_pi = new FloatArrayList();
			for (int i = 0; i < newPi.size(); i++) {
				_pi.add(newPi.get(i));
			}
			pi = new Cycle(newPi);
		}

		return pi.getStartingBy((byte) 0);
	}

	private static byte leftMostSymbol(Cycle bigCycle, Cycle pi) {
		for (int i = 1; i < pi.getSymbols().length; i++)
			if (bigCycle.contains(pi.get(i)))
				return pi.get(i);
		return -1;
	}

	public static byte[] applyTransposition(byte[] rho, byte[] pi) {
		byte a = rho[0], b = rho[1], c = rho[2];
		int indexes[] = new int[3];
		for (int i = 0; i < pi.length; i++) {
			if (pi[i] == a)
				indexes[0] = i;
			if (pi[i] == b)
				indexes[1] = i;
			if (pi[i] == c)
				indexes[2] = i;
		}
		Arrays.sort(indexes);
		byte[] result = new byte[pi.length];

		System.arraycopy(pi, 0, result, 0, indexes[0]);
		System.arraycopy(pi, indexes[1], result, indexes[0], indexes[2] - indexes[1]);
		System.arraycopy(pi, indexes[0], result, indexes[0] + (indexes[2] - indexes[1]), indexes[1] - indexes[0]);
		System.arraycopy(pi, indexes[2], result, indexes[2], pi.length - indexes[2]);

		return result;
	}

	public static byte[] signature(byte[] pi, List<Cycle> mu) {
		for (Cycle cycle : mu) {
			cycle.setLabel(-1);
		}

		Cycle[] symbolToMuCycle = new Cycle[mu.stream().mapToInt(c -> c.size()).sum()];
		for (Cycle muCycle : mu) {
			for (int symbol : muCycle.getSymbols()) {
				symbolToMuCycle[symbol] = muCycle;
			}
		}

		byte[] signature = new byte[pi.length];
		int lastLabel = 0;
		for (int i = 0; i < signature.length; i++) {
			int symbol = pi[i];
			Cycle cycle = symbolToMuCycle[symbol];
			if (cycle.getLabel() == -1) {
				lastLabel++;
				cycle.setLabel(lastLabel);
			}
			signature[i] = (byte) cycle.getLabel();
		}

		return signature;
	}

	// O(n)
	static boolean isOpenGate(int left, int right, Cycle[] symbolToMuCycles, Collection<Cycle> mu,
			Cycle piInverse) {
		int gates = left < right ? right - left : piInverse.size() - (left - right);
		for (int i = 1; i < gates; i++) {
			int index = (i + left) % piInverse.size();
			Cycle cycle = symbolToMuCycles[piInverse.get(index)];
			if (cycle != null && mu.contains(cycle))
				return false;
		}
		return true;
	}

	public static Map<Cycle, Integer> openGatesPerCycle(Collection<Cycle> mu, Cycle piInverse) {
		Cycle[] symbolToMuCycles = new Cycle[piInverse.size()];
		// O(1), since at this point, ||mu|| never exceeds 16
		for (Cycle cycle : mu)
			for (int i = 0; i < cycle.getSymbols().length; i++)
				symbolToMuCycles[cycle.getSymbols()[i]] = cycle;

		Map<Cycle, Integer> result = new HashMap<>();
		for (Cycle muCycle : mu) {
			for (int i = 0; i < muCycle.getSymbols().length; i++) {
				int left = piInverse.indexOf(muCycle.get(i));
				int right = piInverse.indexOf(muCycle.image(muCycle.get(i)));
				// O(n)
				if (isOpenGate(left, right, symbolToMuCycles, mu, piInverse)) {
					if (!result.containsKey(muCycle))
						result.put(muCycle, 0);
					result.put(muCycle, result.get(muCycle) + 1);
				}
			}
		}
		return result;
	}

	public static byte[] replace(byte[] array, byte a, byte b) {
		byte[] replaced = Arrays.copyOf(array, array.length);
		for (int i = 0; i < replaced.length; i++) {
			if (replaced[i] == a)
				replaced[i] = b;
		}
		return replaced;
	}

	static byte[] replace(byte[] array, byte[] substitution) {
		for (int i = 0; i < array.length; i++) {
			array[i] = substitution[array[i]];
		}
		return array;
	}

	public static Triplet<MulticyclePermutation, byte[], List<byte[]>> canonicalize(final MulticyclePermutation spi, final byte[] pi) {
		return canonicalize(spi, pi, null);
	}

	public static Triplet<MulticyclePermutation, byte[], List<byte[]>> canonicalize(final MulticyclePermutation spi, final byte[] pi,
			final List<byte[]> rhos) {
		int maxSymbol = 0;
		for (int i = 0; i < pi.length; i++)
			if (pi[i] > maxSymbol)
				maxSymbol = pi[i];

		final var substitutionMatrix = new byte[maxSymbol + 1];

		for (int i = 0; i < pi.length; i++) {
			substitutionMatrix[pi[i]] = (byte) i;
		}

		final var _pi = Arrays.copyOf(pi, pi.length);

		replace(_pi, substitutionMatrix);

		final var _rhos = new ArrayList<byte[]>();
		if (rhos != null) {
			for (byte[] rho : rhos) {
				final var _rho = Arrays.copyOf(rho, rho.length);
				replace(_rho, substitutionMatrix);
				_rhos.add(_rho);
			}
		}

		final var _spi = new MulticyclePermutation();

		for (Cycle cycle : spi) {
			final var _cycle = Arrays.copyOf(cycle.getSymbols(), cycle.size());
			replace(_cycle, substitutionMatrix);
			_spi.add(new Cycle(_cycle));
		}

		return new Triplet<>(_spi, _pi, _rhos);
	}

	/**
	 * Find a sorting sequence whose approximation ratio lies between 1 and
	 * <code>maxRatio</code>.
	 */
	public static List<byte[]> findSortingSequence(byte[] pi, MulticyclePermutation mu, Stack<byte[]> rhos,
			int initalNumberOfEvenCycles, float maxRatio) {
		return findSortingSequence(pi, mu, rhos, initalNumberOfEvenCycles, 1, maxRatio);
	}

	/**
	 * Find a sorting sequence whose approximation ratio lies between
	 * <code>minRatio</code> and <code>maxRatio</code>.
	 */
	private static List<byte[]> findSortingSequence(byte[] pi, MulticyclePermutation mu, Stack<byte[]> rhos,
			int initalNumberOfEvenCycles, float minRatio, float maxRatio) {
		int n = pi.length;

		int lowerBound = (n - mu.getNumberOfEvenCycles()) / 2;
		float minAchievableRatio = (float) (rhos.size() + lowerBound) / ((n - initalNumberOfEvenCycles) / 2);

		// Do not allow it to exceed the upper bound
		if (minAchievableRatio <= maxRatio) {
			int delta = (mu.getNumberOfEvenCycles() - initalNumberOfEvenCycles);
			float instantRatio = delta > 0
					? (float) (rhos.size() * 2) / (mu.getNumberOfEvenCycles() - initalNumberOfEvenCycles)
					: 0;
			if (0 < instantRatio && minRatio <= instantRatio && instantRatio <= maxRatio) {
				return rhos;
			} else {
				Set<Byte> fixedSymbols = new HashSet<>();
				for (Cycle c : mu) {
					if (c.size() == 1)
						fixedSymbols.add(c.get(0));
				}

				ByteArrayList newOmega = new ByteArrayList(n - fixedSymbols.size());
				for (byte symbol : pi) {
					if (!fixedSymbols.contains(symbol)) {
						newOmega.add(symbol);
					}
				}

				Cycle[] symbolToMuCycle = new Cycle[n];
				for (Cycle muCycle : mu) {
					if (muCycle.size() > 1)
						for (int symbol : muCycle.getSymbols()) {
							symbolToMuCycle[symbol] = muCycle;
						}
				}

				for (byte[] rho : searchAllApp3Cycles(Arrays.copyOfRange(newOmega.elements(), 0, newOmega.size()))) {
					if (is0Or2Move(rho, mu)) {
						rhos.push(rho);
						List<byte[]> solution = findSortingSequence(Util.applyTransposition(rho, pi),
								PermutationGroups.computeProduct(mu, new Cycle(rho).getInverse()), rhos, initalNumberOfEvenCycles,
								minRatio, maxRatio);
						if (!solution.isEmpty()) {
							return rhos;
						}
						rhos.pop();
					}
				}
			}
		}

		return Collections.emptyList();
	}

	private static boolean is0Or2Move(byte[] rho, MulticyclePermutation mu) {
		return PermutationGroups.computeProduct(mu, new Cycle(rho).getInverse()).getNumberOfEvenCycles() >= mu
				.getNumberOfEvenCycles();
	}

	public static List<byte[]> searchAllApp3Cycles(byte[] pi) {
		List<byte[]> result = new ArrayList<>();

		for (int i = 0; i < pi.length - 2; i++) {
			for (int j = i + 1; j < pi.length - 1; j++) {
				for (int k = j + 1; k < pi.length; k++) {
					int a = i, b = j, c = k;
					result.add(new byte[] { pi[a], pi[b], pi[c] });
				}
			}
		}

		return result;
	}

	static <T> Generator<T> combinations(Collection<T> collection, int k) {
		return Factory.createSimpleCombinationGenerator(Factory.createVector(collection), k);
	}

	public static Cycle searchFor2Move(MulticyclePermutation sigmaPiInverse, Cycle pi) {
		List<Cycle> oddCycles = sigmaPiInverse.stream().filter(c -> !c.isEven()).collect(Collectors.toList());
		for (Cycle c1 : oddCycles)
			for (Cycle c2 : oddCycles)
				if (c1 != c2) {
					for (Cycle a : get2CyclesSegments(c1))
						for (Byte b : c2.getSymbols()) {
							for (ICombinatoricsVector<Byte> rho : Util.combinations(Arrays.asList(a.get(0), a.get(1), b), 3)) {
								Cycle rho1 = new Cycle(rho.getVector().stream().mapToInt(i -> i).toArray());
								if (pi.areSymbolsInCyclicOrder(rho1.getSymbols())) {
									return rho1;
								}
							}
						}
				}

		for (Cycle cycle : sigmaPiInverse.stream().filter(c -> !pi.getInverse().isFactor(c)).collect(Collectors.toList())) {
			int before = cycle.isEven() ? 1 : 0;
			for (int i = 0; i < cycle.size() - 2; i++) {
				for (int j = i + 1; j < cycle.size() - 1; j++) {
					for (int k = j + 1; k < cycle.size(); k++) {
						byte a = cycle.get(i), b = cycle.get(j), c = cycle.get(k);
						if (pi.areSymbolsInCyclicOrder(a, b, c)) {
							int after = cycle.getK(a, b) % 2 == 1 ? 1 : 0;
							after += cycle.getK(b, c) % 2 == 1 ? 1 : 0;
							after += cycle.getK(c, a) % 2 == 1 ? 1 : 0;
							if (after - before == 2)
								return new Cycle(a, b, c);
						}
					}
				}
			}
		}

		return null;
	}

	static List<Cycle> get2CyclesSegments(Cycle cycle) {
		List<Cycle> result = new ArrayList<>();
		for (int i = 0; i < cycle.size(); i++) {
			result.add(new Cycle(cycle.get(i), cycle.image(cycle.get(i))));
		}
		return result;
	}
}
