package br.unb.cic.tdp1375;

import static br.unb.cic.tdp1375.permutations.PermutationGroups.computeProduct;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.math3.util.Pair;
import org.paukov.combinatorics.Factory;
import org.paukov.combinatorics.Generator;
import org.paukov.combinatorics.ICombinatoricsVector;

import com.google.common.base.Throwables;

import br.unb.cic.tdp1375.cases.Case;
import br.unb.cic.tdp1375.cases.Cases_3_2;
import br.unb.cic.tdp1375.cases.OddCyclesCases;
import br.unb.cic.tdp1375.permutations.Cycle;
import br.unb.cic.tdp1375.permutations.MulticyclePermutation;
import br.unb.cic.tdp1375.permutations.PermutationGroups;
import br.unb.cic.tdp1375.util.Util;
import cern.colt.list.ByteArrayList;

public class EliasHartman1375 {

	private static List<Case> _1_1OddCyclesCases = new ArrayList<>();
	private static List<Case> _2_2OddCyclesCases = new ArrayList<>();
	private static List<Case> _3_2Cases = new ArrayList<>();
	private static Map<Integer, List<Case>> _11_8UnorientedCases = new HashMap<>();

	static {
		_1_1OddCyclesCases.addAll(OddCyclesCases.get1_1Cases());
		_2_2OddCyclesCases.addAll(OddCyclesCases.get2_2Cases());

		_3_2Cases = Cases_3_2.get3_2Cases();

		String inputFolder = "/home/luiz/Desktop/SBT1375_proof/cases/unoriented";
		List<Case> cases = new ArrayList<>();
		cases.addAll(get11_8Cases(inputFolder + "/(0,3,1)(2,5,4)"));
		cases.addAll(get11_8Cases(inputFolder + "/(0,4,2)(1,5,3)"));
		cases.addAll(get11_8Cases(inputFolder + "/bad-small-(0,7,5)(1,11,9)(2,6,4)(3,10,8)"));
		cases.addAll(get11_8Cases(inputFolder + "/bad-small-(0,4,2)(1,5,3)"));
		cases.addAll(get11_8Cases(inputFolder + "/bad-small-(0,10,2)(1,5,3)(4,8,6)(7,11,9)"));
		cases.addAll(get11_8Cases(inputFolder + "/bad-small-(0,4,2)(1,14,12)(3,7,5)(6,10,8)(9,13,11)"));
		cases.addAll(get11_8Cases(inputFolder + "/bad-small-(0,16,2)(1,5,3)(4,8,6)(7,11,9)(10,14,12)(13,17,15)"));
		_11_8UnorientedCases.putAll(cases.stream().collect(Collectors.groupingBy(Case::getCyclesCount)));
	}

	public static void main(String[] args) {
		System.out.println(sort(new Cycle("0,8,3,7,6,2,5,1,4,9,14,13,12,11,10")));
	}

	@SuppressWarnings({ "unchecked" })
	public static int sort(Cycle pi) {
		pi = Util.simplify(pi);
		
		int n = pi.size();

		byte[] array = new byte[pi.size()];
		for (int i = 0; i < pi.size(); i++)
			array[i] = (byte) i;

		Cycle sigma = new Cycle(array);

		MulticyclePermutation sigmaPiInverse = computeProduct(true, n, sigma, pi.getInverse());

		int distance = 0, lowerBound = (pi.getSymbols().length - sigmaPiInverse.getNumberOfEvenCycles()) / 2;

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

		if (distance < lowerBound)
			throw new RuntimeException("ERROR");

		return distance;
	}

	private static int getNorm(Collection<Cycle> mu) {
		return mu.stream().mapToInt(c -> c.getNorm()).sum();
	}

