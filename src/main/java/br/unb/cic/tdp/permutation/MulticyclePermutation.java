package br.unb.cic.tdp.permutation;

import lombok.Getter;
import lombok.val;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MulticyclePermutation implements Collection<Cycle>, Permutation {

    private final List<Cycle> cycles = new ArrayList<>();

    @Getter
    private final Set<Integer> symbols = new HashSet<>();

    private int numberOfEvenCycles;

    public MulticyclePermutation() {
    }

    public MulticyclePermutation(final String permutation, final boolean include1Cycles) {
        of(permutation);
        if (include1Cycles) {
            for (var i = 0; i <= getMaxSymbol(); i++) {
                if (!getSymbols().contains(i)) {
                    this.add(Cycle.of(i));
                }
            }
        }
    }

    public MulticyclePermutation(final String permutation) {
        of(permutation);
    }

    private void of(String permutation) {
        val cyclePattern = Pattern.compile("\\(([^\\(\\)]*?)\\)");
        if (!permutation.contains("(")) {
            this.add(Cycle.of(permutation));
        } else {
            val matcher= cyclePattern.matcher(permutation);
            while (matcher.find()) {
                this.add(Cycle.of(matcher.group(1)));
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
        return Objects.equals(cycles, ((MulticyclePermutation) o).cycles);
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
        val permutation = new MulticyclePermutation();

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
    public int image(final int a) {
        for (val cycle: cycles) {
            if (cycle.contains(a)) {
                return cycle.image(a);
            }
        }
        return a;
    }

    public boolean isIdentity() {
        return this.isEmpty() || (stream().filter((cycle) -> cycle.size() == 1).count() == this.size());
    }

    @Override
    public int getNumberOfEvenCycles() {
        return numberOfEvenCycles;
    }

    public int getNumberOfSymbols() {
        return symbols.size();
    }

    public Set<Integer> getSymbols() {
        return symbols;
    }

    public int get3Norm() {
        return (this.getNumberOfSymbols() - getNumberOfEvenCycles()) / 2;
    }

    public int getMaxSymbol() {
        return getSymbols().stream().max(Comparator.comparing(Function.identity())).orElse(-1);
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
    public boolean contains(final Object o) {
        return symbols.contains(o);
    }

    @Override
    public boolean contains(final int o) {
        return symbols.contains(o);
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
    public boolean add(final Cycle cycle) {
        numberOfEvenCycles += cycle.size() % 2;
        val symbols = cycle.getSymbols();
        for (int s : symbols) {
            this.symbols.add(s);
        }
        return cycles.add(cycle);
    }

    @Override
    public boolean addAll(final Collection<? extends Cycle> c) {
        c.forEach(this::add);
        return true;
    }

    @Override
    public boolean remove(final Object o) {
        val cycle = (Cycle) o;
        if (cycle.isEven()) numberOfEvenCycles--;
        Arrays.stream(cycle.getSymbols()).forEach(symbols::remove);
        return cycles.remove(o);
    }

    @Override
    public boolean removeAll(final Collection<?> c) {
        c.forEach(o -> {
            val cycle = (Cycle) o;
            if (cycle.isEven()) numberOfEvenCycles--;
            Arrays.stream(cycle.getSymbols()).forEach(symbols::remove);
        });
        return cycles.removeAll(c);
    }

    @Override
    public boolean containsAll(final Collection<?> c) {
        return new HashSet<>(cycles).containsAll(c);
    }

    @Override
    public boolean retainAll(final Collection<?> c) {
        throw new NotImplementedException();
    }

    @Override
    public void clear() {
        cycles.clear();
    }
}
