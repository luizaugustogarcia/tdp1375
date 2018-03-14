package br.unb.cic.tdp.proof;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.ehcache.Cache;
import org.ehcache.PersistentCacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;
import org.paukov.combinatorics.Factory;
import org.paukov.combinatorics.Generator;
import org.paukov.combinatorics.ICombinatoricsVector;

import com.google.common.base.Throwables;
import com.google.common.primitives.Bytes;

import br.unb.cic.tdp.Util;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.permutation.PermutationGroups;
import br.unb.cic.tdp.permutation.MulticyclePermutation.ByteArrayRepresentation;
import cern.colt.list.ByteArrayList;

public class DesimplifyUnoriented {

	private static final String INPUT_DIR = "/home/luiz/Desktop/SBT1375_proof/";
	private static final String OUTPUT_DIR = "/home/luiz/Desktop/SBT1375_proof/cases/unoriented/";
	
	private static final Set<String> VISITED_FILES = new HashSet<>();
	private static final Pattern SIGMA_PI_INVERSE_PATTERN = Pattern.compile(".*\"(.*)\".*");
	private static final Pattern SORTING_PATTERN = Pattern.compile(".*a = (\\d+).*b = (\\d+).*c = (\\d+).*");
	private static Cache<ByteArrayRepresentation, Serializable> cache;
	private static Serializable FAKE_OBJECT = new Serializable() {};

	public static void main(String[] args) throws IOException {
		PersistentCacheManager persistentCacheManager = CacheManagerBuilder.newCacheManagerBuilder()
				.with(CacheManagerBuilder.persistence("/home/luiz/cache/mydata"))
				.withCache("cache",
						CacheConfigurationBuilder.newCacheConfigurationBuilder(ByteArrayRepresentation.class,
								Serializable.class, ResourcePoolsBuilder.newResourcePoolsBuilder().heap(1, MemoryUnit.GB)
										.offheap(2, MemoryUnit.GB).disk(1, MemoryUnit.TB)))
				.build(true);

		cache = persistentCacheManager.getCache("cache", ByteArrayRepresentation.class, Serializable.class);

		System.out.println("/* unoriented interleaving pair */");
		translate("bfs_files/", "[3](0_4_2)[3](1_5_3).html", OUTPUT_DIR + "(0,4,2)(1,5,3)");
		System.out.println("/* unoriented intersecting pair */");
		translate("bfs_files/", "[3](0_3_1)[3](2_5_4).html", OUTPUT_DIR + "(0,3,1)(2,5,4)");

		System.out.println("/* BAD SMALL COMPONENTS */");

		System.out.println("/* the unoriented interleaving pair */");
		translate("comb_files/", "[3](0_4_2)[3](1_5_3).html", OUTPUT_DIR + "bad-small-(0,4,2)(1,5,3)");
		System.out.println("/* the unoriented necklaces of size 4 */");
		translate("comb_files/", "[3](0_10_2)[3](1_5_3)[3](4_8_6)[3](7_11_9).html",
				OUTPUT_DIR + "bad-small-(0,10,2)(1,5,3)(4,8,6)(7,11,9)");
		System.out.println("/* the twisted necklace of size 4 */");
		translate("comb_files/", "[3](0_7_5)[3](1_11_9)[3](2_6_4)[3](3_10_8).html",
				OUTPUT_DIR + "bad-small-(0,7,5)(1,11,9)(2,6,4)(3,10,8)");
		System.out.println("/* the unoriented necklaces of size 5 */");
		translate("comb_files/", "[3](0_4_2)[3](1_14_12)[3](3_7_5)[3](6_10_8)[3](9_13_11).html",
				OUTPUT_DIR + "bad-small-(0,4,2)(1,14,12)(3,7,5)(6,10,8)(9,13,11)");
		System.out.println("/* the unoriented necklaces of size 6 */");
		translate("comb_files/", "[3](0_16_2)[3](1_5_3)[3](4_8_6)[3](7_11_9)[3](10_14_12)[3](13_17_15).html",
				OUTPUT_DIR + "bad-small-(0,16,2)(1,5,3)(4,8,6)(7,11,9)(10,14,12)(13,17,15)");
	}

	private static void translate(String baseFolder, String file, String output) {
		File translated = new File(output);
		try (FileWriter fileWriter = new FileWriter(translated)) {
			try (BufferedWriter bufferedWriter = new BufferedWriter(fileWriter, 10 * 1024 * 1024)) {
				_translate(baseFolder, file, bufferedWriter);
			}
		} catch (IOException e) {
			Throwables.propagate(e);
		}
	}

