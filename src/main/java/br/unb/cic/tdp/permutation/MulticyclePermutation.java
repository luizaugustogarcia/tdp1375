package br.unb.cic.tdp.permutation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.google.common.collect.Lists;

import cern.colt.list.ByteArrayList;

public class MulticyclePermutation extends ArrayList<Cycle> implements Permutation {

	public MulticyclePermutation() {
	}

	public MulticyclePermutation(String permutation) {
		super();

		ByteArrayList cycle = new ByteArrayList();
		byte symbol = 0;
		for (int i = 0; i < permutation.length(); i++) {
			char current = permutation.charAt(i);
			if (current == '(') {
				continue;
			} else if (current == ')') {
				cycle.add(symbol);
				symbol = 0;
				this.add(new Cycle(cycle));
				cycle = new ByteArrayList();
			} else if (current == ',') {
				cycle.add(symbol);
				symbol = 0;
			} else {
				symbol = (byte) (symbol * 10 + Character.getNumericValue(current));
			}
		}
	}

	public ByteArrayRepresentation byteArrayRepresentation() {
		byte[][] representation = new byte[this.size()][];
		for (int i = 0; i < representation.length; i++) {
			byte[] r = new byte[this.get(i).getSymbols().length];
			for (int j = 0; j < r.length; j++) {
				r[j] = (byte) this.get(i).getSymbols()[j];
			}
			representation[i] = r;
		}

		for (int i = 0; i < representation.length; i++) {
			byte min = representation[i][0];
			int index = 0;
			for (int j = 0; j < representation[i].length; j++) {
				if (representation[i][j] < min) {
					min = representation[i][j];
					index = j;
				}
			}

			byte[] _representation = new byte[representation[i].length];
			System.arraycopy(representation[i], index, _representation, 0, _representation.length - index);
			System.arraycopy(representation[i], 0, _representation, _representation.length - index, index);

			representation[i] = _representation;
		}

		Arrays.sort(representation, (a, b) -> Byte.compare(a[0], b[0]));

		return new ByteArrayRepresentation(representation);
	}

	public MulticyclePermutation(Cycle cycle) {
		super();
		this.add(cycle);
	}

	public MulticyclePermutation(Cycle... cycles) {
		super();
		addAll(Arrays.asList(cycles));
	}

	public MulticyclePermutation(Collection<Cycle> cycles) {
		super();
		addAll(cycles);
	}

	public List<Cycle> getLongCycles() {
		List<Cycle> longCycles = Lists.newArrayList();
		for (Cycle cycle : this) {
			if (cycle.isLong()) {
				longCycles.add(cycle);
			}
		}
		return longCycles;
	}

	public List<Cycle> getBigCycles() {
		List<Cycle> bigCycles = Lists.newArrayList();
		for (Cycle cycle : this) {
			if (cycle.size() >= 4) {
				bigCycles.add(cycle);
			}
		}
		return bigCycles;
	}

	@Override
	public String toString() {
		if (this.isEmpty()) {
			return "()";
		}
		return StringUtils.join(this, "");
	}

	public int getNorm() {
		return this.stream().mapToInt(c -> c.getNorm()).sum();
	}

	@Override
	public MulticyclePermutation getInverse() {
		MulticyclePermutation permutation = new MulticyclePermutation();

		this.stream().forEach((cycle) -> {
			permutation.add(cycle.getInverse());
		});

		return permutation;
	}

	public boolean isNCycle() {
		return this.size() == 1;
	}

	public Cycle asNCycle() {
		if (this.size() > 1) {
			throw new RuntimeException("NONCYCLICPERMUTATION");
		}
		return this.get(0);
	}

	public boolean isIdentity() {
		return this.isEmpty() || (stream().filter((cycle) -> cycle.size() == 1).count() == this.size());
	}

	@Override
	public List<Cycle> default2CycleFactorization() {
		List<Cycle> factorization = new LinkedList<>();

		this.stream().forEach((cycle) -> {
			factorization.addAll(cycle.default2CycleFactorization());
		});

		return factorization;
	}

	public boolean hasLongCycles() {
		return this.stream().anyMatch((cycle) -> cycle.isLong());
	}

	@Override
	public int getNumberOfEvenCycles() {
		return (int) this.stream().filter((cycle) -> cycle.size() % 2 == 1).count();
	}

	@Override
	public int getNumberOfEvenCycles(int n) {
		return (int) this.stream().filter((cycle) -> cycle.size() % 2 == 1).count() + (n - getNumberOfSymbols());
	}

	public int getNumberOfSymbols() {
		return this.stream().mapToInt(c -> c.size()).sum();
	}

	public int getNumberNonEmptyCycles() {
		return (int) stream().filter(cycle -> cycle.size() > 0).count();
	}

	public static class ByteArrayRepresentation implements Serializable {

		private byte[][] representation;

		public ByteArrayRepresentation(byte[][] representation) {
			super();
			this.representation = representation;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			for (byte[] cycle : representation) {
				sb.append("(");
				for (int i = 0; i < cycle.length; i++) {
					sb.append(cycle[i]);
					if (i < cycle.length - 1)
						sb.append(",");
				}
				sb.append(")");
			}
			return sb.toString();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.deepHashCode(representation);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ByteArrayRepresentation other = (ByteArrayRepresentation) obj;
			if (!Arrays.deepEquals(representation, other.representation))
				return false;
			return true;
		}
	}
}
