package br.unb.cic.tdp.proof;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;

import com.google.common.base.Throwables;

import br.unb.cic.tdp.Util;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;

class DesimplifyOriented extends DesimplifyUnoriented {

	static List<Case> generate(final String inputDir) {
		final var verifiedConfigurations = new HashSet<Configuration>();

		final var visitedFiles = new HashSet<String>();

		final var cases = new ArrayList<Case>();

		// unoriented interleaving pair
		generate(verifiedConfigurations, cases, inputDir + "bfs_files/", "[3](0_4_2)[3](1_5_3).html", visitedFiles);

		// BAD SMALL COMPONENTS
		// the unoriented interleaving pair
		generate(verifiedConfigurations, cases, inputDir + "comb_files/", "[3](0_4_2)[3](1_5_3).html", visitedFiles);

		return cases;
	}

	private static void generate(final Set<Configuration> verifiedConfigurations, List<Case> cases,
			final String baseFolder, final String file, final Set<String> visitedFiles) {
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
		final var _piInverse = Arrays.copyOf(pi, pi.length);
		ArrayUtils.reverse(_piInverse);
		final var piInverse = new Cycle(_piInverse);

		for (final var combination : combinations(spi, 2)) {
			for (final var joinPair : getJoiningPairs(combination.getVector(), pi)) {
				final var join = join(joinPair, spi, pi, rhos);

				final var cr = Util.canonicalize(join.getValue0(), join.getValue1(), join.getValue2());
				final var _pi = cr.getValue1();
				final var _spi = cr.getValue0();
				final var _rhos = cr.getValue2();

				// skipping configuration containing cycles > 5, since all oriented 7-cycle
				// accept (4,3)-sequence
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

					if (isThereOriented5Cycle && !isThereOriented3Segment) {
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
	}
}
