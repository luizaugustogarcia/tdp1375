package br.unb.cic.tdp;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.util.Pair;
import lombok.SneakyThrows;
import org.apache.commons.collections.ListUtils;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static br.unb.cic.tdp.base.CommonOperations.*;
import static br.unb.cic.tdp.permutation.PermutationGroups.computeProduct;

public class EliasAndHartman extends BaseAlgorithm {

    @SneakyThrows
    public EliasAndHartman() {
        final var _11_8sortings = new HashMap<Configuration, List<Cycle>>();

        Files.lines(Paths.get(this.getClass().getClassLoader()
                .getResource("known-sortings").toURI())).forEach(line -> {
            final var lineSplit = line.trim().split("->");
            if (lineSplit.length > 1) {
                final var spi = new MulticyclePermutation(lineSplit[0].split("#")[1]);
                _11_8sortings.put(new Configuration(spi),
                        Arrays.stream(lineSplit[1].split(";")).map(Cycle::new).collect(Collectors.toList()));
            }
        });

        _11_8cases = new Pair<>(_11_8sortings, _11_8sortings.keySet().stream()
                .collect(Collectors.groupingBy(Configuration::hashCode)));
    }

    public static void main(String[] args) {
        final var eliasAndHartman = new EliasAndHartman();
        System.out.println(eliasAndHartman.sort(new Cycle(args[0])));
    }

    @SuppressWarnings({"unchecked"})
    public int sort(Cycle pi) {
        pi = simplify(pi);

        final var n = pi.size();

        final var sigma = CANONICAL_PI[n];

        var spi = computeProduct(true, n, sigma, pi.getInverse());

        var distance = 0;

        final var _2_2Seq = searchFor2_2Seq(spi, pi);
        if (_2_2Seq != null) {
            pi = computeProduct(_2_2Seq.getSecond(), _2_2Seq.getFirst(), pi).asNCycle();
            spi = computeProduct(true, sigma, pi.getInverse());
            distance += 2;
        }

        while (thereAreOddCycles(spi)) {
            apply2MoveTwoOddCycles(spi, pi);
            spi = computeProduct(true, sigma, pi.getInverse());
            distance += 1;
        }

        final List<Cycle> bigLambda = new ArrayList<>(); // bad small components

        List<Cycle> bigTheta; // unmarked cycles
        while (!(bigTheta = ListUtils.subtract(spi.stream().filter(c -> c.size() > 1).collect(Collectors.toList()), bigLambda)).isEmpty()) {
            final var _2move = searchFor2MoveFromOrientedCycle(bigTheta, pi);
            if (_2move != null) {
                pi = computeProduct(_2move, pi).asNCycle();
                spi = computeProduct(true, sigma, pi.getInverse());
                distance += 1;
            } else {
                List<Cycle> bigGamma = new ArrayList<>();
                final var gamma = bigTheta.stream().filter(c -> c.size() > 1).findFirst().get();
                bigGamma.add(new Cycle(gamma.get(0), gamma.get(1), gamma.get(2)));

                var badSmallComponent = false;

                for (var i = 0; i < 8; i++) {
                    final var norm = get3Norm(bigGamma);

                    bigGamma = extend(bigGamma, spi, pi);

                    if (norm == get3Norm(bigGamma)) {
                        badSmallComponent = true;
                        break;
                    }

                    final var seq = searchForSeq(bigGamma, pi, _11_8cases);
                    if (seq != null) {
                        for (final var rho : seq)
                            pi = computeProduct(rho, pi).asNCycle();
                        spi = computeProduct(true, sigma, pi.getInverse());
                        distance += seq.size();
                        break;
                    }
                }

                if (badSmallComponent) {
                    bigLambda.addAll(bigGamma);
                }
            }

            if (get3Norm(bigLambda) >= 8) {
                final var _11_8Seq = searchForSeq(bigLambda, pi, _11_8cases);
                for (final var rho : _11_8Seq) {
                    pi = computeProduct(rho, pi).asNCycle();
                }
                spi = computeProduct(true, sigma, pi.getInverse());
                distance += _11_8Seq.size();
                bigLambda.clear();
            }
        }

        // At this point 3-norm of spi is less than 8
        while (!spi.isIdentity()) {
            final var _2move = searchFor2MoveFromOrientedCycle(spi, pi);
            if (_2move != null) {
                pi = computeProduct(_2move, pi).asNCycle();
                distance += 1;
            } else {
                apply3_2_Unoriented(spi, pi);
                distance += 3;
            }
            spi = computeProduct(true, sigma, pi.getInverse());
        }

        return distance;
    }

    public List<Cycle> extend(final List<Cycle> mu, final MulticyclePermutation spi, final Cycle pi) {
        final var extension = super.extend(mu, spi, pi);
        if (extension != null) {
            return extension;
        }
        return mu;
    }
}
