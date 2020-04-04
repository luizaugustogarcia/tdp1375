package br.unb.cic.tdp;

import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import org.apache.commons.collections.ListUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static br.unb.cic.tdp.base.CommonOperations.*;
import static br.unb.cic.tdp.permutation.PermutationGroups.computeProduct;

public class EliasAndHartman extends BaseAlgorithm {

    @SuppressWarnings({"unchecked"})
    public int sort(Cycle pi) {
        pi = simplify(pi);

        final var n = pi.size();

        final var sigma = CANONICAL_PI[n];

        var spi = computeProduct(true, n, sigma, pi.getInverse());
        System.out.println(spi.get3Norm());

        var distance = 0;

        final var _2_2Seq = searchFor2_2Seq(spi, pi);
        if (_2_2Seq != null) {
            pi = computeProduct(_2_2Seq.getSecond(), _2_2Seq.getFirst(), pi).asNCycle();
            distance += 2;
            spi = computeProduct(true, sigma, pi.getInverse());
        }

        while (thereAreOddCycles(spi)) {
            apply2MoveTwoOddCycles(spi, pi);
            distance += 1;
            spi = computeProduct(true, sigma, pi.getInverse());
        }

        final List<Collection<Cycle>> badSmallComponents = new ArrayList<>();

        List<Cycle> spiMinusBadSmallComponents;
        while (!spi.isIdentity() && !(spiMinusBadSmallComponents = ListUtils.subtract(
                spi.stream().filter(c -> c.size() > 1).collect(Collectors.toList()),
                badSmallComponents.stream().flatMap(Collection::stream).collect(Collectors.toList()))).isEmpty()) {
            final var _2move = searchFor2MoveFromOrientedCycle(spi, pi);
            if (_2move != null) {
                pi = computeProduct(_2move, pi).asNCycle();
                distance += 1;
            } else {
                List<Cycle> mu = new ArrayList<>();
                final var initialFactor = spiMinusBadSmallComponents.stream().filter(c -> c.size() > 1)
                        .findFirst().get();
                mu.add(new Cycle(initialFactor.get(0), initialFactor.get(1), initialFactor.get(2)));

                var badSmallComponent = false;
                // O(n)
                for (var i = 0; i < 8; i++) {
                    final var norm = getNorm(mu);

                    mu = extend(mu, spi, pi);

                    if (norm == getNorm(mu)) {
                        badSmallComponent = true;
                        break;
                    }

                    final var _11_8Seq = searchForSeq(mu, pi, _11_8cases);
                    if (_11_8Seq != null) {
                        for (final var rho : _11_8Seq)
                            pi = computeProduct(rho, pi).asNCycle();
                        distance += _11_8Seq.size();
                        badSmallComponent = false;
                        break;
                    }
                }

                if (badSmallComponent)
                    badSmallComponents.add(mu);
            }

            spi = computeProduct(true, sigma, pi.getInverse());
        }

        // Bad small components
        final List<Cycle> mu = new ArrayList<>();
        final var iterator = badSmallComponents.iterator();
        while (iterator.hasNext()) {
            mu.addAll(iterator.next());
            iterator.remove();
            if (getNorm(mu) >= 16) {
                final var _11_8Seq = searchForSeq(mu, pi, _11_8cases);
                for (final var rho : _11_8Seq) {
                    pi = computeProduct(rho, pi).asNCycle();
                }
                distance += _11_8Seq.size();
                spi = computeProduct(true, sigma, pi.getInverse());
            }
        }

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
