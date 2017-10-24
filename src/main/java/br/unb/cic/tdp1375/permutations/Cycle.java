package br.unb.cic.tdp1375.permutations;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

import cern.colt.list.ByteArrayList;

public class Cycle implements Permutation, Comparable<Cycle> {

	private byte[] symbols;
	private byte[] symbolIndexes;
	private byte minSymbol = -1;
	private byte maxSymbol = -1;
	private Cycle inverse;
	private int label = -1;

	public Cycle(ByteArrayList lSymbols) {
		byte[] content = new byte[lSymbols.size()];
		System.arraycopy(lSymbols.elements(), 0, content, 0, lSymbols.size());
		this.symbols = content;
		updateIndexes();
	}

	public Cycle(int... symbols) {
		this.symbols = new byte[symbols.length];

		for (int i = 0; i < this.symbols.length; i++)
			this.symbols[i] = (byte) symbols[i];
		updateIndexes();
	}

	public Cycle(byte... symbols) {
		this.symbols = symbols;
		updateIndexes();
	}

	public Cycle(String cycle) {
		this(cycle.replace("(", "").replace(")", "").split(",|\\s"));
	}

	public byte[] getSymbols() {
		return symbols;
	}

	public Cycle(String strSymbols[]) {
		this.symbols = new byte[strSymbols.length];
		for (int i = 0; i < strSymbols.length; i++) {
			String strSymbol = strSymbols[i];
			this.symbols[i] = Byte.parseByte(strSymbol);
		}
		updateIndexes();
	}

	private void updateIndexes() {
		for (int i = 0; i < symbols.length; i++) {
			if (minSymbol == -1 || symbols[i] < minSymbol) {
				minSymbol = symbols[i];
			}
			if (symbols[i] > maxSymbol) {
				maxSymbol = symbols[i];
			}
		}

		symbolIndexes = new byte[maxSymbol + 1];

		Arrays.fill(symbolIndexes, (byte) -1);

		for (int i = 0; i < symbols.length; i++) {
			symbolIndexes[symbols[i]] = (byte) i;
		}
	}

	public byte getMaxSymbol() {
		return maxSymbol;
	}

	public byte getMinSymbol() {
		return minSymbol;
	}

	public void redefine(byte[] symbols) {
		minSymbol = -1;
		maxSymbol = -1;
		this.symbols = symbols;
		updateIndexes();
		inverse = null;
		hashCode = null;
	}

	public void redefine(Cycle cycle) {
		redefine(cycle.symbols);
	}

	public boolean is3Cycle() {
		return size() == 3;
	}

	public boolean is2Cycle() {
		return size() == 2;
	}

	public boolean isLong() {
		return size() > 2;
	}

	@Override
	public String toString() {
		return defaultStringRepresentation();
	}

	@Override
	public Cycle getInverse() {
		if (inverse == null) {
			byte[] symbolsCopy = new byte[this.symbols.length];
			System.arraycopy(this.symbols, 0, symbolsCopy, 0, this.symbols.length);
			ArrayUtils.reverse(symbolsCopy);
			inverse = new Cycle(symbolsCopy);
		}
		return inverse;
	}

	public int getNorm() {
		return this.size() - 1;
	}

	public String defaultStringRepresentation() {
		Cycle _default = this.getStartingBy(minSymbol);

		StringBuilder representation = new StringBuilder().append("(");

		for (int i = 0;; i++) {
			representation.append(_default.symbols[i]);
			if (i == _default.symbols.length - 1)
				break;
			representation.append(",");
		}
		representation.append(")");

		return representation.toString();
	}

	public static String toString(int[] a) {
		int iMax = a.length - 1;
		if (iMax == -1) {
			return "";
		}

		StringBuilder b = new StringBuilder();
		for (int i = 0;; i++) {
			b.append(a[i]);
			if (i == iMax) {
				return b.toString();
			}
			b.append(",");
		}
	}

	private Integer hashCode;
	
