package br.unb.cic.tdp;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.permutation.PermutationGroups;
import br.unb.cic.tdp.util.Pair;
import com.google.common.collect.Multimap;
import lombok.SneakyThrows;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static br.unb.cic.tdp.base.CommonOperations.*;
import static br.unb.cic.tdp.permutation.PermutationGroups.computeProduct;

public class EliasAndHartman extends BaseAlgorithm {

    @SuppressWarnings({"unchecked"})
    public Pair<Cycle, List<Cycle>> sort(Cycle pi) {
        final var sorting = new ArrayList<Cycle>();

        final var simplification = simplify(pi);
        pi = simplification;

        final var n = pi.size();

        final var _sigma = new int[n];
        for (int i = 0; i < pi.size(); i++) {
            _sigma[i] = (int)i;
        }
        final var sigma = Cycle.create(_sigma);

        var spi = computeProduct(true, n, sigma, pi.getInverse());

        final var _2_2Seq = searchFor2_2Seq(spi, pi);
        if (_2_2Seq.isPresent()) {
            pi = computeProduct(_2_2Seq.get().getSecond(), _2_2Seq.get().getFirst(), pi).asNCycle();
            spi = computeProduct(true, pi.size(), sigma, pi.getInverse());
            sorting.addAll(Arrays.asList(_2_2Seq.get().getFirst(), _2_2Seq.get().getSecond()));
        }

        while (thereAreOddCycles(spi)) {
            final var pair = apply2MoveTwoOddCycles(spi, pi);
            sorting.add(pair.getFirst());
            pi = pair.getSecond();
            spi = computeProduct(true, pi.size(), sigma, pi.getInverse());
        }

        final List<List<Cycle>> badSmallComponents = new ArrayList<>();

        final var nonBadSmallComponents = new HashSet<>(spi.getNonTrivialCycles());
        while (!nonBadSmallComponents.isEmpty()) {
            final var _2move = searchFor2MoveFromOrientedCycle(nonBadSmallComponents, pi);
            if (_2move.isPresent()) {
                pi = computeProduct(_2move.get(), pi).asNCycle();
                spi = computeProduct(true, pi.size(), sigma, pi.getInverse());
                sorting.add(_2move.get());
            } else {
                List<Cycle> configuration = new ArrayList<>();
                final var gamma = nonBadSmallComponents.stream().filter(c -> c.size() > 1).findFirst().get();
                configuration.add(Cycle.create(gamma.get(0), gamma.get(1), gamma.get(2)));

                var badSmallComponent = false;

                for (var i = 0; i < 8; i++) {
                    final var norm = get3Norm(configuration);

                    configuration = ehExtend(configuration, spi, pi);

                    if (norm == get3Norm(configuration)) {
                        badSmallComponent = true;
                        break;
                    }

                    final var seq = searchFor11_8_Seq(configuration, pi);
                    if (seq.isPresent()) {
                        for (final var move : seq.get())
                            pi = computeProduct(move, pi).asNCycle();
                        spi = computeProduct(true, pi.size(), sigma, pi.getInverse());
                        sorting.addAll(seq.get());
                        break;
                    }
                }

                if (badSmallComponent) {
                    badSmallComponents.add(configuration);
                }
            }

            final var badSmallComponentsCycles = badSmallComponents.stream().flatMap(Collection::stream).collect(Collectors.toList());

            if (get3Norm(badSmallComponentsCycles) >= 8) {
                final var _11_8Seq = searchForSeqBadSmallComponents(badSmallComponentsCycles, pi);
                for (final var move : _11_8Seq.get()) {
                    pi = computeProduct(move, pi).asNCycle();
                }
                spi = computeProduct(true, pi.size(), sigma, pi.getInverse());
                sorting.addAll(_11_8Seq.get());
                badSmallComponents.clear();
            }

            nonBadSmallComponents.clear();
            nonBadSmallComponents.addAll(spi.getNonTrivialCycles());
            badSmallComponentsCycles.forEach(nonBadSmallComponents::remove);
        }

        // At this point 3-norm of spi is less than 8
        while (!spi.isIdentity()) {
            final var _2move = searchFor2MoveFromOrientedCycle(spi, pi);
            if (_2move.isPresent()) {
                sorting.add(_2move.get());
                pi = computeProduct(_2move.get(), pi).asNCycle();
            } else {
                final var pair = apply3_2_Unoriented(spi, pi);
                sorting.addAll(pair.getFirst());
                pi = pair.getSecond();
            }
            spi = computeProduct(true, pi.size(), sigma, pi.getInverse());
        }

        return new Pair<>(simplification, sorting);
    }

    @SneakyThrows
    @Override
    protected void load11_8Sortings(final Multimap<Integer, Pair<Configuration, List<Cycle>>> sortings) {
        Files.lines(Paths.get(this.getClass().getClassLoader()
                .getResource("eh-sortings.txt").toURI())).forEach(line -> {
            final var lineSplit = line.trim().split("->");
            if (lineSplit.length > 1) {
                var permutation = lineSplit[0].split("#")[1];
                final var spi = new MulticyclePermutation(permutation);
                final var config = new Configuration(spi);
                sortings.put(config.hashCode(),
                        new Pair<>(config, Arrays.stream(lineSplit[1].split(";")).map(Cycle::create).collect(Collectors.toList())));
            }
        });
    }

    public static void main(String[] args) {
        System.out.println("Loading cases into memory...");
        final var eh = new EliasAndHartman();
        System.out.println("Finished loading...");
        var pi = Cycle.create(args[0]);
        final var moves = eh.sort(pi);
        System.out.println(pi);
        for (Cycle move : moves.getSecond()) {
            pi = PermutationGroups.computeProduct(move, pi).asNCycle();
            System.out.println(pi);
        }
    }
}
