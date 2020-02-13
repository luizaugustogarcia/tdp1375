package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.Configuration;
import br.unb.cic.tdp.Silvaetal;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import cern.colt.list.ByteArrayList;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Bytes;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static br.unb.cic.tdp.BaseAlgorithm.getNorm;
import static br.unb.cic.tdp.CommonOperations.CANONICAL_PI;
import static br.unb.cic.tdp.CommonOperations.signature;
import static br.unb.cic.tdp.permutation.PermutationGroups.computeProduct;
import static br.unb.cic.tdp.util.ByteArrayOperations.replace;
import static java.util.stream.Collectors.toCollection;

public class ProofVerifier {

    public static void main(String[] args) throws IOException {
        // mvn exec:java -Dexec.mainClass="br.unb.cic.tdp.proof.ProofVerifier" -Dexec.args=".\\proof\\"
        final var sortings = loadConfigurations(args[0]);
        // intersecting pair
        verify(new Configuration(new MulticyclePermutation("(0,3,1)(2,5,4)"), new Cycle("0,1,2,3,4,5")), sortings, 0);
        // interleaving pair
        verify(new Configuration(new MulticyclePermutation("(0,4,2)(1,5,3)"), new Cycle("0,1,2,3,4,5")), sortings, 0);
    }

    private static Set<Configuration> loadConfigurations(String proofFolder) throws IOException {
        return Files.lines(Paths.get(proofFolder + "lemma-24-unoriented.txt"))
                .filter(line -> line.contains("->")).map(line -> {
                    final var lineSplit = line.trim().split("#")[1].split("->");
                    final var spi = new MulticyclePermutation(lineSplit[0]);
                    final var pi = CANONICAL_PI[spi.getNumberOfSymbols()];
                    return new Configuration(spi, pi);
                }).collect(Collectors.toSet());
    }

    public static void verify(final Configuration configuration, final Set<Configuration> sortings, final int depth) {
        if (hasSorting(configuration, sortings)) {
            //System.out.println(StringUtils.repeat("\t", depth) + configuration.toString() + "->HAS SORTING");
            return;
        } else {
            if (configuration.isFull()) {
                if (isValid(configuration)) {
                    System.out.println(StringUtils.repeat("\t", depth) + configuration.toString() + "->BAD SMALL COMPONENT");
                } else {
                    System.out.println(StringUtils.repeat("\t", depth) + configuration.toString() + "->INVALID");
                }
            }
        }

        final var spi = configuration.getSpi();

        if (spi.get3Norm() > 8) {
            throw new RuntimeException("ERROR");
        }

        for (final var extension : type1Extensions(configuration)) {
            verify(extension, sortings, depth + 1);
        }

        for (final var extension : type2Extensions(configuration)) {
            verify(extension, sortings, depth + 1);
        }

        for (final var extension : type3Extensions(configuration)) {
            verify(extension, sortings, depth + 1);
        }
    }

    private static boolean hasSorting(final Configuration configuration, final Set<Configuration> sortings) {
        if (isSimple(configuration)) {
            return sortings.contains(configuration);
        }
        return subConfigurationHasSorting(configuration, sortings);
    }

    private static boolean isSimple(final Configuration configuration) {
        return !configuration.getSpi().stream().anyMatch(c -> c.size() > 3);
    }

