package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.util.Pair;

import java.util.Set;
import java.util.Stack;

import static br.unb.cic.tdp.base.CommonOperations.searchForSorting;
import static br.unb.cic.tdp.base.CommonOperations.twoLinesNotation;

public class OrientedFourCycles {

    public static void main(String[] args) {
        Configuration config = new Configuration("(0 2 1 3)");
        SortOrExtend.type3Extensions(config)
                .stream()
                .map(Pair::getSecond)
                .forEach(c -> {
                    System.out.println(c + "-" +
                            searchForSorting(c, Set.of(0), twoLinesNotation(c.getSpi()), c.getPi().getSymbols(), new Stack<>(), 1.5));
                });

        SortOrExtend.type3Extensions(config.getCanonical())
                .stream().map(Pair::getSecond).forEach(System.out::println);

    }
}
