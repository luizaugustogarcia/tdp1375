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
import br.unb.cic.tdp.proof.OrientedCycleGreaterThan5;
import cern.colt.list.ByteArrayList;

public class Silvaetal extends BaseAlgorithm {

	private static final String ORTD_INTERLEAVING_PAIR = "oriented/(0,4,2)(1,5,3)";
	private static final String ORTD_BAD_SMAL_INTERLEAVING_PAIR = "oriented/(0,4,2)(1,5,3)";

	private List<Case> _11_8OrientedCycle = new ArrayList<>();

	private Map<Integer, List<Case>> _11_8OrientedCases = new HashMap<>();

	public static void main(String[] args) {
		Silvaetal silvaetal = new Silvaetal(args[0]);
		System.out.println(silvaetal.sort(new Cycle(args[1])));
	}

	public Silvaetal(String casesFolder) {
		super(casesFolder);
		loadExtraCases(casesFolder);
	}

	private void loadExtraCases(String casesFolder) {
		ArrayList<Case> cases = new ArrayList<>();
		cases.addAll(load11_8Cases(String.format("%s/%s", casesFolder, ORTD_INTERLEAVING_PAIR)));
		cases.addAll(load11_8Cases(String.format("%s/%s", casesFolder, ORTD_BAD_SMAL_INTERLEAVING_PAIR)));
		_11_8OrientedCases.putAll(cases.stream().collect(Collectors.groupingBy(Case::getCyclesCount)));

		// Generates the (4,3)-sequences cases to be applied when we have an oriented
		// cycle with length greater than 6
		_11_8OrientedCycle.addAll(OrientedCycleGreaterThan5.get11_8Cases());
	}

	@SuppressWarnings({ "unchecked" })
	public int sort(Cycle pi) {
		int n = pi.size();

		byte[] array = new byte[pi.size()];
		for (int i = 0; i < pi.size(); i++)
			array[i] = (byte) i;

		Cycle sigma = new Cycle(array);

		MulticyclePermutation sigmaPiInverse = computeProduct(true, n, sigma, pi.getInverse());

		int distance = 0, lowerBound = (pi.getSymbols().length - sigmaPiInverse.getNumberOfEvenCycles()) / 2;

		// O(n^6)
		Pair<Cycle, Cycle> _2_2Seq = searchFor2_2Seq(sigmaPiInverse, pi);
		if (_2_2Seq != null) {
			pi = computeProduct(_2_2Seq.getSecond(), _2_2Seq.getFirst(), pi).asNCycle();
			distance += 2;
			sigmaPiInverse = computeProduct(true, sigma, pi.getInverse());
		}

		// O(n^2)
		while (thereAreOddCycles(sigmaPiInverse)) {
			apply2MoveTwoOddCycles(sigmaPiInverse, pi);
			distance += 1;
			sigmaPiInverse = computeProduct(true, sigma, pi.getInverse());
		}

		List<Collection<Cycle>> badSmallComponents = new ArrayList<>();

		// O(n^4)
		List<Cycle> sigmaPiInverseWithoOutBadSmallComponents;
		while (!sigmaPiInverse.isIdentity() && !(sigmaPiInverseWithoOutBadSmallComponents = ListUtils.subtract(
				sigmaPiInverse.stream().filter(c -> c.size() > 1).collect(Collectors.toList()),
				badSmallComponents.stream().flatMap(c -> c.stream()).collect(Collectors.toList()))).isEmpty()) {
			// O(n^3)
			Cycle _2move = searchFor2MoveOriented(sigmaPiInverse, pi);
			if (_2move != null) {
				pi = computeProduct(_2move, pi).asNCycle();
				distance += 1;
			} else {
				Cycle orientedCycle = searchForOrientedCycleBiggerThan5(sigmaPiInverse, pi);
				if (orientedCycle != null) {
					// O(n^3)
					distance += apply11_8SeqOrientedCase(orientedCycle, sigmaPiInverse, pi);
				} else {
					Set<Cycle> mu = new HashSet<>();
					Cycle initialFactor = sigmaPiInverseWithoOutBadSmallComponents.stream().filter(c -> c.size() > 1)
							.findFirst().get();
					mu.add(new Cycle(initialFactor.get(0), initialFactor.get(1), initialFactor.get(2)));

					boolean badSmallComponent = false;
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
							break;
						}
					}

					if (badSmallComponent)
						badSmallComponents.add(mu);
				}
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

		// O(n)
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
		byte[][] rhos = null;

		if (mu.stream().filter(c -> c.size() == 5 && !pi.getInverse().isFactor(c)).count() > 0)
			rhos = searchForSeq(mu, sigmaPiInverse, pi, _11_8OrientedCases.get(mu.size()));

		if (rhos == null)
			rhos = searchForSeq(mu, sigmaPiInverse, pi, _11_8UnorientedCases.get(mu.size()));

		return rhos;
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

		// Type 3 extension
		// The outer loop is O(1) since, at this point, ||mu|| never exceeds 16
		for (Cycle muCycle : mu) {
			if (muCycle.size() < symbolToSigmaPiInverseCycles[muCycle.get(0)].size()) {
				Cycle sigmaPiInverseCycle = align(symbolToSigmaPiInverseCycles[muCycle.get(0)], muCycle);
				muCycle = muCycle.getStartingBy(sigmaPiInverseCycle.get(0));
				byte[] newSymbols = Arrays.copyOf(muCycle.getSymbols(), muCycle.getSymbols().length + 2);
				newSymbols[muCycle.getSymbols().length] = sigmaPiInverseCycle
						.image(muCycle.get(muCycle.getSymbols().length - 1));
				newSymbols[muCycle.getSymbols().length + 1] = sigmaPiInverseCycle
						.image(newSymbols[muCycle.getSymbols().length]);

				Set<Cycle> newMu = new HashSet<>(mu);
				newMu.remove(muCycle);
				newMu.add(new Cycle(newSymbols));

				Map<Cycle, Integer> openGates = Util.openGatesPerCycle(newMu, piInverse);
				if (openGates.values().stream().mapToInt(j -> j.intValue()).sum() <= 2)
					return newMu;
			}
		}

		return mu;
	}