    private static boolean subConfigurationHasSorting(final Configuration configuration, final Set<Configuration> sortings) {
        for (final var cycle : configuration.getSpi()) {
            for (int i = 0; i < (cycle.size() == 3 ? 1 : cycle.size()); i++) {
                byte a = cycle.get(i), b = cycle.pow(a, 1), c = cycle.pow(a, 2);

                List<Cycle> mu = new ArrayList<>();

                mu.add(new Cycle(a, b, c));

                for (var j = 0; j < 8; j++) {
                    final var norm = getNorm(mu);

                    mu = Silvaetal.extend(mu, configuration.getSpi(), configuration.getPi());

                    if (norm == getNorm(mu)) {
                        break;
                    }

                    if (sortings.contains(toConfiguration(mu))) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static Configuration toConfiguration(final List<Cycle> mu) {
        final var maxSymbol = mu.stream().flatMap(c -> Bytes.asList(c.getSymbols()).stream())
                .mapToInt(b -> (int) b).max().getAsInt();
        final var pi = Bytes.toArray(mu.stream().flatMap(c -> Bytes.asList(c.getSymbols()).stream())
                .collect(Collectors.toCollection(HashSet::new)));

        final var substitution = new byte[maxSymbol + 1];

        for (int i = 0; i < pi.length; i++) {
            substitution[pi[i]] = (byte) i;
        }

        final var configuration = new Configuration(mu.stream().map(c -> new Cycle(replace(c.getSymbols().clone(), substitution)))
                .collect(Collectors.toCollection(MulticyclePermutation::new)), CANONICAL_PI[pi.length]);

        return configuration;
    }

    private static boolean isValid(Configuration configuration) {
        final var sigma = computeProduct(configuration.getSpi(), configuration.getPi());
        return sigma.size() == 1 && sigma.stream().findFirst().get().size() == configuration.getPi().size();
    }

    private static Set<Configuration> type1Extensions(final Configuration configuration) {
        final var pi = configuration.getPi();
        final var spi = configuration.getSpi();

        final var newCycleLabel = (byte) (spi.size() + 1);

        final var result = new HashSet<Configuration>();

        final var signature = signature(spi, pi);
        for (int i = 0; i < signature.length; i++) {
            if (signature[i] == signature[(i + 1) % signature.length]) {
                final var a = (i + 1) % signature.length;
                for (int b = 0; b < signature.length; b++) {
                    for (int c = b; c < signature.length; c++) {
                        if (!(a == b && b == c)) {
                            result.add(toConfiguration(extendSignature(signature, newCycleLabel, a, b, c)));
                        }
                    }
                }
            }
        }

        return result;
    }

    private static Set<Configuration> type2Extensions(final Configuration configuration) {
        if (!configuration.isFull()) {
            return Collections.emptySet();
        }

        final var pi = configuration.getPi();
        final var spi = configuration.getSpi();

        final var newCycleLabel = (byte) (spi.size() + 1);

        final var result = new HashSet<Configuration>();

        final var signature = signature(spi, pi);
        for (int a = 0; a < signature.length; a++) {
            for (int b = a; b < signature.length; b++) {
                for (int c = b; c < signature.length; c++) {
                    if (!(a == b && b == c)) {
                        result.add(toConfiguration(extendSignature(signature, newCycleLabel, a, b, c)));
                    }
                }
            }
        }

        return result;
    }

    private static Set<Configuration> type3Extensions(final Configuration configuration) {
        final var pi = configuration.getPi();
        final var spi = configuration.getSpi();

        final var result = new HashSet<Configuration>();

        final var signature = signature(spi, pi);
        for (int label = 1; label <= configuration.getSpi().size(); label++) {
            for (int a = 0; a < signature.length; a++) {
                for (int b = a; b < signature.length; b++) {
                    final var extension = toConfiguration(extendSignature(signature, (byte) label, a, b));
                    if (extension.numberOfOpenGates() <= 2) {
                        result.add(extension);
                    }
                }
            }
        }

        return result;
    }

    private static Configuration toConfiguration(final ByteArrayList extension) {
        final var cyclesByLabel = new HashMap<Byte, ByteArrayList>();
        for (int j = extension.size() - 1; j >= 0; j--) {
            final var label = extension.get(j);
            cyclesByLabel.computeIfAbsent(label, l -> new ByteArrayList());
            cyclesByLabel.get(label).add((byte) j);
        }

        final var spi = cyclesByLabel.values().stream().map(Cycle::new)
                .collect(toCollection(MulticyclePermutation::new));

        return new Configuration(spi, CANONICAL_PI[spi.getNumberOfSymbols()]);
    }

    private static ByteArrayList extendSignature(final byte[] signature, final byte label, final int... positions) {
        Preconditions.checkArgument(1 < positions.length && positions.length <= 3);
        Arrays.sort(positions);
        final var extension = new ByteArrayList(signature);
        for (int i = 0; i < positions.length; i++) {
            extension.beforeInsert(positions[i] + i, label);
        }
        return extension;
    }
}