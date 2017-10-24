package br.unb.cic.tdp1375.eliashartman;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.ehcache.Cache;
import org.ehcache.PersistentCacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;
import org.paukov.combinatorics.ICombinatoricsVector;

import com.google.common.base.Throwables;

import br.unb.cic.tdp1375.permutations.Cycle;
import br.unb.cic.tdp1375.permutations.MulticyclePermutation;
import br.unb.cic.tdp1375.permutations.MulticyclePermutation.ByteArrayRepresentation;
import br.unb.cic.tdp1375.util.Util;

public class TranslateSBT1375ProofOriented extends TranslateSBT1375ProofUnoriented {

	private static final String INPUT_DIR = "/home/luiz/SBT1375_proof/";
	private static final String OUTPUT_DIR = "/home/luiz/cases/oriented/";

	private static final Set<String> VISITED_FILES = new HashSet<>();
	private static final Pattern SIGMA_PI_INVERSE_PATTERN = Pattern.compile(".*\"(.*)\".*");
	private static Cache<ByteArrayRepresentation, Object> cache;
	private static Object FAKE_OBJECT = new Object();

	public static void main(String[] args) throws IOException {
		PersistentCacheManager persistentCacheManager = CacheManagerBuilder.newCacheManagerBuilder()
				.with(CacheManagerBuilder.persistence("/home/luiz/cache/mydata"))
				.withCache("cache",
						CacheConfigurationBuilder.newCacheConfigurationBuilder(ByteArrayRepresentation.class,
								Object.class, ResourcePoolsBuilder.newResourcePoolsBuilder().heap(1, MemoryUnit.GB)
										.offheap(2, MemoryUnit.GB).disk(1, MemoryUnit.TB)))
				.build(true);

		cache = persistentCacheManager.getCache("cache", ByteArrayRepresentation.class, Object.class);

		System.out.println("/* unoriented interleaving pair */");
		translate("bfs_files/", "[3](0_4_2)[3](1_5_3).html", OUTPUT_DIR + "(0,4,2)(1,5,3)");

		System.out.println("/* BAD SMALL COMPONENTS */");

		System.out.println("/* the unoriented interleaving pair */");
		translate("comb_files/", "[3](0_4_2)[3](1_5_3).html", OUTPUT_DIR + "bad-small-(0,4,2)(1,5,3)");
	}

	private static void translate(String baseFolder, String file, String output) {
		File translated = new File(output);
		try (FileWriter fileWriter = new FileWriter(translated)) {
			try (BufferedWriter bufferedWriter = new BufferedWriter(fileWriter, 1 * 1024 * 1024)) {
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
		byte[] _piInverse = Arrays.copyOf(pi, pi.length);
		ArrayUtils.reverse(_piInverse);
		Cycle piInverse = new Cycle(_piInverse);

		for (ICombinatoricsVector<Cycle> combination : combinations(sigmaPiInverse, 2)) {

			for (byte[] joiningPair : getJoiningPairs(combination.getVector(), pi)) {
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

					// skipping cycles > 5, since all 7-cycle accept (4,3)-sequence
					boolean isThereOrientedCycleGreaterThan5 = _sigmaPiInverse.stream()
							.filter(c -> !piInverse.areSymbolsInCyclicOrder(c.getSymbols()) && c.size() > 5)
							.count() > 0;

					if (!isThereOrientedCycleGreaterThan5) {
						boolean isThereOriented5Cycle = false;
						boolean isThereOriented3Segment = false;
						for (Cycle cycle : _sigmaPiInverse) {
							isThereOriented5Cycle |= cycle.size() == 5
									&& !piInverse.areSymbolsInCyclicOrder(cycle.getSymbols());
							for (int i = 0; i < cycle.size(); i++) {
								byte a = cycle.get(i), b = cycle.image(a), c = cycle.image(b);
								if (isApplicable(new byte[] { a, b, c }, _pi)) {
									// there is a valid 2-move
									isThereOriented3Segment = true;
									break;
								}
							}
						}

						if (is11_8(_sigmaPiInverse, _pi, _rhos)) {
							if (!isThereOriented3Segment && isThereOriented5Cycle)
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

	private static boolean isApplicable(byte[] rho, byte[] pi) {
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
}
