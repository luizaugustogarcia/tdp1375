package br.unb.cic.tdp.permutation;

import cern.colt.list.ByteArrayList;
import com.google.common.collect.Maps;
import com.google.common.primitives.Bytes;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class MulticyclePermutation extends ArrayList<Cycle> implements Permutation {

    public MulticyclePermutation() {
    }

    public MulticyclePermutation(final String permutation) {
        var cycle = new ByteArrayList();
        byte symbol = 0;
        for (var i = 0; i < permutation.length(); i++) {
            final var current = permutation.charAt(i);
            if (current != '(') {
                if (current == ')') {
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
    }

    public MulticyclePermutation(final Cycle cycle) {
        this.add(cycle);
    }

    public MulticyclePermutation(final Collection<Cycle> cycles) {
        addAll(cycles);
    }

    @Override
    public String toString() {
        if (this.isEmpty()) {
            return "()";
        }
        return StringUtils.join(this, "");
    }

    public int getNorm() {
        return this.stream().mapToInt(Cycle::getNorm).sum();
    }

    @Override
    public MulticyclePermutation getInverse() {
        final var permutation = new MulticyclePermutation();

        this.forEach((cycle) -> permutation.add(cycle.getInverse()));

        return permutation;
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
    public int getNumberOfEvenCycles() {
        return (int) this.stream().filter((cycle) -> cycle.size() % 2 == 1).count();
    }

    public int getNumberOfSymbols() {
        return this.stream().mapToInt(Cycle::size).sum();
    }

    public List<Byte> getSymbols() {
        return this.stream().flatMap(cycle -> Bytes.asList(cycle.getSymbols()).stream()).collect(Collectors.toList());
    }

    public boolean isSameCycleType(final MulticyclePermutation other) {
        if (this.size() != other.size()) {
            return false;
        }

        final var difference = Maps.difference(
                this.stream().collect(Collectors.groupingBy(Cycle::size, Collectors.counting())),
                other.stream().collect(Collectors.groupingBy(Cycle::size, Collectors.counting())));

        return difference.areEqual();
    }

    public int get3Norm() {
        return (this.getNumberOfSymbols() - getNumberOfEvenCycles()) / 2;
    }
}
