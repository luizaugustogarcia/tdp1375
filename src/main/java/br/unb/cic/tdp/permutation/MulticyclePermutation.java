package br.unb.cic.tdp.permutation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Bytes;

import cern.colt.list.ByteArrayList;

public class MulticyclePermutation extends ArrayList<Cycle> implements Permutation {

	private static final long serialVersionUID = -249634481357599063L;

	public MulticyclePermutation() {
	}

	public MulticyclePermutation(final String permutation) {
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

	public MulticyclePermutation(Cycle cycle) {
		this.add(cycle);
	}

	public MulticyclePermutation(Collection<Cycle> cycles) {
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

	public List<Byte> getSymbols() {
		return this.stream().flatMap(cycle -> Bytes.asList(cycle.getSymbols()).stream()).collect(Collectors.toList());
	}

	public boolean isSameCycleType(final MulticyclePermutation other) {
		if (this.size() != other.size()) {
			return false;
		}

		final var difference = Maps.difference(
				this.stream().collect(Collectors.groupingBy(c -> c.size(), Collectors.counting())),
				other.stream().collect(Collectors.groupingBy(c -> c.size(), Collectors.counting())));

		return difference.areEqual();
	}
}
