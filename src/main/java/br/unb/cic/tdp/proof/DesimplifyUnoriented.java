package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.CommonOperations;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.permutation.PermutationGroups;
import cern.colt.list.ByteArrayList;
import com.google.common.base.Throwables;
import com.google.common.primitives.Bytes;
import org.apache.commons.lang.StringUtils;
import org.javatuples.Triplet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

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

    private static void generate(final Set<Configuration> verifiedConfigurations, final List<Case> cases, final String baseFolder, final String file,
                                 final Set<String> visitedFiles) {
        if (visitedFiles.contains(baseFolder + file))
            return;

        final var rhos = getSorting(baseFolder + file);

        if (!rhos.isEmpty()) {
            final var spi = getSpi(file);
            final var n = spi.stream().mapToInt(Cycle::size).sum();
            final var pi = new byte[n];
            for (var i = 0; i < n; i++) {
                pi[i] = (byte) i;
            }

            if (CommonOperations.is11_8(spi, pi, rhos)) {
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
            } catch (final Exception e) {
                Throwables.propagate(e);
            }
        }

        visitedFiles.add(baseFolder + file);
    }

    private static void desimplify(final Set<Configuration> verifiedConfigurations, final List<Case> cases,
                                   final MulticyclePermutation spi, final byte[] pi, final List<byte[]> rhos) {
        for (final var combination : CommonOperations.combinations(spi, 2)) {
            // only join cycles which are not intersecting
            if (areNotIntersecting(combination.getVector(), pi)) {
                final var joiningPairs = getJoinPairs(combination.getVector(), pi);

                for (final var joinPair : joiningPairs) {
                    final var join = join(joinPair, spi, pi, rhos);

                    final var cr = CommonOperations.canonicalize(join.getValue0(), join.getValue1(), join.getValue2());
                    final var _pi = cr.getValue1();
                    final var _spi = cr.getValue0();
                    final var _rhos = cr.getValue2();

                    if (CommonOperations.is11_8(_spi, _pi, _rhos)) {
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


    protected static List<byte[]> getSorting(final String file) {
        final var mu = getSpi(file);
        var pi = new byte[mu.getNumberOfSymbols()];
        for (var i = 0; i < pi.length; i++) {
            pi[i] = (byte) i;
        }

        var hasSorting = false;

        final var rhos = new ArrayList<byte[]>();

        try (final var fr = new BufferedReader(new FileReader(file), 1024)) {
            try (final var scanner = new Scanner(fr)) {
                scanner.useDelimiter("\\n");

                while (scanner.hasNext()) {
                    final var line = scanner.next();

                    if (line.contains("SORTING"))
                        hasSorting = true;

                    if (hasSorting) {
                        final var matcher = SORTING_PATTERN.matcher(line);

                        if (matcher.matches()) {
                            final var a = Byte.parseByte(matcher.group(1));
                            final var b = Byte.parseByte(matcher.group(2));
                            final var c = Byte.parseByte(matcher.group(3));

                            final var rho = new byte[]{pi[a], pi[b], pi[c]};
                            rhos.add(rho);

                            pi = CommonOperations.applyTransposition(rho, pi);
                        }
                    }
                }
            }
        } catch (final IOException e) {
            Throwables.propagate(e);
        }

        return rhos;
    }

    protected static MulticyclePermutation getSpi(final String file) {
        final var _file = new File(file);
        var str = StringUtils.removeEnd(_file.getName().replace("_", ","), ".html");
        str = str.replaceAll("\\[.*?\\]", "");
        str = str.replace(" ", ",");
        return new MulticyclePermutation(str);
    }

    /**
     * Performs a join operation, producing new \spi, \pi and \rhos.
     */
    protected static Triplet<MulticyclePermutation, byte[], List<byte[]>> join(final byte[] joinPair,
                                                                               final MulticyclePermutation spi, final byte[] pi, final List<byte[]> rhos) {
        final var symbolToCycle = new HashMap<Byte, Cycle>();

        final var _spi = new MulticyclePermutation(spi);

        for (Cycle cycle : _spi) {
            for (var j = 0; j < cycle.size(); j++) {
                final var symbol = cycle.getSymbols()[j];
                symbolToCycle.put(symbol, cycle);
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
            final var __pi = CommonOperations.applyTransposition(rho, _pi);
            final var _rho = PermutationGroups.computeProduct(false,
                    new Cycle(CommonOperations.replace(removeSymbol(__pi, joinPair[0]), joinPair[1], joinPair[0])),
                    new Cycle(CommonOperations.replace(removeSymbol(_pi, joinPair[0]), joinPair[1], joinPair[0])).getInverse());

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

    protected static List<byte[]> getJoinPairs(final List<Cycle> cycles, final byte[] pi) {
        final var symbols = new HashSet<>(Bytes.asList(pi));

        final var symbolToLabel = new HashMap<Byte, Byte>();

        for (var i = 0; i < cycles.size(); i++) {
            for (var j = 0; j < cycles.get(i).size(); j++) {
                final var symbol = cycles.get(i).getSymbols()[j];
                symbolToLabel.put(symbol, (byte) i);
                symbols.remove(symbol);
            }
        }

        final var _pi = new ByteArrayList(Arrays.copyOf(pi, pi.length));
        _pi.removeAll(new ByteArrayList(Bytes.toArray(symbols)));

        final var results = new ArrayList<byte[]>();
        for (var i = 0; i < _pi.size(); i++) {
            final var currentLabel = symbolToLabel.get(_pi.get(i));
            final var nextLabel = symbolToLabel.get(_pi.get((i + 1) % _pi.size()));
            if (!currentLabel.equals(nextLabel) && (_pi.get(i) + 1) % pi.length == _pi.get((i + 1) % _pi.size()))
                results.add(new byte[]{_pi.get(i), _pi.get((i + 1) % _pi.size())});
        }

        return results;
    }

    private static boolean areNotIntersecting(final List<Cycle> cycles, final byte[] pi) {
        final var symbols = new HashSet<>(Bytes.asList(pi));

        final var symbolToLabel = new HashMap<Byte, Byte>();

        for (var i = 0; i < cycles.size(); i++) {
            for (var j = 0; j < cycles.get(i).size(); j++) {
                final var symbol = cycles.get(i).getSymbols()[j];
                symbolToLabel.put(symbol, (byte) i);
                symbols.remove(symbol);
            }
        }

        final var _pi = new ByteArrayList(Arrays.copyOf(pi, pi.length));
        _pi.removeAll(new ByteArrayList(Bytes.toArray(symbols)));

        final var states = new HashMap<Integer, Integer>();
        for (var i = 0; i < _pi.size(); i++) {
            final int currentLabel = symbolToLabel.get(_pi.get(i));
            final int nextLabel = symbolToLabel.get(_pi.get((i + 1) % _pi.size()));
            var state = states.getOrDefault(currentLabel, 0);
            if (currentLabel != nextLabel)
                state++;
            if (state == 2)
                return false;
            states.put(currentLabel, state);
        }

        return true;
    }
}
