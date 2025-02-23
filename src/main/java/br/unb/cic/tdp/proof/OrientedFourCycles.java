package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.Configuration;

import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import static br.unb.cic.tdp.base.CommonOperations.*;
import static org.paukov.combinatorics3.Generator.permutation;

public class OrientedFourCycles {

    public static void main(String[] args) {
        permutation(List.of(0, 1, 2, 3)).simple().stream()
                .map(permutation -> new Configuration("(" + permutation.stream().map(Object::toString).collect(Collectors.joining(" ")) + ")"))
                .distinct()
                .forEach(configuration -> {
                    if (isOriented(configuration.getPi(), configuration.getSpi().asNCycle())) {
                        System.out.println(searchForSorting(configuration, Set.of(), twoLinesNotation(configuration.getSpi()), configuration.getPi().getSymbols(), new Stack<>(), 1.6));
                    }
                });
    }
}
