package br.unb.cic.tdp.proof.util;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SearchParams {
    public final int[] move;
    public final ListOfCycles spi;
    public final boolean[] parity;
    public final int[][] spiIndex;
    public final int[] pi;
}