	@Override
	public int hashCode() {
		if (hashCode == null)
			hashCode = Arrays.hashCode(getStartingBy(getMinSymbol()).getSymbols());
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (this.symbols.length == 0 && ((Cycle) obj).symbols.length == 0) {
			return true;
		}

		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Cycle other = (Cycle) obj;

		if (size() != other.size()) {
			return false;
		}

		return Arrays.equals(getStartingBy(getMinSymbol()).getSymbols(),
				((Cycle) obj).getStartingBy(((Cycle) obj).getMinSymbol()).getSymbols());
	}

	@Override
	public List<Cycle> default2CycleFactorization() {
		List<Cycle> factorization = new LinkedList<>();

		for (int i = this.size() - 1; i > 0; i--) {
			Cycle factor = new Cycle(this.get(0), this.get(i));
			factorization.add(factor);
		}
		return factorization;
	}

	public boolean isEven() {
		return this.size() % 2 == 1;
	}

	public byte image(byte a) {
		if (this.indexOf(a) == -1) {
			return a;
		}
		return this.get((this.indexOf(a) + 1) % this.size());
	}

	public byte pow(byte a, int power) {
		if (this.indexOf(a) == -1) {
			return a;
		}
		if (power == 0) {
			return a;
		}
		return this.get((((this.indexOf(a) + power) % this.size()) + this.size()) % this.size());
	}

	public int getK(byte a, byte b) {
		if (!contains(a) || !contains(b)) {
			throw new IllegalArgumentException();
		}
		int aIndex = indexOf(a);
		int bIndex = indexOf(b);

		if (bIndex >= aIndex)
			return bIndex - aIndex;

		return (symbols.length - aIndex) + bIndex;
	}

	public Cycle getStartingBy(byte symbol) {
		if (!contains(symbol)) {
			throw new IllegalArgumentException();
		}

		if (get(0) == symbol) {
			return this;
		}

		int index = indexOf(symbol);
		byte[] symbols = new byte[this.symbols.length];
		System.arraycopy(this.symbols, index, symbols, 0, symbols.length - index);
		System.arraycopy(this.symbols, 0, symbols, symbols.length - index, index);
		return new Cycle(symbols);
	}

	@Override
	public int getNumberOfEvenCycles() {
		return size() % 2;
	}

	public Cycle defaultCycleRepresentation() {
		if (size() > 0) {
			return getStartingBy(minSymbol);
		}
		return this;
	}

	@Override
	public int getNumberOfEvenCycles(int n) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public int compareTo(Cycle o) {
		return this.defaultStringRepresentation().compareTo(o.defaultStringRepresentation());
	}

	public byte get(int i) {
		return symbols[i];
	}

	public int indexOf(byte symbol) {
		return symbolIndexes[symbol];
	}

	public boolean contains(byte symbol) {
		return symbol <= symbolIndexes.length - 1 && symbolIndexes[symbol] != -1;
	}

	public boolean isFactor(Cycle cycle) {
		return areSymbolsInCyclicOrder(cycle.symbols);
	}

	public boolean areSymbolsInCyclicOrder(byte... symbols) {
		int firstIndex = -1;
		int lastIndex = -1;
		int state = 0; // 0 - antes de dar uma volta, 1 - acabou de dar uma
						// volta, 2 - após dar a volta
		for (int symbol : symbols) {
			if (state == 0 && symbolIndexes[symbol] < lastIndex) {
				state = 1;
			}
			if (state == 1 || state == 2) {
				if (symbolIndexes[symbol] > firstIndex || (state == 2 && symbolIndexes[symbol] < lastIndex)) {
					return false;
				}
				if (state == 1) {
					state = 2;
				}
			}
			lastIndex = symbolIndexes[symbol];
			if (firstIndex == -1) {
				firstIndex = lastIndex;
			}
		}
		return true;
	}

	@Override
	public int size() {
		return symbols.length;
	}

	public boolean isEmpty() {
		return symbols.length == 0;
	}

	public int getLabel() {
		return label;
	}

	public void setLabel(int label) {
		this.label = label;
	}
}