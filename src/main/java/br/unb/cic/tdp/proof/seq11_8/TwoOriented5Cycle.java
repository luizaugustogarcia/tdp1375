package br.unb.cic.tdp.proof.seq11_8;

import br.unb.cic.tdp.base.CommonOperations;
import br.unb.cic.tdp.base.Configuration;
import org.paukov.combinatorics.Factory;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Stack;

import static br.unb.cic.tdp.proof.seq11_8.Extensions.loadKnownSortings;
import static br.unb.cic.tdp.proof.seq11_8.Extensions.searchForSorting;

public class Oriented5Cycle {

    public static void main(String[] args) throws IOException {
        final var knownSortings = loadKnownSortings(args[0]);

        final var configs = new LinkedList<Configuration>();
        final var fractions = new float[]{0.1F, 0.3F, 0.5F, 0.2F, 0.4F};

        for (final var permutation : Factory.createPermutationGenerator(Factory.createVector(new Byte[]{1, 1, 1, 1, 1, 2, 2, 2, 2, 2}))) {
            final var signature = new float[10];

            var nextFraction = new int[2];
            for (int i = 0; i < permutation.getSize(); i++) {
                signature[i] = permutation.getValue(i) + fractions[nextFraction[permutation.getValue(i) - 1]];
                nextFraction[permutation.getValue(i) - 1]++;
            }

            configs.add(Configuration.fromSignature(signature));
        }

        configs.forEach(c -> {
            final var sorting = searchForSorting(c, knownSortings);
            if (sorting == null) {
                System.out.println(CommonOperations.searchForSortingSeq(c.getPi(), c.getSpi(), new Stack<>(), c.getSpi().getNumberOfEvenCycles(), 1.375F));
            } else {
                System.out.println(sorting);
            }
        });
    }
}