	private static void apply2MoveTwoOddCycles(MulticyclePermutation sigmaPiInverse, Cycle pi) {
		int evenCycles = sigmaPiInverse.getNumberOfEvenCycles();
		List<Cycle> oddCycles = sigmaPiInverse.stream().filter(c -> !c.isEven()).collect(Collectors.toList());
		for (Cycle c1 : oddCycles)
			for (Cycle c2 : oddCycles)
				if (c1 != c2) {
					for (Cycle a : get2CyclesSegments(c1))
						for (Cycle b : get2CyclesSegments(c2)) {
							for (ICombinatoricsVector<Byte> rho : combinations(
									Arrays.asList(a.get(0), a.get(1), b.get(0), b.get(1)), 3)) {
								Cycle rho1 = new Cycle(rho.getVector().stream().mapToInt(i -> i).toArray());
								if (pi.areSymbolsInCyclicOrder(rho1.getSymbols())
										&& (computeProduct(sigmaPiInverse, rho1.getInverse())).getNumberOfEvenCycles()
												- evenCycles == 2) {
									applyMovesRedefining(pi, rho1.getSymbols());
									return;
								}
							}
						}
				}
	}

	private static Pair<Cycle, Cycle> searchFor2_2Seq(MulticyclePermutation sigmaPiInverse, Cycle pi) {
		List<Cycle> oddCycles = sigmaPiInverse.stream().filter(c -> !c.isEven()).collect(Collectors.toList());
		for (Cycle c1 : oddCycles)
			for (Cycle c2 : oddCycles)
				if (c1 != c2) {
					for (Cycle a : get2CyclesSegments(c1))
						for (Byte b : c2.getSymbols()) {
							for (ICombinatoricsVector<Byte> rho : combinations(Arrays.asList(a.get(0), a.get(1), b),
									3)) {
								Cycle rho1 = new Cycle(rho.getVector().stream().mapToInt(i -> i).toArray());
								if (pi.areSymbolsInCyclicOrder(rho1.getSymbols())) {
									MulticyclePermutation _sigmaPiInverse = PermutationGroups
											.computeProduct(sigmaPiInverse, rho1.getInverse());
									Cycle _pi = new Cycle(Util.applyTransposition(rho1.getSymbols(), pi.getSymbols()));
									Cycle rho2 = searchFor2Move(_sigmaPiInverse, _pi);
									if (rho2 != null)
										return new Pair<>(rho1, rho2);
								}
							}
						}
				}

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
							if (after - before == 2) {
								Cycle rho1 = new Cycle(a, b, c);
								MulticyclePermutation _sigmaPiInverse = PermutationGroups.computeProduct(sigmaPiInverse,
										rho1.getInverse());
								Cycle _pi = new Cycle(Util.applyTransposition(rho1.getSymbols(), pi.getSymbols()));
								Cycle rho2 = searchFor2Move(_sigmaPiInverse, _pi);
								if (rho2 != null)
									return new Pair<>(rho1, rho2);
							}
						}
					}
				}
			}
		}

		return null;
	}

	public static <T> Generator<T> combinations(Collection<T> collection, int k) {
		return Factory.createSimpleCombinationGenerator(Factory.createVector(collection), k);
	}

	private static List<Cycle> get2CyclesSegments(Cycle cycle) {
		List<Cycle> result = new ArrayList<>();
		for (int i = 0; i < cycle.size(); i++) {
			result.add(new Cycle(cycle.get(i), cycle.image(cycle.get(i))));
		}
		return result;
	}

	private static byte[][] searchFor11_8Seq(Collection<Cycle> mu, MulticyclePermutation sigmaPiInverse, Cycle pi) {
		return searchForSeq(mu, sigmaPiInverse, pi, _11_8UnorientedCases.get(mu.size()));
	}

	private static Set<Cycle> extend(Set<Cycle> mu, MulticyclePermutation sigmaPiInverse, Cycle pi) {
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

	private static boolean contains(Set<Byte> muSymbols, Cycle cycle) {
		for (Byte symbol : cycle.getSymbols())
			if (muSymbols.contains(symbol))
				return true;
		return false;
	}

	// O(n)
	private static Cycle getIntersectingCycle(int left, int right, Cycle[] symbolToSigmaPiInverseCycles,
			MulticyclePermutation sigmaPiInverse, Cycle piInverse) {
		int gates = left < right ? right - left : piInverse.size() - (left - right);
		for (int i = 1; i < gates; i++) {
			int index = (i + left) % piInverse.size();
			Cycle intersectingCycle = symbolToSigmaPiInverseCycles[piInverse.get(index)];
			if (intersectingCycle != null && intersectingCycle.size() > 1) {
				byte a = piInverse.get(index);
				byte b = intersectingCycle.image(a);
				if (isOutOfInterval(piInverse.indexOf(b), left, right, piInverse.size())) {
					return intersectingCycle.getStartingBy(a);
				}
			}
		}
		return null;
	}

	private static boolean isOutOfInterval(int x, int left, int right, int n) {
		if (left < right)
			return x < left || x > right;
		return x > left && x < right;
	}

	// O(n)
	private static byte[][] searchForSeq(Collection<Cycle> mu, MulticyclePermutation sigmaPiInverse, Cycle pi,
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

	private static byte[] getStartingBy(byte[] cycle, int i) {
		byte[] _symbols = new byte[cycle.length];
		System.arraycopy(cycle, i, _symbols, 0, _symbols.length - i);
		System.arraycopy(cycle, 0, _symbols, _symbols.length - i, i);
		return _symbols;
	}

	private static List<Case> get11_8Cases(String file) {
		List<Case> _cases = new ArrayList<>();

		try (Reader fr = new BufferedReader(new FileReader(file), 10000000)) {
			try (Scanner scanner = new Scanner(fr)) {
				scanner.useDelimiter("\\n");

				while (scanner.hasNext()) {
					String line = scanner.next().trim();
					MulticyclePermutation sigmaPiInverse = new MulticyclePermutation(line.split(";")[0]);
					byte[] pi = new byte[sigmaPiInverse.stream().mapToInt(c -> c.getSymbols().length).sum()];
					for (int i = 0; i < pi.length; i++) {
						pi[i] = (byte) i;
					}
					List<byte[]> rhos = new ArrayList<>();
					for (String _rho : line.split(";")[1].split("-")) {
						_rho = _rho.replace("[", "").replace("]", "");
						byte[] rho = new byte[3];
						rho[0] = Byte.parseByte(_rho.split(",")[0].trim());
						rho[1] = Byte.parseByte(_rho.split(",")[1].trim());
						rho[2] = Byte.parseByte(_rho.split(",")[2].trim());
						rhos.add(rho);
					}
					_cases.add(new Case(pi, sigmaPiInverse, rhos));
				}
			}
		} catch (IOException e) {
			Throwables.propagate(e);
		}

		return _cases;
	}

	private static void apply3_2(MulticyclePermutation sigmaPiInverse, Cycle pi) {
		Cycle initialFactor = sigmaPiInverse.stream().filter(c -> c.size() > 1).findFirst().get();
		Set<Cycle> mu = new HashSet<>();
		mu.add(new Cycle(initialFactor.get(0), initialFactor.get(1), initialFactor.get(2)));
		for (int i = 0; i < 2; i++) {
			mu = extend(mu, sigmaPiInverse, pi);
			byte[][] rhos = searchForSeq(mu, sigmaPiInverse, pi, _3_2Cases);
			if (rhos != null) {
				applyMovesRedefining(pi, rhos);
				return;
			}
		}
	}

	private static void applyMovesRedefining(Cycle omega, byte[]... rhos) {
		byte[] pi = omega.getSymbols();
		for (byte[] rho : rhos)
			pi = Util.applyTransposition(rho, pi);
		omega.redefine(pi);
	}

	private static boolean thereAreOddCycles(MulticyclePermutation sigmaPiInverse) {
		return sigmaPiInverse.stream().filter(c -> !c.isEven()).count() > 0;
	}

	private static Cycle searchFor2Move(MulticyclePermutation sigmaPiInverse, Cycle pi) {
		List<Cycle> oddCycles = sigmaPiInverse.stream().filter(c -> !c.isEven()).collect(Collectors.toList());
		for (Cycle c1 : oddCycles)
			for (Cycle c2 : oddCycles)
				if (c1 != c2) {
					for (Cycle a : get2CyclesSegments(c1))
						for (Byte b : c2.getSymbols()) {
							for (ICombinatoricsVector<Byte> rho : combinations(Arrays.asList(a.get(0), a.get(1), b),
									3)) {
								Cycle rho1 = new Cycle(rho.getVector().stream().mapToInt(i -> i).toArray());
								if (pi.areSymbolsInCyclicOrder(rho1.getSymbols())) {
									return rho1;
								}
							}
						}
				}

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

	private static Cycle searchFor2MoveOriented(MulticyclePermutation sigmaPiInverse, Cycle pi) {
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
