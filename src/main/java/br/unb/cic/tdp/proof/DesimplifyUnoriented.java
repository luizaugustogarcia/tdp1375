package br.unb.cic.tdp.proof;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import org.javatuples.Triplet;
import org.paukov.combinatorics.Factory;
import org.paukov.combinatorics.Generator;

import com.google.common.base.Throwables;
import com.google.common.primitives.Bytes;

import br.unb.cic.tdp.Util;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.permutation.PermutationGroups;
import cern.colt.list.ByteArrayList;

class DesimplifyUnoriented {

	protected static final Pattern spiPattern = Pattern.compile(".*\"(.*)\".*");
	private static final Pattern SORTING_PATTERN = Pattern.compile(".*a = (\\d+).*b = (\\d+).*c = (\\d+).*");

	static List<Case> generate(final String inputDir) {
		final var verifiedConfigurations = new HashSet<Configuration>();

		final var visitedFiles = new HashSet<String>();

		final var cases = new ArrayList<Case>();

		// unoriented interleaving pair
		generate(verifiedConfigurations, cases, inputDir + "bfs_files/", "[3](0_4_2)[3](1_5_3).html", visitedFiles);

		// unoriented intersecting pair
		generate(verifiedConfigurations, cases, inputDir + "bfs_files/", "[3](0_3_1)[3](2_5_4).html", visitedFiles);

		// BAD SMALL COMPONENTS

		// the unoriented interleaving pair
		generate(verifiedConfigurations, cases, inputDir + "comb_files/", "[3](0_4_2)[3](1_5_3).html", visitedFiles);
		// the unoriented necklaces of size 4
		generate(verifiedConfigurations, cases, inputDir + "comb_files/", "[3](0_10_2)[3](1_5_3)[3](4_8_6)[3](7_11_9).html",
				visitedFiles);
		// the twisted necklace of size 4
		generate(verifiedConfigurations, cases, inputDir + "comb_files/", "[3](0_7_5)[3](1_11_9)[3](2_6_4)[3](3_10_8).html",
				visitedFiles);
		// the unoriented necklaces of size 5
		generate(verifiedConfigurations, cases, inputDir + "comb_files/",
				"[3](0_4_2)[3](1_14_12)[3](3_7_5)[3](6_10_8)[3](9_13_11).html", visitedFiles);
		// the unoriented necklaces of size 6
		generate(verifiedConfigurations, cases, inputDir + "comb_files/",
				"[3](0_16_2)[3](1_5_3)[3](4_8_6)[3](7_11_9)[3](10_14_12)[3](13_17_15).html", visitedFiles);

		return cases;
	}

	private static void generate(final Set<Configuration> verifiedConfigurations, List<Case> cases, String baseFolder, String file,
			final Set<String> visitedFiles) {
		if (visitedFiles.contains(baseFolder + file))
			return;

		final var rhos = getSorting(baseFolder + file);

		if (!rhos.isEmpty()) {
			final var spi = getSigmaPiInverse(file);
			final var n = spi.stream().mapToInt(c -> c.size()).sum();
			final var pi = new byte[n];
			for (var i = 0; i < n; i++) {
				pi[i] = (byte) i;
			}

			if (is11_8(spi, pi, rhos)) {
				cases.add(new Case(pi, spi, rhos));
			} else {
				throw new RuntimeException("ERROR");
			}

			desimplify(verifiedConfigurations, cases, spi, pi, rhos);
		} else {
			try (final var fr = new BufferedReader(new FileReader(baseFolder + file), 100000)) {
				try (final var scanner = new Scanner(fr)) {
					scanner.useDelimiter("\\n");

					while (scanner.hasNext()) {
						final var line = scanner.next();

						if (line.startsWith("View")) {
							final var matcher = spiPattern.matcher(line);
							if (matcher.matches())
								generate(verifiedConfigurations, cases, baseFolder, matcher.group(1), visitedFiles);
						}
					}
				}
			} catch (Exception e) {
				Throwables.propagate(e);
			}
		}

		visitedFiles.add(baseFolder + file);
	}

