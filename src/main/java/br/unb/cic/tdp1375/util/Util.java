package br.unb.cic.tdp1375.util;

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

import org.apache.commons.lang.StringUtils;
import org.paukov.combinatorics.Factory;
import org.paukov.combinatorics.Generator;
import org.paukov.combinatorics.ICombinatoricsVector;

import com.google.common.primitives.Bytes;

import br.unb.cic.tdp1375.permutations.Cycle;
import br.unb.cic.tdp1375.permutations.MulticyclePermutation;
import br.unb.cic.tdp1375.permutations.PermutationGroups;
import cern.colt.list.ByteArrayList;
import cern.colt.list.FloatArrayList;

public class Util {

	public static Cycle simplify(Cycle pi) {
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
	public static boolean isOpenGate(int left, int right, Cycle[] symbolToMuCycles, Collection<Cycle> mu,
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

	public static byte[] replace(byte[] array, byte[] substitution) {
		for (int i = 0; i < array.length; i++) {
			array[i] = substitution[array[i]];
		}
		return array;
	}

	public static MulticyclePermutation canonicalize(MulticyclePermutation sigmaPiInverse, byte[] pi,
			List<byte[]> rhos) {
		int max = 0;
		for (int i = 0; i < pi.length; i++)
			if (pi[i] > max)
				max = pi[i];

		byte[] substitution = new byte[max + 1];

		for (int i = 0; i < pi.length; i++) {
			substitution[pi[i]] = (byte) i;
		}

		replace(pi, substitution);

		for (byte[] rho : rhos) {
			replace(rho, substitution);
		}

		MulticyclePermutation mu = new MulticyclePermutation();

		for (Cycle cycle : sigmaPiInverse) {
			byte[] symbols = Arrays.copyOf(cycle.getSymbols(), cycle.size());
			replace(symbols, substitution);
			mu.add(new Cycle(symbols));
		}

		return mu;
	}

	/**
	 * Find a sequence whose approximation ratio lies between 1 and maxRatio
	 */
	public static List<byte[]> findSequence(byte[] pi, MulticyclePermutation mu, Stack<byte[]> rhos,
			int initalNumberOfEvenCycles, float maxRatio) {
		int n = pi.length;

		int lowerBound = (n - mu.getNumberOfEvenCycles()) / 2;
		float minAchievableRatio = (float) (rhos.size() + lowerBound) / ((n - initalNumberOfEvenCycles) / 2);

		// Does not allow it to exceed the upper bound
		if (minAchievableRatio <= maxRatio) {
			int delta = (mu.getNumberOfEvenCycles() - initalNumberOfEvenCycles);
			float instantRatio = delta > 0
					? (float) (rhos.size() * 2) / (mu.getNumberOfEvenCycles() - initalNumberOfEvenCycles)
					: 0;
			if (0 < instantRatio && instantRatio <= maxRatio) {
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
						List<byte[]> solution = findSequence(Util.applyTransposition(rho, pi),
								PermutationGroups.computeProduct(mu, new Cycle(rho).getInverse()), rhos,
								initalNumberOfEvenCycles, maxRatio);
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

	public static void desimplify(MulticyclePermutation sigmaPiInverse, Cycle pi, int depth) {
		System.out.println(StringUtils.repeat("\t", depth) + pi);

		for (ICombinatoricsVector<Cycle> combination : combinations(sigmaPiInverse, 2)) {
			List<byte[]> joiningPairs = getJoiningPairs(combination.getVector(), pi);
			for (byte[] joiningPair : joiningPairs) {
				if (joiningPair[0] == 0)
					continue;

				byte[] _newPi = removeAndReplace(pi, joiningPair);

				ByteArrayList copy = new ByteArrayList(Arrays.copyOf(_newPi, _newPi.length));
				copy.sort();

				byte[] sigma = new byte[copy.size()];
				byte[] newPi = new byte[_newPi.length];
				for (int i = 0; i < copy.size(); i++) {
					newPi[i] = (byte) copy.indexOf(_newPi[i]);
					sigma[i] = (byte) i;
				}

				desimplify(PermutationGroups.computeProduct(new Cycle(sigma), new Cycle(newPi).getInverse()),
						new Cycle(newPi), depth + 1);
			}
		}
	}

	private static byte[] removeAndReplace(Cycle pi, byte[] pair) {
		ByteArrayList temp = new ByteArrayList(Arrays.copyOf(pi.getSymbols(), pi.size()));
		temp.remove(temp.indexOf(pair[1]));
		temp.set(temp.indexOf(pair[0]), pair[1]);
		return Arrays.copyOfRange(temp.elements(), 0, temp.size());
	}

	private static List<byte[]> getJoiningPairs(List<Cycle> cycles, Cycle pi) {
		Set<Byte> symbols = new HashSet<>(Bytes.asList(pi.getSymbols()));

		Map<Byte, Byte> symbolToLabel = new HashMap<>();

		for (int i = 0; i < cycles.size(); i++) {
			for (int j = 0; j < cycles.get(i).size(); j++) {
				byte symbol = cycles.get(i).getSymbols()[j];
				symbolToLabel.put(symbol, (byte) i);
				symbols.remove(symbol);
			}
		}

		ByteArrayList _pi = new ByteArrayList(Arrays.copyOf(pi.getSymbols(), pi.size()));
		_pi.removeAll(new ByteArrayList(Bytes.toArray(symbols)));

		Cycle piCycle = new Cycle(pi.getSymbols());

		List<byte[]> results = new ArrayList<>();
		for (int i = 0; i < _pi.size(); i++) {
			byte currentLabel = symbolToLabel.get(_pi.get(i));
			byte nextLabel = symbolToLabel.get(_pi.get((i + 1) % _pi.size()));
			int currentSymbolIndex = piCycle.indexOf(_pi.get(i));
			int nextSymbolIndex = piCycle.indexOf(_pi.get((i + 1) % _pi.size()));
			if (currentLabel != nextLabel && nextSymbolIndex == (currentSymbolIndex + 1) % piCycle.size())
				results.add(new byte[] { _pi.get(i), _pi.get((i + 1) % _pi.size()) });
		}

		return results;
	}

	private static <T> Generator<T> combinations(Collection<T> collection, int k) {
		return Factory.createSimpleCombinationGenerator(Factory.createVector(collection), k);
	}
}
