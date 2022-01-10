package br.unb.cic.tdp;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.proof.ProofGenerator;

public class Teste {
    public static void main(String[] args) {
        final var c = new Configuration("(4 8 6)(10 20 12)(21 25 23)(22 26 24)(7 29 27)(9 19 17)(11 15 13)(28 32 30)(14 18 16)(31 35 33)(1 5 3)(0 34 2)");
        System.out.println(ProofGenerator.searchForSorting(c));
    }
}
