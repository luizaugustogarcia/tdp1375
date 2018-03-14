package br.unb.cic.tdp;

import static br.unb.cic.tdp.permutation.PermutationGroups.computeProduct;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.math3.util.Pair;

import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.proof.Case;
import cern.colt.list.ByteArrayList;

public class EliasAndHartman extends BaseAlgorithm {

	public EliasAndHartman(String casesFolder) {
		super(casesFolder);
	}

	@SuppressWarnings({ "unchecked" })
	public int sort(Cycle pi) {
		pi = Util.simplify(pi);

		int n = pi.size();

		byte[] array = new byte[pi.size()];
		for (int i = 0; i < pi.size(); i++)
			array[i] = (byte) i;

		Cycle sigma = new Cycle(array);

		MulticyclePermutation sigmaPiInverse = computeProduct(true, n, sigma, pi.getInverse());

		int distance = 0;

		Pair<Cycle, Cycle> _2_2Seq = searchFor2_2Seq(sigmaPiInverse, pi);
		if (_2_2Seq != null) {
			pi = computeProduct(_2_2Seq.getSecond(), _2_2Seq.getFirst(), pi).asNCycle();
			distance += 2;
			sigmaPiInverse = computeProduct(true, sigma, pi.getInverse());
		}

		while (thereAreOddCycles(sigmaPiInverse)) {
			apply2MoveTwoOddCycles(sigmaPiInverse, pi);
			distance += 1;
			sigmaPiInverse = computeProduct(true, sigma, pi.getInverse());
		}

		List<Collection<Cycle>> badSmallComponents = new ArrayList<>();

		List<Cycle> sigmaPiInverseWithoOutBadSmallComponents;
		while (!sigmaPiInverse.isIdentity() && !(sigmaPiInverseWithoOutBadSmallComponents = ListUtils.subtract(
				sigmaPiInverse.stream().filter(c -> c.size() > 1).collect(Collectors.toList()),
				badSmallComponents.stream().flatMap(c -> c.stream()).collect(Collectors.toList()))).isEmpty()) {
			Cycle _2move = searchFor2MoveOriented(sigmaPiInverse, pi);
			if (_2move != null) {
				pi = computeProduct(_2move, pi).asNCycle();
				distance += 1;
			} else {
				Set<Cycle> mu = new HashSet<>();
				Cycle initialFactor = sigmaPiInverseWithoOutBadSmallComponents.stream().filter(c -> c.size() > 1)
						.findFirst().get();
				mu.add(new Cycle(initialFactor.get(0), initialFactor.get(1), initialFactor.get(2)));

				boolean badSmallComponent = true;
				// O(n)
				for (int i = 0; i < 8; i++) {
					int norm = getNorm(mu);

					mu = extend(mu, sigmaPiInverse, pi);

					if (norm == getNorm(mu)) {
						badSmallComponent = true;
						break;
					}

					byte[][] _11_8Seq = searchFor11_8Seq(mu, sigmaPiInverse, pi);
					if (_11_8Seq != null) {
						for (byte[] rho : _11_8Seq)
							pi = computeProduct(new Cycle(rho), pi).asNCycle();
						distance += _11_8Seq.length;
						badSmallComponent = false;
						break;
					}
				}

				if (badSmallComponent)
					badSmallComponents.add(mu);
			}

			sigmaPiInverse = computeProduct(true, sigma, pi.getInverse());
		}

		// Bad small components
		Collection<Cycle> mu = new ArrayList<>();
		Iterator<Collection<Cycle>> iterator = badSmallComponents.iterator();
		while (iterator.hasNext()) {
			mu.addAll(iterator.next());
			iterator.remove();
			if (getNorm(mu) >= 16) {
				byte[][] _11_8Seq = searchFor11_8Seq(mu, sigmaPiInverse, pi);
				for (byte[] rho : _11_8Seq) {
					pi = computeProduct(new Cycle(rho), pi).asNCycle();
				}
				distance += _11_8Seq.length;
				sigmaPiInverse = computeProduct(true, sigma, pi.getInverse());
			}
		}

		while (!sigmaPiInverse.isIdentity()) {
			Cycle _2move = searchFor2MoveOriented(sigmaPiInverse, pi);
			if (_2move != null) {
				pi = computeProduct(_2move, pi).asNCycle();
				distance += 1;
			} else {
				apply3_2(sigmaPiInverse, pi);
				distance += 3;
			}
			sigmaPiInverse = computeProduct(true, sigma, pi.getInverse());
		}
		
		return distance;
	}

	private byte[][] searchFor11_8Seq(Collection<Cycle> mu, MulticyclePermutation sigmaPiInverse, Cycle pi) {
		return searchForSeq(mu, sigmaPiInverse, pi, _11_8UnorientedCases.get(mu.size()));
	}

