package br.unb.cic.tdp1375.permutations;

import java.util.Arrays;
import java.util.Collection;

import cc.redberry.core.utils.BitArray;
import cern.colt.list.ByteArrayList;

public class PermutationGroups {

	public static MulticyclePermutation computeProduct(Collection<Permutation> permutations) {
		return computeProduct(true, permutations.toArray(new Permutation[permutations.size()]));
	}

	public static MulticyclePermutation computeProduct(Permutation... permutations) {
		return computeProduct(true, permutations);
	}

	public static MulticyclePermutation computeProduct(boolean include1Cycle, Permutation... p) {
		int n = 0;
		for (Permutation p1 : p) {
			if (p1 instanceof Cycle) {
				n = Math.max(((Cycle) p1).getMaxSymbol(), n);
			} else {
				for (Cycle c : ((MulticyclePermutation) p1)) {
					n = Math.max(c.getMaxSymbol(), n);
				}
			}
		}
		return computeProduct(include1Cycle, n + 1, p);
	}

	public static MulticyclePermutation computeProduct(boolean include1Cycle, int n, Permutation... permutations) {
		byte[][] functions = new byte[permutations.length][n];

		// initializing
		for (int i = 0; i < permutations.length; i++)
			Arrays.fill(functions[i], (byte) -1);

		for (int i = 0; i < permutations.length; i++) {
			if (permutations[i] instanceof Cycle) {
				Cycle cycle = (Cycle) permutations[i];
				for (int j = 0; j < cycle.size(); j++) {
					functions[i][cycle.get(j)] = cycle.image(cycle.get(j));
				}
			} else {
				for (Cycle cycle : ((MulticyclePermutation) permutations[i])) {
					for (int j = 0; j < cycle.size(); j++) {
						functions[i][cycle.get(j)] = cycle.image(cycle.get(j));
					}
				}
			}
		}

		MulticyclePermutation result = new MulticyclePermutation();

		ByteArrayList cycle = new ByteArrayList();
		BitArray seen = new BitArray(n);
		int counter = 0;
		while (counter < n) {
			byte start = (byte) seen.nextZeroBit(0);

			byte image = start;
			for (int i = functions.length - 1; i >= 0; i--) {
				image = functions[i][image] == -1 ? image : functions[i][image];
			}

			if (image == start) {
				++counter;
				seen.set(start);
				if (include1Cycle)
					result.add(new Cycle(start));
				continue;
			}
			while (!seen.get(start)) {
				seen.set(start);
				++counter;
				cycle.add(start);

				image = start;
				for (int i = functions.length - 1; i >= 0; i--) {
					image = functions[i][image] == -1 ? image : functions[i][image];
				}

				start = image;
			}

			result.add(new Cycle(Arrays.copyOfRange(cycle.elements(), 0, cycle.size())));
			cycle.clear();
		}

		return result;
	}
}