	private static void _translate(String baseFolder, String file, BufferedWriter bufferedWriter) throws IOException {
		if (VISITED_FILES.contains(baseFolder + file))
			return;

		List<byte[]> rhos = getSorting(baseFolder, file);

		if (!rhos.isEmpty()) {
			MulticyclePermutation sigmaPiInverse = getSigmaPiInverse(file);

			int n = sigmaPiInverse.stream().mapToInt(c -> c.size()).sum();

			byte[] pi = new byte[n];
			for (int i = 0; i < n; i++) {
				pi[i] = (byte) i;
			}

			if (is11_8(sigmaPiInverse, pi, rhos)) {
				bufferedWriter.write(sigmaPiInverse + ";"
						+ rhos.stream().map(r -> Arrays.toString(r).intern()).collect(Collectors.joining("-")) + "\n");
			} else
				throw new RuntimeException("ERROR");

			desimplify(bufferedWriter, sigmaPiInverse, pi, rhos, 1);
		} else
			try (Reader fr = new BufferedReader(new FileReader(INPUT_DIR + baseFolder + file), 100000)) {
				try (Scanner scanner = new Scanner(fr)) {
					scanner.useDelimiter("\\n");

					while (scanner.hasNext()) {
						String line = scanner.next();

						if (line.startsWith("View")) {
							Matcher matcher = SIGMA_PI_INVERSE_PATTERN.matcher(line);
							if (matcher.matches())
								_translate(baseFolder, matcher.group(1), bufferedWriter);
						}
					}
				}
			}

		VISITED_FILES.add(baseFolder + file);
	}

	private static void desimplify(BufferedWriter writer, MulticyclePermutation sigmaPiInverse, byte[] pi,
			List<byte[]> rhos, int depth) throws IOException {
		for (ICombinatoricsVector<Cycle> combination : combinations(sigmaPiInverse, 2)) {
			if (areNotIntersecting(combination.getVector(), pi)) {
				List<byte[]> joiningPairs = getJoiningPairs(combination.getVector(), pi);

				for (byte[] joiningPair : joiningPairs) {
					byte[] _pi = removeSymbol(pi, joiningPair[1]);

					MulticyclePermutation _sigmaPiInverse = new MulticyclePermutation(sigmaPiInverse);

					// clone
					List<byte[]> _rhos = new ArrayList<>();
					for (byte[] rho : rhos) {
						_rhos.add(Arrays.copyOf(rho, rho.length));
					}

					joinCyclesAndReplaceRhos(_sigmaPiInverse, joiningPair, pi, _rhos);

					_sigmaPiInverse = Util.canonicalize(_sigmaPiInverse, _pi, _rhos);

					ByteArrayRepresentation byteArrayRepresentation = _sigmaPiInverse.byteArrayRepresentation();
					if (!cache.containsKey(byteArrayRepresentation)) {
						cache.put(byteArrayRepresentation, FAKE_OBJECT);

						if (is11_8(_sigmaPiInverse, _pi, _rhos)) {
							writer.write(StringUtils.repeat("\t", depth) + byteArrayRepresentation + ";"
									+ _rhos.stream().map(r -> Arrays.toString(r)).collect(Collectors.joining("-"))
									+ "\n");
							desimplify(writer, _sigmaPiInverse, _pi, _rhos, depth + 1);
						} else
							throw new RuntimeException("ERROR");
					}
				}
			}
		}
	}

	protected static boolean is11_8(MulticyclePermutation sigmaPiInverse, byte[] pi, List<byte[]> rhos) {
		int before = sigmaPiInverse.getNumberOfEvenCycles();
		for (byte[] rho : rhos) {
			if (areSymbolsInCyclicOrder(rho, pi)) {
				pi = Util.applyTransposition(rho, pi);
				sigmaPiInverse = PermutationGroups.computeProduct(sigmaPiInverse, new Cycle(rho).getInverse());
			} else {
				return false;
			}
		}
		int after = sigmaPiInverse.getNumberOfEvenCycles();
		return after > before && (float) rhos.size() / ((after - before) / 2) <= ((float) 11 / 8);
	}

