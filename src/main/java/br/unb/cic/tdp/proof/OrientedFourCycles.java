package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.util.Pair;

import java.util.Set;
import java.util.Stack;

import static br.unb.cic.tdp.base.CommonOperations.searchForSorting;
import static br.unb.cic.tdp.base.CommonOperations.twoLinesNotation;

public class OrientedFourCycles {

    public static void main(String[] args) {
        SortOrExtend.type3Extensions(new Configuration("(0 2 1)"))
                .stream()
                .map(Pair::getSecond)
                .forEach(c -> {
                    System.out.println(c + "-" +
                            searchForSorting(c, Set.of(0), twoLinesNotation(c.getSpi()), c.getPi().getSymbols(), new Stack<>(), 1.5));
                });
    }
}
