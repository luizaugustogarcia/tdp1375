package br.unb.cic.tdp;

import static br.unb.cic.tdp.permutation.PermutationGroups.computeProduct;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.math3.util.Pair;
import org.paukov.combinatorics.ICombinatoricsVector;

import com.google.common.base.Throwables;

import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.permutation.PermutationGroups;
import br.unb.cic.tdp.proof.Case;
import br.unb.cic.tdp.proof.OddCyclesCases;

abstract class BaseAlgorithm {

	protected static final String UNRTD_3_2 = "unoriented/3_2";
	protected static final String UNRTD_INTERSECTING_PAIR = "unoriented/(0,3,1)(2,5,4)";
	protected static final String UNRTD_INTERLEAVING_PAIR = "unoriented/(0,4,2)(1,5,3)";
	protected static final String UNRTD_BAD_SMALL_INTERLEAVING_PAIR = "unoriented/bad-small-(0,4,2)(1,5,3)";
	protected static final String UNRTD_BAD_SMALL_NECKLACE_SIZE_4 = "unoriented/bad-small-(0,10,2)(1,5,3)(4,8,6)(7,11,9)";
	protected static final String UNRTD_BAD_SMALL_TWISTED_NECKLACE_SIZE_4 = "unoriented/bad-small-(0,7,5)(1,11,9)(2,6,4)(3,10,8)";
	protected static final String UNRTD_BAD_SMALL_NECKLACE_SIZE_5 = "unoriented/bad-small-(0,4,2)(1,14,12)(3,7,5)(6,10,8)(9,13,11)";
	protected static final String UNRTD_BAD_SMALL_NECKLACE_SIZE_6 = "unoriented/bad-small-(0,16,2)(1,5,3)(4,8,6)(7,11,9)(10,14,12)(13,17,15)";

	protected Map<Integer, List<Case>> _11_8UnorientedCases = new HashMap<>();
	protected List<Case> _1_1OddCyclesCases = new ArrayList<>();
	protected List<Case> _2_2OddCyclesCases = new ArrayList<>();
	protected List<Case> _3_2Cases = new ArrayList<>();

	public BaseAlgorithm(String casesFolder) {
		loadCases(casesFolder);
	}

	abstract int sort(Cycle pi);

	protected void loadCases(String casesFolder) {
		// Generates the 2-moves to be applied when we have one odd cycle in sigma
		// pi^{-1}
		_1_1OddCyclesCases.addAll(OddCyclesCases.get1_1Cases());

		// Generates the (2,2)-sequences to be applied when we have four odd cycles in
		// sigma pi^{-1}
		_2_2OddCyclesCases.addAll(OddCyclesCases.get2_2Cases());

		// Loads the (3,2)-sequences to be applied to the interleaving pair and to
		// the cases where three 3-cycles are intersecting
		_3_2Cases = loadCasesFromFile(String.format("%s/%s", casesFolder, UNRTD_3_2));

		List<Case> cases = new ArrayList<>();

		cases.addAll(loadCasesFromFile(String.format("%s/%s", casesFolder, UNRTD_INTERSECTING_PAIR)));
		cases.addAll(loadCasesFromFile(String.format("%s/%s", casesFolder, UNRTD_INTERLEAVING_PAIR)));
		cases.addAll(loadCasesFromFile(String.format("%s/%s", casesFolder, UNRTD_BAD_SMALL_INTERLEAVING_PAIR)));
		cases.addAll(loadCasesFromFile(String.format("%s/%s", casesFolder, UNRTD_BAD_SMALL_NECKLACE_SIZE_4)));
		cases.addAll(loadCasesFromFile(String.format("%s/%s", casesFolder, UNRTD_BAD_SMALL_TWISTED_NECKLACE_SIZE_4)));
		cases.addAll(loadCasesFromFile(String.format("%s/%s", casesFolder, UNRTD_BAD_SMALL_NECKLACE_SIZE_5)));
		cases.addAll(loadCasesFromFile(String.format("%s/%s", casesFolder, UNRTD_BAD_SMALL_NECKLACE_SIZE_6)));

		_11_8UnorientedCases.putAll(cases.stream().collect(Collectors.groupingBy(Case::getCyclesCount)));
	}

	protected int getNorm(Collection<Cycle> mu) {
		return mu.stream().mapToInt(c -> c.getNorm()).sum();
	}

	protected byte[] getStartingBy(byte[] cycle, int i) {
		byte[] _symbols = new byte[cycle.length];
		System.arraycopy(cycle, i, _symbols, 0, _symbols.length - i);
		System.arraycopy(cycle, 0, _symbols, _symbols.length - i, i);
		return _symbols;
	}

	protected boolean isOutOfInterval(int x, int left, int right, int n) {
		if (left < right)
			return x < left || x > right;
		return x > left && x < right;
	}

