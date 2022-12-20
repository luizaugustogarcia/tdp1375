package br.unb.cic.tdp;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.permutation.PermutationGroups;
import br.unb.cic.tdp.util.Pair;
import com.google.common.collect.Multimap;
import lombok.SneakyThrows;
import lombok.val;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static br.unb.cic.tdp.base.CommonOperations.*;
import static br.unb.cic.tdp.permutation.PermutationGroups.computeProduct;

public class EliasAndHartman extends AbstractSbtAlgorithm {

    public Pair<Cycle, List<Cycle>> doSort(Cycle pi) {
        val sorting = new ArrayList<Cycle>();

        val simplification = simplify(pi);
        pi = simplification;

        val n = pi.size();

        val sigmaPrime = new int[n];
        for (int i = 0; i < pi.size(); i++) {
            sigmaPrime[i] = i;
        }
        val sigma = Cycle.create(sigmaPrime);

        var spi = computeProduct(true, n, sigma, pi.getInverse());

        val _2_2Seq = searchFor2_2Seq(spi, pi);
        if (_2_2Seq.isPresent()) {
            pi = computeProduct(_2_2Seq.get().getSecond(), _2_2Seq.get().getFirst(), pi).asNCycle();
            spi = computeProduct(true, pi.size(), sigma, pi.getInverse());
            sorting.addAll(Arrays.asList(_2_2Seq.get().getFirst(), _2_2Seq.get().getSecond()));
        }

        while (thereAreOddCycles(spi)) {
            val pair = apply2MoveTwoOddCycles(spi, pi);
            sorting.add(pair.getFirst());
            pi = pair.getSecond();
            spi = computeProduct(true, pi.size(), sigma, pi.getInverse());
        }

        final List<List<Cycle>> badSmallComponents = new ArrayList<>();

        val nonBadSmallComponents = new HashSet<>(spi.getNonTrivialCycles());
        while (!nonBadSmallComponents.isEmpty()) {
            val _2move = searchFor2MoveFromOrientedCycle(nonBadSmallComponents, pi);
            if (_2move.isPresent()) {
                pi = applyTranspositionOptimized(pi, _2move.get());
                spi = computeProduct(true, pi.size(), sigma, pi.getInverse());
                sorting.add(_2move.get());
            } else {
                List<Cycle> configuration = new ArrayList<>();
                val gamma = nonBadSmallComponents.stream().filter(c -> c.size() > 1).findFirst().get();
                configuration.add(Cycle.create(gamma.get(0), gamma.get(1), gamma.get(2)));

                var badSmallComponent = false;

                for (var i = 0; i < 8; i++) {
                    val norm = get3Norm(configuration);

                    configuration = ehExtend(configuration, spi, pi);

                    if (norm == get3Norm(configuration)) {
                        badSmallComponent = true;
                        break;
                    }

                    val seq = searchFor11_8_Seq(configuration, pi);
                    if (seq.isPresent()) {
                        for (val move : seq.get())
                            pi = applyTranspositionOptimized(pi, move);
                        spi = computeProduct(true, pi.size(), sigma, pi.getInverse());
                        sorting.addAll(seq.get());
                        break;
                    }
                }

                if (badSmallComponent) {
                    badSmallComponents.add(configuration);
                }
            }

            val badSmallComponentsCycles = badSmallComponents.stream().flatMap(Collection::stream).collect(Collectors.toList());

            if (get3Norm(badSmallComponentsCycles) >= 8) {
                val _11_8Seq = searchFor11_8_Seq(badSmallComponentsCycles, pi);
                for (val move : _11_8Seq.get()) {
                    pi = applyTranspositionOptimized(pi, move);
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
            val _2move = searchFor2MoveFromOrientedCycle(spi, pi);
            if (_2move.isPresent()) {
                sorting.add(_2move.get());
                pi = applyTranspositionOptimized(pi, _2move.get()).asNCycle();
            } else {
                val pair = apply3_2_Unoriented(spi, pi);
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
                .getResource("eh-sortings.txt").toURI())).parallel().forEach(line -> {
            val lineSplit = line.trim().split("->");
            if (lineSplit.length > 1) {
                var permutation = lineSplit[0].split("#")[1];
                val spi = new MulticyclePermutation(permutation);
                val config = new Configuration(spi);
                sortings.put(config.hashCode(),
                        new Pair<>(config, Arrays.stream(lineSplit[1].split(";")).map(Cycle::create).collect(Collectors.toList())));
            }
        });
    }

    public static void main(String[] args) {
        val eliasAndHartman = new EliasAndHartman();
        val permutation = "0," + args[0];
        val moves = eliasAndHartman.sort(permutation);
        var pi = moves.getFirst();
        System.out.println(pi);
        for (Cycle move : moves.getSecond()) {
            pi = PermutationGroups.computeProduct(move, pi).asNCycle();
            System.out.println(pi);
        }
    }
}