	private Set<Cycle> extend(Set<Cycle> mu, MulticyclePermutation sigmaPiInverse, Cycle pi) {
		Cycle piInverse = pi.getInverse().getStartingBy(pi.getMinSymbol());
		Set<Byte> muSymbols = new HashSet<>();

		Cycle[] symbolToMuCycles = new Cycle[piInverse.size()];
		// O(1), since at this point, ||mu|| never exceeds 16
		for (Cycle muCycle : mu)
			for (int i = 0; i < muCycle.getSymbols().length; i++) {
				symbolToMuCycles[muCycle.getSymbols()[i]] = muCycle;
				muSymbols.add(muCycle.getSymbols()[i]);
			}

		Cycle[] symbolToSigmaPiInverseCycles = new Cycle[piInverse.size()];
		for (Cycle cycle : sigmaPiInverse)
			for (int i = 0; i < cycle.getSymbols().length; i++)
				symbolToSigmaPiInverseCycles[cycle.getSymbols()[i]] = cycle;

		// Type 1 extension
		// These two outer loops are O(1), since at this point, ||mu|| never
		// exceeds 16
		for (Cycle muCycle : mu) {
			for (int i = 0; i < muCycle.getSymbols().length; i++) {
				int left = piInverse.indexOf(muCycle.get(i));
				int right = piInverse.indexOf(muCycle.image(muCycle.get(i)));
				// O(n)
				if (Util.isOpenGate(left, right, symbolToMuCycles, mu, piInverse)) {
					Cycle intersectingCycle = getIntersectingCycle(left, right, symbolToSigmaPiInverseCycles,
							sigmaPiInverse, piInverse);
					if (intersectingCycle != null
							&& !contains(muSymbols, symbolToSigmaPiInverseCycles[intersectingCycle.get(0)])) {
						byte a = intersectingCycle.get(0), b = intersectingCycle.image(a),
								c = intersectingCycle.image(b);
						Set<Cycle> newMu = new HashSet<>(mu);
						newMu.add(new Cycle(a, b, c));
						return newMu;
					}
				}
			}
		}

		// Type 2 extension
		// O(n)
		for (Cycle muCycle : mu) {
			for (int i = 0; i < muCycle.getSymbols().length; i++) {
				int left = piInverse.indexOf(muCycle.get(i));
				int right = piInverse.indexOf(muCycle.image(muCycle.get(i)));
				int gates = left < right ? right - left : piInverse.size() - (left - right);
				for (int j = 1; j < gates; j++) {
					int index = (j + left) % piInverse.size();
					if (symbolToMuCycles[piInverse.get(index)] == null) {
						Cycle intersectingCycle = symbolToSigmaPiInverseCycles[piInverse.get(index)];
						if (intersectingCycle != null && intersectingCycle.size() > 1
								&& !contains(muSymbols, symbolToSigmaPiInverseCycles[intersectingCycle.get(0)])) {
							byte a = piInverse.get(index);
							byte b = intersectingCycle.image(a);
							if (isOutOfInterval(piInverse.indexOf(b), left, right, piInverse.size())) {
								byte c = intersectingCycle.image(b);
								Set<Cycle> newMu = new HashSet<>(mu);
								newMu.add(new Cycle(a, b, c));
								return newMu;
							}
						}
					}
				}
			}
		}

		return mu;
	}

	// O(n)
	private byte[][] searchForSeq(Collection<Cycle> mu, MulticyclePermutation sigmaPiInverse, Cycle pi,
			List<Case> cases) {
		if (cases != null) {
			Cycle[] symbolToMuCycle = new Cycle[pi.size()];

			for (Cycle muCycle : mu)
				for (int symbol : muCycle.getSymbols())
					symbolToMuCycle[symbol] = muCycle;

			int symbolsCount = mu.stream().mapToInt(c -> c.size()).sum();

			ByteArrayList _piArrayList = new ByteArrayList(symbolsCount);
			// O(n)
			for (int i = 0; i < pi.getSymbols().length; i++) {
				Cycle muCycle = symbolToMuCycle[pi.getSymbols()[i]];
				if (muCycle != null)
					_piArrayList.add(pi.getSymbols()[i]);
			}

			// |_pi| is constant, since ||mu|| is constant
			byte[] piSymbols = _piArrayList.elements();

			for /* O(1) */ (Case _case : cases)
				if (symbolsCount == _case.getSignature().length) {
					rotation: for /* O(1) */ (int i = 0; i < piSymbols.length; i++) {
						byte[] _piSymbols = getStartingBy(piSymbols, i);

						Map<Cycle, Integer> labels = new HashMap<>();
						for /* O(1) */ (int j = 0; j < _piSymbols.length; j++) {
							Cycle muCycle = symbolToMuCycle[_piSymbols[j]];
							if (muCycle != null && !labels.containsKey(muCycle))
								labels.put(muCycle, labels.size() + 1);
						}

						for (Entry<Cycle, Integer> entry : labels.entrySet())
							if (entry.getKey().size() != _case.getSigmaPiInverse().get(entry.getValue() - 1).size())
								continue rotation;

						for (int j = 0; j < _piSymbols.length; j++) {
							if (_case.getSignature()[j] != labels.get(symbolToMuCycle[_piSymbols[j]]))
								continue rotation;
							else {
								if (j == _case.getSignature().length - 1) {
									byte[][] rhos = new byte[_case.getRhos().size()][];
									for (int k = 0; k < rhos.length; k++) {
										byte[] _rho = Arrays.copyOf(_case.getRhos().get(k), 3);
										Util.replace(_rho, _piSymbols);
										rhos[k] = _rho;
									}

									return rhos;
								}
							}
						}
					}
				}
		}

		return null;
	}

	private void apply3_2(MulticyclePermutation sigmaPiInverse, Cycle pi) {
		Cycle initialFactor = sigmaPiInverse.stream().filter(c -> c.size() > 1).findFirst().get();
		Set<Cycle> mu = new HashSet<>();
		mu.add(new Cycle(initialFactor.get(0), initialFactor.get(1), initialFactor.get(2)));
		for (int i = 0; i < 2; i++) {
			mu = extend(mu, sigmaPiInverse, pi);
			byte[][] rhos = searchForSeq(mu, sigmaPiInverse, pi, _3_2Cases);
			if (rhos != null) {
				applyMoves(pi, rhos);
				return;
			}
		}
	}

	private Cycle searchFor2MoveOriented(MulticyclePermutation sigmaPiInverse, Cycle pi) {
		for (Cycle cycle : sigmaPiInverse.stream().filter(c -> !pi.getInverse().isFactor(c))
				.collect(Collectors.toList())) {
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
}