	// O(n)
	protected Cycle getIntersectingCycle(int left, int right, Cycle[] symbolToSigmaPiInverseCycles,
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

	protected void apply2MoveTwoOddCycles(MulticyclePermutation sigmaPiInverse, Cycle pi) {
		int evenCycles = sigmaPiInverse.getNumberOfEvenCycles();
		List<Cycle> oddCycles = sigmaPiInverse.stream().filter(c -> !c.isEven()).collect(Collectors.toList());
		for (Cycle c1 : oddCycles)
			for (Cycle c2 : oddCycles)
				if (c1 != c2) {
					for (Cycle a : get2CyclesSegments(c1))
						for (Cycle b : get2CyclesSegments(c2)) {
							for (ICombinatoricsVector<Byte> rho : Util
									.combinations(Arrays.asList(a.get(0), a.get(1), b.get(0), b.get(1)), 3)) {
								Cycle rho1 = new Cycle(rho.getVector().stream().mapToInt(i -> i).toArray());
								if (pi.areSymbolsInCyclicOrder(rho1.getSymbols())
										&& (computeProduct(sigmaPiInverse, rho1.getInverse())).getNumberOfEvenCycles()
												- evenCycles == 2) {
									applyMoves(pi, rho1.getSymbols());
									return;
								}
							}
						}
				}
	}

	protected boolean thereAreOddCycles(MulticyclePermutation sigmaPiInverse) {
		return sigmaPiInverse.stream().filter(c -> !c.isEven()).count() > 0;
	}

	protected boolean contains(Set<Byte> muSymbols, Cycle cycle) {
		for (Byte symbol : cycle.getSymbols())
			if (muSymbols.contains(symbol))
				return true;
		return false;
	}

	protected Cycle searchFor2Move(MulticyclePermutation sigmaPiInverse, Cycle pi) {
		List<Cycle> oddCycles = sigmaPiInverse.stream().filter(c -> !c.isEven()).collect(Collectors.toList());
		for (Cycle c1 : oddCycles)
			for (Cycle c2 : oddCycles)
				if (c1 != c2) {
					for (Cycle a : get2CyclesSegments(c1))
						for (Byte b : c2.getSymbols()) {
							for (ICombinatoricsVector<Byte> rho : Util
									.combinations(Arrays.asList(a.get(0), a.get(1), b), 3)) {
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

	protected Pair<Cycle, Cycle> searchFor2_2Seq(MulticyclePermutation sigmaPiInverse, Cycle pi) {
		List<Cycle> oddCycles = sigmaPiInverse.stream().filter(c -> !c.isEven()).collect(Collectors.toList());
		for (Cycle c1 : oddCycles)
			for (Cycle c2 : oddCycles)
				if (c1 != c2) {
					for (Cycle a : get2CyclesSegments(c1))
						for (Byte b : c2.getSymbols()) {
							for (ICombinatoricsVector<Byte> rho : Util
									.combinations(Arrays.asList(a.get(0), a.get(1), b), 3)) {
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

	protected void applyMoves(Cycle omega, byte[]... rhos) {
		byte[] pi = omega.getSymbols();
		for (byte[] rho : rhos)
			pi = Util.applyTransposition(rho, pi);
		omega.redefine(pi);
	}

	static Pattern LINE_PATTERN = Pattern.compile("(\\(.*\\))");

	protected List<Case> loadCasesFromFile(String file) {
		List<Case> _cases = new ArrayList<>();

		try (BufferedReader fr = new BufferedReader(new FileReader(file), 10000000)) {
			String line;

			while ((line = fr.readLine()) != null) {
				String[] parts = line.trim().split(";");
				MulticyclePermutation sigmaPiInverse = new MulticyclePermutation(parts[0]);
				byte[] pi = new byte[sigmaPiInverse.stream().mapToInt(c -> c.getSymbols().length).sum()];
				for (int i = 0; i < pi.length; i++) {
					pi[i] = (byte) i;
				}
				List<byte[]> rhos = new ArrayList<>();
				for (String _rho : parts[1].split("-")) {
					_rho = _rho.replaceAll("\\[|\\]|\\s", "");
					byte[] rho = new byte[3];
					String[] symbols = _rho.split(",");
					rho[0] = Byte.parseByte(symbols[0]);
					rho[1] = Byte.parseByte(symbols[1]);
					rho[2] = Byte.parseByte(symbols[2]);
					rhos.add(rho);
				}
				_cases.add(new Case(pi, sigmaPiInverse, rhos));
			}
		} catch (IOException e) {
			Throwables.propagate(e);
		}

		return _cases;
	}

	protected List<Cycle> get2CyclesSegments(Cycle cycle) {
		List<Cycle> result = new ArrayList<>();
		for (int i = 0; i < cycle.size(); i++) {
			result.add(new Cycle(cycle.get(i), cycle.image(cycle.get(i))));
		}
		return result;
	}

}
