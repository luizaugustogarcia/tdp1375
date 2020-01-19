package br.unb.cic.tdp.proof;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.ehcache.Cache;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;

import com.google.common.base.Throwables;

import br.unb.cic.tdp.Util;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.permutation.MulticyclePermutation.CyclicRepresentation;

public class DesimplifyOriented extends DesimplifyUnoriented {

	public static List<Case> generate(final String inputDir) {
		final var persistentCacheManager = CacheManagerBuilder.newCacheManagerBuilder()
				.withCache("cache", CacheConfigurationBuilder.newCacheConfigurationBuilder(CyclicRepresentation.class,
						Serializable.class, ResourcePoolsBuilder.newResourcePoolsBuilder().heap(500, MemoryUnit.GB)))
				.build(true);

		final var cache = persistentCacheManager.getCache("cache", CyclicRepresentation.class, Serializable.class);

		final var visitedFiles = new HashSet<String>();

		final var cases = new ArrayList<Case>();
		// unoriented interleaving pair
		cases.addAll(generate(cache, inputDir + "bfs_files/", "[3](0_4_2)[3](1_5_3).html", visitedFiles));

		// BAD SMALL COMPONENTS
		// the unoriented interleaving pair
		cases.addAll(generate(cache, inputDir + "comb_files/", "[3](0_4_2)[3](1_5_3).html", visitedFiles));

		return cases;
	}

	private static List<Case> generate(final Cache<CyclicRepresentation, Serializable> cache, final String baseFolder,
			final String file, final Set<String> visitedFiles) {
		if (visitedFiles.contains(baseFolder + file))
			return Collections.emptyList();

		final var rhos = getSorting(baseFolder + file);

		final var cases = new ArrayList<Case>();

		if (!rhos.isEmpty()) {
			final var spi = getSigmaPiInverse(file);
			final var n = spi.stream().mapToInt(c -> c.size()).sum();
			final var pi = new byte[n];
			for (var i = 0; i < n; i++) {
				pi[i] = (byte) i;
			}

			cases.addAll(desimplify(cache, spi, pi, rhos));
		} else {
			try (final var fr = new BufferedReader(new FileReader(baseFolder + file), 100000)) {
				try (final var scanner = new Scanner(fr)) {
					scanner.useDelimiter("\\n");

					while (scanner.hasNext()) {
						final var line = scanner.next();

						if (line.startsWith("View")) {
							final var matcher = spiPattern.matcher(line);
							if (matcher.matches())
								cases.addAll(generate(cache, baseFolder, matcher.group(1), visitedFiles));
						}
					}
				}
			} catch (Exception e) {
				Throwables.propagate(e);
			}
		}

		visitedFiles.add(baseFolder + file);

		return cases;
	}

	private static List<Case> desimplify(final Cache<CyclicRepresentation, Serializable> cache,
			final MulticyclePermutation spi, final byte[] pi, final List<byte[]> rhos) {
		final var cases = new ArrayList<Case>();

		final var _piInverse = Arrays.copyOf(pi, pi.length);
		ArrayUtils.reverse(_piInverse);
		final var piInverse = new Cycle(_piInverse);

		for (final var combination : combinations(spi, 2)) {
			for (final var joiningPair : getJoiningPairs(combination.getVector(), pi)) {
				final var _pi = removeSymbol(pi, joiningPair[1]);

				var _spi = new MulticyclePermutation(spi);

				// clone rhos
				final var _rhos = new ArrayList<byte[]>();
				for (final var rho : rhos) {
					_rhos.add(Arrays.copyOf(rho, rho.length));
				}

				joinCyclesAndReplaceRhos(_spi, joiningPair, pi, _rhos);

				_spi = Util.canonicalize(_spi, _pi, _rhos);

				final var cyclicRepresentation = _spi.cyclicRepresentation();

				if (!cache.containsKey(cyclicRepresentation)) {
					cache.put(cyclicRepresentation, FAKE_OBJECT);

					// skipping cycles > 5, since all 7-cycle accept (4,3)-sequence
					final var isThereOrientedCycleGreaterThan5 = _spi.stream()
							.filter(c -> !piInverse.areSymbolsInCyclicOrder(c.getSymbols()) && c.size() > 5).count() > 0;

					if (!isThereOrientedCycleGreaterThan5) {
						var isThereOriented5Cycle = false;
						var isThereOriented3Segment = false;
						for (final var cycle : _spi) {
							isThereOriented5Cycle |= cycle.size() == 5 && !piInverse.areSymbolsInCyclicOrder(cycle.getSymbols());
							for (int i = 0; i < cycle.size(); i++) {
								byte a = cycle.get(i), b = cycle.image(a), c = cycle.image(b);
								// if rho (a,b,c) is applicable
								if (areSymbolsInCyclicOrder(new byte[] { a, b, c }, _pi)) {
									// there is a valid 2-move
									isThereOriented3Segment = true;
									break;
								}
							}
						}

						if (is11_8(_spi, _pi, _rhos)) {
							if (!isThereOriented3Segment && isThereOriented5Cycle) {
								cases.add(new Case(_pi, _spi, _rhos));
							}

							cases.addAll(desimplify(cache, _spi, _pi, _rhos));
						} else {
							throw new RuntimeException("ERROR");
						}
					}
				}
			}
		}

		return cases;
	}
}
