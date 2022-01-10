package br.unb.cic.tdp.permutation;

import cern.colt.list.IntArrayList;
import lombok.Getter;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import org.eclipse.collections.impl.factory.primitive.IntObjectMaps;

import java.util.*;
import java.util.stream.Collectors;

public class MulticyclePermutation implements Collection<Cycle>, Permutation {
    private List<Cycle> cycles = new ArrayList<>();

    @Getter
    private MutableIntObjectMap index = IntObjectMaps.mutable.empty();
    private int numberOfEvenCycles;

    public MulticyclePermutation() {
    }

    public MulticyclePermutation(final String permutation, final boolean include1Cycles) {
        of(permutation);
        if (include1Cycles) {
            for (int i = 0; i <= getMaxSymbol(); i++) {
                if (!getSymbols().contains(i)) {
                    this.add(Cycle.create(i));
                }
            }
        }
    }

    public MulticyclePermutation(final String permutation) {
        of(permutation);
    }

    private void of(String permutation) {
        var symbols = new IntArrayList();
        var cycle = new Cycle();
        int symbol = 0;
        for (var i = 0; i < permutation.length(); i++) {
            final var current = permutation.charAt(i);
            if (current != '(') {
                if (current == ')') {
                    symbols.add(symbol);
                    index.put(symbol, cycle);
                    symbol = 0;
                    symbols.trimToSize();
                    cycle.update(symbols.elements());
                    this.add(cycle);
                    symbols = new IntArrayList();
                    cycle = new Cycle();
                } else if (current == ',' || current == ' ') {
                    symbols.add(symbol);
                    symbol = 0;
                } else {
                    symbol = symbol * 10 + Character.getNumericValue(current);
                    index.put(symbol, cycle);
                }
            }
        }
    }

    public MulticyclePermutation(final Cycle cycle) {
        this.add(cycle);
    }

    public MulticyclePermutation(final Collection<Cycle> cycles) {
        this.addAll(cycles);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MulticyclePermutation cycles1 = (MulticyclePermutation) o;
        return Objects.equals(cycles, cycles1.cycles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cycles);
    }

    @Override
    public String toString() {
        if (this.isEmpty()) {
            return "()";
        }
        return StringUtils.join(this, "");
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
        return cycles.stream().findFirst().get();
    }

    @Override
    public int image(int a) {
        return ((Cycle) index.get(a)).image(a);
    }

    public boolean isIdentity() {
        return this.isEmpty() || (stream().filter((cycle) -> cycle.size() == 1).count() == this.size());
    }

    @Override
    public int getNumberOfEvenCycles() {
        return numberOfEvenCycles;
    }

    public int getNumberOfSymbols() {
        return index.size();
    }

    public MutableIntSet getSymbols() {
        return index.keySet();
    }

    public int get3Norm() {
        return (this.getNumberOfSymbols() - getNumberOfEvenCycles()) / 2;
    }

    public int getMaxSymbol() {
        return getSymbols().max();
    }

    public List<Cycle> getNonTrivialCycles() {
        return this.stream().filter(c -> c.size() > 1).collect(Collectors.toList());
    }

    @Override
    public int size() {
        return cycles.size();
    }

    @Override
    public boolean isEmpty() {
        return cycles.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return cycles.contains(o);
    }

    @Override
    public Iterator<Cycle> iterator() {
        return cycles.iterator();
    }

    @Override
    public Object[] toArray() {
        return cycles.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return cycles.toArray(a);
    }

    @Override
    public boolean add(Cycle cycle) {
        numberOfEvenCycles += cycle.size() % 2;
        Arrays.stream(cycle.getSymbols()).forEach(s -> {
            index.put(s, cycle);
        });
        return cycles.add(cycle);
    }

    @Override
    public boolean addAll(Collection<? extends Cycle> c) {
        c.forEach(cycle -> {
            numberOfEvenCycles += cycle.size() % 2;
            Arrays.stream(cycle.getSymbols()).forEach(s -> {
                index.put(s, cycle);
            });
        });
        return cycles.addAll(c);
    }

    @Override
    public boolean remove(Object o) {
        final var cycle = (Cycle) o;
        if (cycle.isEven()) numberOfEvenCycles--;
        Arrays.stream(cycle.getSymbols()).forEach(index::remove);
        return cycles.remove(o);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        c.forEach(o -> {
            final var cycle = (Cycle) o;
            if (cycle.isEven()) numberOfEvenCycles--;
            Arrays.stream(cycle.getSymbols()).forEach(index::remove);
        });
        return cycles.removeAll(c);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return cycles.containsAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new NotImplementedException();
    }

    @Override
    public void clear() {
        cycles.clear();
    }

    public Cycle getCycle(int symbol) {
        return (Cycle) index.get(symbol);
    }
}
