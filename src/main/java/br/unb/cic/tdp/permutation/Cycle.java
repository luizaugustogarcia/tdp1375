package br.unb.cic.tdp.permutation;

import cern.colt.list.IntArrayList;
import org.apache.commons.lang.ArrayUtils;

import java.util.*;

import static br.unb.cic.tdp.base.CommonOperations.mod;

public class Cycle implements Permutation, Comparable<Cycle> {
    private int[] symbols;
    private int[] symbolIndexes;
    private int minSymbol = -1;
    private int maxSymbol = -1;
    private Cycle inverse;
    private Integer hashCode;

    private Cycle(final int... symbols) {
        this.symbols = symbols;
        updateInternalState();
    }

    public static Cycle create(final String cycle) {
        final var strSymbols = cycle.replace("(", "").replace(")", "").split(",|\\s");
        final var symbols = new int[strSymbols.length];
        for (var i = 0; i < strSymbols.length; i++) {
            final var strSymbol = strSymbols[i];
            symbols[i] = Integer.parseInt(strSymbol);
        }
        return create(symbols);
    }

    public static Cycle create(final IntArrayList lSymbols) {
        final var symbols = new int[lSymbols.size()];
        System.arraycopy(lSymbols.elements(), 0, symbols, 0, lSymbols.size());
        return create(symbols);
    }

    public static Cycle create(final int... symbols) {
        return new Cycle(symbols);
    }

    public int[] getSymbols() {
        return symbols;
    }

    private void updateInternalState() {
        for (int symbol : symbols) {
            if (minSymbol == -1 || symbol < minSymbol) {
                minSymbol = symbol;
            }
            if (symbol > maxSymbol) {
                maxSymbol = symbol;
            }
        }

        symbolIndexes = new int[maxSymbol + 1];

        Arrays.fill(symbolIndexes, (int) -1);

        for (var i = 0; i < symbols.length; i++) {
            symbolIndexes[symbols[i]] = (int) i;
        }
    }

    public int getMaxSymbol() {
        return maxSymbol;
    }

    public int getMinSymbol() {
        return minSymbol;
    }

    @Override
    public String toString() {
        return defaultStringRepresentation();
    }

    @Override
    public Cycle getInverse() {
        if (inverse == null) {
            final var symbolsCopy = new int[this.symbols.length];
            System.arraycopy(this.symbols, 0, symbolsCopy, 0, this.symbols.length);
            ArrayUtils.reverse(symbolsCopy);
            inverse = Cycle.create(symbolsCopy);
        }
        return inverse;
    }

    public int getNorm() {
        return this.size() - 1;
    }

    private String defaultStringRepresentation() {
        final var _default = this.startingBy(minSymbol);

        final var representation = new StringBuilder().append("(");

        for (var i = 0; ; i++) {
            representation.append(_default.symbols[i]);
            if (i == _default.symbols.length - 1)
                break;
            representation.append(" ");
        }
        representation.append(")");

        return representation.toString();
    }

    @Override
    public int hashCode() {
        if (hashCode == null) {
            hashCode = Arrays.hashCode(startingBy(getMinSymbol()).getSymbols());
        }
        return hashCode;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final var other = (Cycle) obj;

        if (size() != other.size()) {
            return false;
        }

        return Arrays.equals(startingBy(getMinSymbol()).getSymbols(),
                ((Cycle) obj).startingBy(((Cycle) obj).getMinSymbol()).getSymbols());
    }

    public boolean isEven() {
        return this.size() % 2 == 1;
    }

    public int image(final int a) {
        return symbols[(symbolIndexes[a] + 1) % symbols.length];
    }

    public int pow(final int a, final int power) {
        return symbols[mod(symbolIndexes[a] + power, symbols.length)];
    }

    public int getK(final int a, final int b) {
        final var aIndex = indexOf(a);
        final var bIndex = indexOf(b);

        if (bIndex >= aIndex)
            return bIndex - aIndex;

        return (symbols.length - aIndex) + bIndex;
    }

    public Cycle startingBy(final int symbol) {
        if (this.symbols[0] == symbol) {
            return this;
        }

        final var index = indexOf(symbol);
        final var symbols = new int[this.symbols.length];
        System.arraycopy(this.symbols, index, symbols, 0, symbols.length - index);
        System.arraycopy(this.symbols, 0, symbols, symbols.length - index, index);

        return Cycle.create(symbols);
    }

    @Override
    public int getNumberOfEvenCycles() {
        return size() % 2;
    }

    @Override
    public int compareTo(final Cycle o) {
        return this.defaultStringRepresentation().compareTo(o.defaultStringRepresentation());
    }

    public int get(final int i) {
        return symbols[i];
    }

    public int indexOf(final int symbol) {
        return symbolIndexes[symbol];
    }

    public boolean contains(final int symbol) {
        return symbol <= symbolIndexes.length - 1 && symbolIndexes[symbol] != -1;
    }

    @Override
    public int size() {
        return symbols.length;
    }

    @Override
    public Cycle asNCycle() {
        return this;
    }

    public boolean isLong() {
        return this.size() > 3;
    }

    public int[] getSymbolIndexes() {
        return symbolIndexes;
    }
}