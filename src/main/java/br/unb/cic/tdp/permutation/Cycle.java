package br.unb.cic.tdp.permutation;

import cern.colt.list.ByteArrayList;
import org.apache.commons.lang.ArrayUtils;

import java.util.Arrays;

import static br.unb.cic.tdp.base.CommonOperations.areSymbolsInCyclicOrder;

public class Cycle implements Permutation, Comparable<Cycle> {

    private byte[] symbols;
    private byte[] symbolIndexes;
    private byte minSymbol = -1;
    private byte maxSymbol = -1;
    private Cycle inverse;
    private Integer hashCode;

    public Cycle(final ByteArrayList lSymbols) {
        final var content = new byte[lSymbols.size()];
        System.arraycopy(lSymbols.elements(), 0, content, 0, lSymbols.size());
        this.symbols = content;
        updateIndexes();
    }

    public Cycle(final int... symbols) {
        this.symbols = new byte[symbols.length];

        for (var i = 0; i < this.symbols.length; i++)
            this.symbols[i] = (byte) symbols[i];
        updateIndexes();
    }

    public Cycle(final byte... symbols) {
        this.symbols = symbols;
        updateIndexes();
    }

    public Cycle(final String cycle) {
        this(cycle.replace("(", "").replace(")", "").split(",|\\s"));
    }

    public Cycle(final String[] strSymbols) {
        this.symbols = new byte[strSymbols.length];
        for (var i = 0; i < strSymbols.length; i++) {
            final var strSymbol = strSymbols[i];
            this.symbols[i] = Byte.parseByte(strSymbol);
        }
        updateIndexes();
    }

    public byte[] getSymbols() {
        return symbols;
    }

    private void updateIndexes() {
        for (byte symbol : symbols) {
            if (minSymbol == -1 || symbol < minSymbol) {
                minSymbol = symbol;
            }
            if (symbol > maxSymbol) {
                maxSymbol = symbol;
            }
        }

        symbolIndexes = new byte[maxSymbol + 1];

        Arrays.fill(symbolIndexes, (byte) -1);

        for (var i = 0; i < symbols.length; i++) {
            symbolIndexes[symbols[i]] = (byte) i;
        }
    }

    public byte getMaxSymbol() {
        return maxSymbol;
    }

    public byte getMinSymbol() {
        return minSymbol;
    }

    public void redefine(final byte[] symbols) {
        minSymbol = -1;
        maxSymbol = -1;
        this.symbols = symbols;
        updateIndexes();
        inverse = null;
        hashCode = null;
    }

    @Override
    public String toString() {
        return defaultStringRepresentation();
    }

    @Override
    public Cycle getInverse() {
        if (inverse == null) {
            final var symbolsCopy = new byte[this.symbols.length];
            System.arraycopy(this.symbols, 0, symbolsCopy, 0, this.symbols.length);
            ArrayUtils.reverse(symbolsCopy);
            inverse = new Cycle(symbolsCopy);
        }
        return inverse;
    }

    public int getNorm() {
        return this.size() - 1;
    }

    private String defaultStringRepresentation() {
        final var _default = this.getStartingBy(minSymbol);

        final var representation = new StringBuilder().append("(");

        for (var i = 0; ; i++) {
            representation.append(_default.symbols[i]);
            if (i == _default.symbols.length - 1)
                break;
            representation.append(",");
        }
        representation.append(")");

        return representation.toString();
    }

    @Override
    public int hashCode() {
        if (hashCode == null)
            hashCode = Arrays.hashCode(getStartingBy(getMinSymbol()).getSymbols());
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

        return Arrays.equals(getStartingBy(getMinSymbol()).getSymbols(),
                ((Cycle) obj).getStartingBy(((Cycle) obj).getMinSymbol()).getSymbols());
    }

    public boolean isEven() {
        return this.size() % 2 == 1;
    }

    public byte image(final byte a) {
        return symbols[(symbolIndexes[a] + 1) % symbols.length];
    }

    public byte pow(final byte a, final int power) {
        return symbols[(symbolIndexes[a] + power) % symbols.length];
    }

    public int getK(final byte a, final byte b) {
        final var aIndex = indexOf(a);
        final var bIndex = indexOf(b);

        if (bIndex >= aIndex)
            return bIndex - aIndex;

        return (symbols.length - aIndex) + bIndex;
    }

    public Cycle getStartingBy(final byte symbol) {
        if (this.symbols[0] == symbol) {
            return this;
        }

        final var index = indexOf(symbol);
        final var symbols = new byte[this.symbols.length];
        System.arraycopy(this.symbols, index, symbols, 0, symbols.length - index);
        System.arraycopy(this.symbols, 0, symbols, symbols.length - index, index);

        return new Cycle(symbols);
    }

    @Override
    public int getNumberOfEvenCycles() {
        return size() % 2;
    }

    @Override
    public int compareTo(final Cycle o) {
        return this.defaultStringRepresentation().compareTo(o.defaultStringRepresentation());
    }

    public byte get(final int i) {
        return symbols[i];
    }

    public int indexOf(final byte symbol) {
        return symbolIndexes[symbol];
    }

    public boolean contains(final byte symbol) {
        return symbol <= symbolIndexes.length - 1 && symbolIndexes[symbol] != -1;
    }

    public boolean isApplicable(final Cycle rho) {
        return areSymbolsInCyclicOrder(rho.getSymbols(), this.getSymbols());
    }

    @Deprecated
    public boolean isOriented(final byte... symbols) {
        boolean leap = false;
        for (int i = 0; i < symbols.length; i++) {
            if (symbolIndexes[symbols[i]] > symbolIndexes[symbols[(i + 1) % symbols.length]]) {
                if (!leap) {
                    leap = true;
                } else {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public int size() {
        return symbols.length;
    }

    @Override
    public Cycle asNCycle() {
        return this;
    }
}