	private static boolean areSymbolsInCyclicOrder(byte[] rho, byte[] pi) {
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

	protected static void joinCyclesAndReplaceRhos(MulticyclePermutation sigmaPiInverse, byte[] joiningPair,
			byte[] originalPi, List<byte[]> rhos) {
		Map<Byte, Cycle> symbolToCycle = new HashMap<>();

		for (int i = 0; i < sigmaPiInverse.size(); i++) {
			for (int j = 0; j < sigmaPiInverse.get(i).size(); j++) {
				byte symbol = sigmaPiInverse.get(i).getSymbols()[j];
				symbolToCycle.put(symbol, sigmaPiInverse.get(i));
			}
		}

		Cycle a = symbolToCycle.get(joiningPair[0]);
		Cycle b = symbolToCycle.get(joiningPair[1]);

		a = a.getInverse().getStartingBy(a.getInverse().image(joiningPair[0]));
		b = b.getInverse().getStartingBy(joiningPair[1]);

		byte[] cSymbols = new byte[a.size() + b.size() - 1];
		System.arraycopy(a.getSymbols(), 0, cSymbols, 0, a.size());
		System.arraycopy(b.getSymbols(), 1, cSymbols, a.size(), b.size() - 1);

		Cycle c = new Cycle(cSymbols);
		sigmaPiInverse.add(c.getInverse());
		sigmaPiInverse.remove(symbolToCycle.get(joiningPair[0]));
		sigmaPiInverse.remove(symbolToCycle.get(joiningPair[1]));

		List<byte[]> _rhos = new ArrayList<>();
		byte[] pi = originalPi;
		for (byte[] rho : rhos) {
			byte[] _pi = Util.applyTransposition(rho, pi);
			MulticyclePermutation _rho = PermutationGroups.computeProduct(false,
					new Cycle(Util.replace(removeSymbol(_pi, joiningPair[0]), joiningPair[1], joiningPair[0])),
					new Cycle(Util.replace(removeSymbol(pi, joiningPair[0]), joiningPair[1], joiningPair[0]))
							.getInverse());

			pi = _pi;
			// sometimes _rho = (),
			if (_rho.size() != 0)
				_rhos.add(_rho.asNCycle().getSymbols());
		}

		rhos.clear();
		rhos.addAll(_rhos);
	}

	protected static byte[] removeSymbol(byte[] array, byte symbol) {
		ByteArrayList temp = new ByteArrayList(Arrays.copyOf(array, array.length));
		temp.remove(temp.indexOf(symbol));
		return Arrays.copyOfRange(temp.elements(), 0, temp.size());
	}

	protected static List<byte[]> getJoiningPairs(List<Cycle> cycles, byte[] pi) {
		Set<Byte> symbols = new HashSet<>(Bytes.asList(pi));

		Map<Byte, Byte> symbolToLabel = new HashMap<>();

		for (int i = 0; i < cycles.size(); i++) {
			for (int j = 0; j < cycles.get(i).size(); j++) {
				byte symbol = cycles.get(i).getSymbols()[j];
				symbolToLabel.put(symbol, (byte) i);
				symbols.remove(symbol);
			}
		}

		ByteArrayList _pi = new ByteArrayList(Arrays.copyOf(pi, pi.length));
		_pi.removeAll(new ByteArrayList(Bytes.toArray(symbols)));

		List<byte[]> results = new ArrayList<>();
		for (int i = 0; i < _pi.size(); i++) {
			byte currentLabel = symbolToLabel.get(_pi.get(i));
			byte nextLabel = symbolToLabel.get(_pi.get((i + 1) % _pi.size()));
			if (currentLabel != nextLabel && (_pi.get(i) + 1) % pi.length == _pi.get((i + 1) % _pi.size()))
				results.add(new byte[] { _pi.get(i), _pi.get((i + 1) % _pi.size()) });
		}

		return results;
	}

	private static boolean areNotIntersecting(List<Cycle> cycles, byte[] pi) {
		Set<Byte> symbols = new HashSet<>(Bytes.asList(pi));

		Map<Byte, Byte> symbolToLabel = new HashMap<>();

		for (int i = 0; i < cycles.size(); i++) {
			for (int j = 0; j < cycles.get(i).size(); j++) {
				byte symbol = cycles.get(i).getSymbols()[j];
				symbolToLabel.put(symbol, (byte) i);
				symbols.remove(symbol);
			}
		}

		ByteArrayList _pi = new ByteArrayList(Arrays.copyOf(pi, pi.length));
		_pi.removeAll(new ByteArrayList(Bytes.toArray(symbols)));

		Map<Integer, Integer> states = new HashMap<>();
		for (int i = 0; i < _pi.size(); i++) {
			int currentLabel = symbolToLabel.get(_pi.get(i));
			int state = !states.containsKey(currentLabel) ? 0 : states.get(currentLabel);
			int nextLabel = symbolToLabel.get(_pi.get((i + 1) % _pi.size()));
			if (currentLabel != nextLabel)
				state++;
			if (state == 2)
				return false;
			states.put(currentLabel, state);
		}

		return true;
	}

	protected static List<byte[]> getSorting(String baseFolder, String file) {
		MulticyclePermutation mu = getSigmaPiInverse(file);
		byte[] pi = new byte[mu.getNumberOfSymbols()];
		for (int i = 0; i < pi.length; i++) {
			pi[i] = (byte) i;
		}

		boolean hasSorting = false;

		List<byte[]> rhos = new ArrayList<>();

		try (Reader fr = new BufferedReader(new FileReader(INPUT_DIR + baseFolder + file), 100000)) {
			try (Scanner scanner = new Scanner(fr)) {
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

	protected static MulticyclePermutation getSigmaPiInverse(String file) {
		String str = StringUtils.removeEnd(file.replace("_", ","), ".html");
		str = str.replaceAll("\\[.*?\\]", "");
		str = str.replace(" ", ",");
		return new MulticyclePermutation(str);
	}

	public static <T> Generator<T> combinations(Collection<T> collection, int k) {
		return Factory.createSimpleCombinationGenerator(Factory.createVector(collection), k);
	}
}
