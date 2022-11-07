package br.unb.cic.tdp.proof.seq3_2;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import com.google.common.primitives.Ints;
import lombok.val;
import org.apache.commons.math3.util.Pair;
import org.paukov.combinatorics3.Generator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import static br.unb.cic.tdp.base.CommonOperations.*;

public class Oriented5Cycle {

    public static void main(String[] args) {
        generate().forEach(p -> System.out.println(p.getFirst().toString() + "," + p.getSecond().toString()));
    }

    /**
     * Generate (3,2)-sequences to apply when there is a cycle in \spi with length
     * equals to 5 that doesn't allow the application of a 2-move.
     */
    public static List<Pair<Configuration, List<Cycle>>> generate() {
        val orientedCycle = Cycle.create("(0,3,1,4,2)");
        val triple = new int[]{0, 1, 2};

        val result = new ArrayList<Pair<Configuration, List<Cycle>>>();

        val verifiedConfigurations = new HashSet<Configuration>();

        val spi = new MulticyclePermutation(orientedCycle);

        Generator.permutation(spi.getSymbols()).simple().stream().map(permutation -> Cycle.create(Ints.toArray(permutation))).forEach(pi -> {
            val config = new Configuration(spi, pi);
            if (areSymbolsInCyclicOrder(pi, triple) && !verifiedConfigurations.contains(config)) {
                verifiedConfigurations.add(config);

                val _2Move = searchFor2MoveFromOrientedCycle(spi, pi);
                if (_2Move.isEmpty()) {
                    val moves = searchForSortingSeq(pi, spi, new Stack<>(), 1, 1.5F);
                    assert !moves.isEmpty() : "ERROR";
                    result.add(new Pair<>(config, moves));
                } else {
                    result.add(new Pair<>(config, List.of(_2Move.get())));
                }
            }
        });

        return result;
    }
}