	private static void desimplify(final Set<Configuration> verifiedConfigurations, List<Case> cases,
			final MulticyclePermutation spi, final byte[] pi, final List<byte[]> rhos) {
		for (final var combination : combinations(spi, 2)) {
			// only join cycles which are not intersecting
			if (areNotIntersecting(combination.getVector(), pi)) {
				final var joiningPairs = getJoiningPairs(combination.getVector(), pi);

				for (final var joinPair : joiningPairs) {
					final var join = join(joinPair, spi, pi, rhos);

					final var cr = Util.canonicalize(join.getValue0(), join.getValue1(), join.getValue2());
					final var _pi = cr.getValue1();
					final var _spi = cr.getValue0();
					final var _rhos = cr.getValue2();

					if (is11_8(_spi, _pi, _rhos)) {
						final var configuration = new Configuration(_pi, _spi);

						if (!verifiedConfigurations.contains(configuration)) {
							verifiedConfigurations.add(configuration);
							cases.add(new Case(_pi, _spi, _rhos));
							desimplify(verifiedConfigurations, cases, _spi, _pi, _rhos);
						}
					} else {
						throw new RuntimeException("ERROR");
					}
				}
			}
		}
	}

	protected static boolean is11_8(MulticyclePermutation spi, byte[] pi, List<byte[]> rhos) {
		final var before = spi.getNumberOfEvenCycles();
		for (final var rho : rhos) {
			if (areSymbolsInCyclicOrder(rho, pi)) {
				pi = Util.applyTransposition(rho, pi);
				spi = PermutationGroups.computeProduct(spi, new Cycle(rho).getInverse());
			} else {
				return false;
			}
		}
		final var after = spi.getNumberOfEvenCycles();
		return after > before && (float) rhos.size() / ((after - before) / 2) <= ((float) 11 / 8);
	}

