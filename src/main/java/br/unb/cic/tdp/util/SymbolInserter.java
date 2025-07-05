package br.unb.cic.tdp.util;

import lombok.val;

public final class SymbolInserter {


    public static int[] insertSymbols(final int[] p,
                                      final int[] s,
                                      final int[] symbols) {

        val n = p.length;
        if (n != s.length) {
            throw new IllegalArgumentException("p and s must have the same length");
        }

        var required = 0;
        for (val v : s) {
            if (v < 0) {
                throw new IllegalArgumentException("s[i] must be non-negative");
            }
            required += v;
        }
        if (required != symbols.length) {
            throw new IllegalArgumentException("symbols.length must equal Σ s[i]");
        }

        val out = new int[n + symbols.length];
        var outIdx = 0;
        var symIdx = 0;

        for (var i = 0; i < n; i++) {
            val limit = s[i];
            for (var j = 0; j < limit; j++) {
                out[outIdx++] = symbols[symIdx++];
            }
            out[outIdx++] = p[i];
        }
        return out;
    }

}
