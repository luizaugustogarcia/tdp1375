package br.unb.cic.tdp;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import lombok.SneakyThrows;
import org.apache.commons.collections.ListUtils;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static br.unb.cic.tdp.base.CommonOperations.*;
import static br.unb.cic.tdp.permutation.PermutationGroups.computeProduct;

public class EliasAndHartman extends BaseAlgorithm {

    @SuppressWarnings({"unchecked"})
    public List<Cycle> sort(Cycle pi) {
        final var sorting = new ArrayList<Cycle>();

        pi = simplify(pi);

        final var n = pi.size();

        final var _sigma = new byte[n];
        for (int i = 0; i < pi.size(); i++) {
            _sigma[i] = (byte)i;
        }
        final var sigma = Cycle.create(_sigma);

        var spi = computeProduct(true, n, sigma, pi.getInverse());

        final var _2_2Seq = searchFor2_2Seq(spi, pi);
        if (_2_2Seq.isPresent()) {
            pi = computeProduct(_2_2Seq.get().getSecond(), _2_2Seq.get().getFirst(), pi).asNCycle();
            spi = computeProduct(true, sigma, pi.getInverse());
            sorting.addAll(Arrays.asList(_2_2Seq.get().getFirst(), _2_2Seq.get().getSecond()));
        }

        while (thereAreOddCycles(spi)) {
            final var pair = apply2MoveTwoOddCycles(spi, pi);
            sorting.add(pair.getFirst());
            pi = pair.getSecond();
            spi = computeProduct(true, sigma, pi.getInverse());
        }

        final List<Cycle> bigLambda = new ArrayList<>(); // bad small components

        List<Cycle> bigTheta; // unmarked cycles
        while (!(bigTheta = ListUtils.subtract(spi.stream().filter(c -> c.size() > 1).collect(Collectors.toList()), bigLambda)).isEmpty()) {
            final var _2move = searchFor2MoveFromOrientedCycle(bigTheta, pi);
            if (_2move.isPresent()) {
                pi = computeProduct(_2move.get(), pi).asNCycle();
                spi = computeProduct(true, sigma, pi.getInverse());
                sorting.add(_2move.get());
            } else {
                List<Cycle> bigGamma = new ArrayList<>();
                final var gamma = bigTheta.stream().filter(c -> c.size() > 1).findFirst().get();
                bigGamma.add(Cycle.create(gamma.get(0), gamma.get(1), gamma.get(2)));

                var badSmallComponent = false;

                for (var i = 0; i < 8; i++) {
                    final var norm = get3Norm(bigGamma);

                    bigGamma = extend(bigGamma, spi, pi);

                    if (norm == get3Norm(bigGamma)) {
                        badSmallComponent = true;
                        break;
                    }

                    final var seq = searchForSeq(bigGamma, pi);
                    if (seq.isPresent()) {
                        for (final var move : seq.get())
                            pi = computeProduct(move, pi).asNCycle();
                        spi = computeProduct(true, sigma, pi.getInverse());
                        sorting.addAll(seq.get());
                        break;
                    }
                }

                if (badSmallComponent) {
                    bigLambda.addAll(bigGamma);
                }
            }

            if (get3Norm(bigLambda) >= 8) {
                final var _11_8Seq = searchForSeq(bigLambda, pi);
                for (final var move : _11_8Seq.get()) {
                    pi = computeProduct(move, pi).asNCycle();
                }
                spi = computeProduct(true, sigma, pi.getInverse());
                sorting.addAll(_11_8Seq.get());
                bigLambda.clear();
            }
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
            spi = computeProduct(true, sigma, pi.getInverse());
        }

        return sorting;
    }

    @SneakyThrows
    @Override
    protected void load11_8Sortings(final Map<Configuration, List<Cycle>> sortings) {
        Files.lines(Paths.get(this.getClass().getClassLoader()
                .getResource("known-sortings").toURI())).forEach(line -> {
            final var lineSplit = line.trim().split("->");
            if (lineSplit.length > 1) {
                var permutation = lineSplit[0].split("#")[1];
                final var spi = new MulticyclePermutation(permutation);
                sortings.put(new Configuration(spi),
                        Arrays.stream(lineSplit[1].split(";")).map(s -> Cycle.create(s)).collect(Collectors.toList()));
            }
        });
    }
}