	// O(n)
	private Cycle align(Cycle sigmaPiInverseCycle, Cycle segment) {
		for (int i = 0; i < segment.size(); i++) {
			byte symbol = segment.get(i);
			int index = sigmaPiInverseCycle.indexOf(symbol);
			boolean match = true;
			for (int j = 1; j < segment.size(); j++) {
				if (segment.get((i + j) % segment.size()) != sigmaPiInverseCycle
						.get((index + j) % sigmaPiInverseCycle.size())) {
					match = false;
					break;
				}
				symbol = segment.image(symbol);
			}
			if (match)
				return sigmaPiInverseCycle.getStartingBy(segment.get(i));
		}
		return null;
	}

	private Cycle searchForOrientedCycleBiggerThan5(MulticyclePermutation sigmaPiInverse, Cycle pi) {
		return sigmaPiInverse.stream().filter(c -> c.size() > 5 && !pi.getInverse().isFactor(c)).findFirst()
				.orElse(null);
	}

	// O(n^3)
	private int apply11_8SeqOrientedCase(Cycle orientedCycle, MulticyclePermutation sigmaPiInverse, Cycle pi) {
		for (int j = 0; j < orientedCycle.size(); j++) {
			byte a = orientedCycle.get(j);
			byte d = orientedCycle.image(a);
			byte e = orientedCycle.image(d);

			for (int i = 3; i <= orientedCycle.size() - 4; i++) {
				byte b = orientedCycle.get((i + j) % orientedCycle.size());
				byte f = orientedCycle.image(b);

				for (int l = 5; l <= orientedCycle.size() - 2; i++) {
					byte c = orientedCycle.get((j + l) % orientedCycle.size());
					byte g = orientedCycle.image(c);

					Cycle factor = new Cycle(a, d, e, b, f, c, g);

					for (Case _case : _11_8OrientedCycle) {
						Cycle _orientedCycleCase = _case.getSigmaPiInverse().get(0);

						byte[] substitution = new byte[7];
						for (int k = 0; k < substitution.length; k++)
							substitution[_orientedCycleCase.getSymbols()[k]] = factor.get(k);

						byte[][] rhos = new byte[_case.getRhos().size()][];
						for (int k = 0; k < rhos.length; k++) {
							byte[] _rho = Arrays.copyOf(_case.getRhos().get(k), 3);
							Util.replace(_rho, substitution);
							rhos[k] = _rho;
						}

						if (is11_8(sigmaPiInverse, pi.getSymbols(), rhos)) {
							applyMoves(pi, rhos);
							return rhos.length;
						}
					}
				}
			}
		}

		throw new RuntimeException("ERROR");
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
							if (entry.getKey().size() != _case.getSigmaPiInverse().get(entry.getValue() - 1).size()
									|| isOriented(pi, entry.getKey()) != isOriented(_case.getPi(),
											_case.getSigmaPiInverse().get(entry.getValue() - 1)))
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

	private boolean isOriented(Cycle pi, Cycle cycle) {
		return !pi.getInverse().isFactor(cycle);
	}

	private boolean is11_8(MulticyclePermutation sigmaPiInverse, byte[] pi, byte[][] rhos) {
		int evenCycles = sigmaPiInverse.getNumberOfEvenCycles();
		for (byte[] rho : rhos) {
			if (areSymbolsInCyclicOrder(rho, pi)) {
				pi = Util.applyTransposition(rho, pi);
				sigmaPiInverse = computeProduct(sigmaPiInverse, new Cycle(rho).getInverse());
			} else {
				return false;
			}
		}
		int after = sigmaPiInverse.getNumberOfEvenCycles();
		return after > evenCycles && (float) rhos.length / ((after - evenCycles) / 2) <= ((float) 11 / 8);
	}

	private boolean areSymbolsInCyclicOrder(byte[] rho, byte[] pi) {
		int a = 0, b = 0, c = 0;
		for (int i = 0; i < pi.length; i++) {
			if (pi[i] == rho[0])
				a = i;
			if (pi[i] == rho[1])
				b = i;
			if (pi[i] == rho[2])
				c = i;
		}
		return a < b && b < c || c < a && a < b || b < c && c < a;
	}

	private void apply3_2(MulticyclePermutation sigmaPiInverse, Cycle pi) {
		Cycle orientedCycle = sigmaPiInverse.stream().filter(c -> c.size() >= 5 && !pi.getInverse().isFactor(c))
				.findFirst().orElse(null);

		if (orientedCycle != null) {
			apply3_2OrientedCase(orientedCycle, sigmaPiInverse, pi);
			return;
		} else {
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
	}

	private void apply3_2OrientedCase(Cycle orientedCycle, MulticyclePermutation sigmaPiInverse, Cycle pi) {
		int even = sigmaPiInverse.getNumberOfEvenCycles();

		for (int i = 0; i <= orientedCycle.size() - 5; i++) {
			byte _0 = orientedCycle.get(0);
			byte _3 = orientedCycle.image(_0);
			byte _1 = orientedCycle.image(_3);

			byte _4 = orientedCycle.get(i + 3);
			byte _2 = orientedCycle.image(_4);

			byte[] rho1 = new byte[] { _0, _1, _2 };
			byte[] rho2 = new byte[] { _1, _2, _3 };
			byte[] rho3 = new byte[] { _2, _3, _4 };

			if (computeProduct(true, sigmaPiInverse, new Cycle(rho1).getInverse(), new Cycle(rho2).getInverse(),
					new Cycle(rho3).getInverse()).getNumberOfEvenCycles() - even >= 4) {
				applyMoves(pi, rho1, rho2, rho3);
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
