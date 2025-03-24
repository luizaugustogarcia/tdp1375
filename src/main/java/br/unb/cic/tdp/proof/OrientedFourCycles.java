package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.util.Pair;

import static br.unb.cic.tdp.base.CommonOperations.searchForSorting;

public class OrientedFourCycles {

    public static void main(String[] args) {
        Configuration config = new Configuration("(0 2 1)");
        SortOrExtend.type3Extensions(config)
                .stream()
                .map(Pair::getSecond)
                .forEach(c -> {
                    System.out.println(c + "-" +
                            searchForSorting(null, c, 1.6));
                });
    }
}