	protected static boolean areSymbolsInCyclicOrder(final byte[] rho, final byte[] pi) {
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

	protected static Triplet<MulticyclePermutation, byte[], List<byte[]>> join(final byte[] joinPair,
			final MulticyclePermutation spi, final byte[] pi, final List<byte[]> rhos) {
		final var symbolToCycle = new HashMap<Byte, Cycle>();

		final var _spi = new MulticyclePermutation(spi);

		for (int i = 0; i < _spi.size(); i++) {
			for (int j = 0; j < _spi.get(i).size(); j++) {
				final var symbol = _spi.get(i).getSymbols()[j];
				symbolToCycle.put(symbol, _spi.get(i));
			}
		}

		var a = symbolToCycle.get(joinPair[0]);
		var b = symbolToCycle.get(joinPair[1]);

		a = a.getInverse().getStartingBy(a.getInverse().image(joinPair[0]));
		b = b.getInverse().getStartingBy(joinPair[1]);

		final var cSymbols = new byte[a.size() + b.size() - 1];
		System.arraycopy(a.getSymbols(), 0, cSymbols, 0, a.size());
		System.arraycopy(b.getSymbols(), 1, cSymbols, a.size(), b.size() - 1);

		final var c = new Cycle(cSymbols);
		_spi.add(c.getInverse());
		_spi.remove(symbolToCycle.get(joinPair[0]));
		_spi.remove(symbolToCycle.get(joinPair[1]));

		final var _rhos = new ArrayList<byte[]>();
		var _pi = pi;
		for (final var rho : rhos) {
			final var __pi = Util.applyTransposition(rho, _pi);
			final var _rho = PermutationGroups.computeProduct(false,
					new Cycle(Util.replace(removeSymbol(__pi, joinPair[0]), joinPair[1], joinPair[0])),
					new Cycle(Util.replace(removeSymbol(_pi, joinPair[0]), joinPair[1], joinPair[0])).getInverse());

			_pi = __pi;
			// sometimes _rho = (),
			if (_rho.size() != 0)
				_rhos.add(_rho.asNCycle().getSymbols());
		}

		_pi = removeSymbol(pi, joinPair[1]);

		return new Triplet<>(_spi, _pi, _rhos);
	}

	private static byte[] removeSymbol(final byte[] array, final byte symbol) {
		final var temp = new ByteArrayList(Arrays.copyOf(array, array.length));
		temp.remove(temp.indexOf(symbol));
		return Arrays.copyOfRange(temp.elements(), 0, temp.size());
	}

	protected static List<byte[]> getJoiningPairs(final List<Cycle> cycles, final byte[] pi) {
		final var symbols = new HashSet<>(Bytes.asList(pi));

		final var symbolToLabel = new HashMap<Byte, Byte>();

		for (int i = 0; i < cycles.size(); i++) {
			for (int j = 0; j < cycles.get(i).size(); j++) {
				final var symbol = cycles.get(i).getSymbols()[j];
				symbolToLabel.put(symbol, (byte) i);
				symbols.remove(symbol);
			}
		}

		final var _pi = new ByteArrayList(Arrays.copyOf(pi, pi.length));
		_pi.removeAll(new ByteArrayList(Bytes.toArray(symbols)));

		final var results = new ArrayList<byte[]>();
		for (int i = 0; i < _pi.size(); i++) {
			final var currentLabel = symbolToLabel.get(_pi.get(i));
			final var nextLabel = symbolToLabel.get(_pi.get((i + 1) % _pi.size()));
			if (currentLabel != nextLabel && (_pi.get(i) + 1) % pi.length == _pi.get((i + 1) % _pi.size()))
				results.add(new byte[] { _pi.get(i), _pi.get((i + 1) % _pi.size()) });
		}

		return results;
	}

	private static boolean areNotIntersecting(final List<Cycle> cycles, final byte[] pi) {
		final var symbols = new HashSet<>(Bytes.asList(pi));

		final var symbolToLabel = new HashMap<Byte, Byte>();

		for (int i = 0; i < cycles.size(); i++) {
			for (int j = 0; j < cycles.get(i).size(); j++) {
				final var symbol = cycles.get(i).getSymbols()[j];
				symbolToLabel.put(symbol, (byte) i);
				symbols.remove(symbol);
			}
		}

		final var _pi = new ByteArrayList(Arrays.copyOf(pi, pi.length));
		_pi.removeAll(new ByteArrayList(Bytes.toArray(symbols)));

		final var states = new HashMap<Integer, Integer>();
		for (int i = 0; i < _pi.size(); i++) {
			final int currentLabel = symbolToLabel.get(_pi.get(i));
			final int nextLabel = symbolToLabel.get(_pi.get((i + 1) % _pi.size()));
			var state = !states.containsKey(currentLabel) ? 0 : states.get(currentLabel);
			if (currentLabel != nextLabel)
				state++;
			if (state == 2)
				return false;
			states.put(currentLabel, state);
		}

		return true;
	}

	protected static List<byte[]> getSorting(final String file) {
		final var mu = getSigmaPiInverse(file);
		var pi = new byte[mu.getNumberOfSymbols()];
		for (int i = 0; i < pi.length; i++) {
			pi[i] = (byte) i;
		}

		var hasSorting = false;

		final var rhos = new ArrayList<byte[]>();

		try (final var fr = new BufferedReader(new FileReader(file), 1024)) {
			try (final var scanner = new Scanner(fr)) {
				scanner.useDelimiter("\\n");

				while (scanner.hasNext()) {
					String line = scanner.next();

					if (line.contains("SORTING"))
						hasSorting = true;

					if (hasSorting) {
						Matcher matcher = SORTING_PATTERN.matcher(line);

						if (matcher.matches()) {
							byte a = Byte.parseByte(matcher.group(1));
							byte b = Byte.parseByte(matcher.group(2));
							byte c = Byte.parseByte(matcher.group(3));

							byte[] rho = new byte[] { pi[a], pi[b], pi[c] };
							rhos.add(rho);

							pi = Util.applyTransposition(rho, pi);
						}
					}
				}
			}
		} catch (IOException e) {
			Throwables.propagate(e);
		}

		return rhos;
	}

	protected static MulticyclePermutation getSigmaPiInverse(final String file) {
		final var _file = new File(file);
		var str = StringUtils.removeEnd(_file.getName().replace("_", ","), ".html");
		str = str.replaceAll("\\[.*?\\]", "");
		str = str.replace(" ", ",");
		return new MulticyclePermutation(str);
	}

	static <T> Generator<T> combinations(final Collection<T> collection, final int k) {
		return Factory.createSimpleCombinationGenerator(Factory.createVector(collection), k);
	}
}
