package br.unb.cic.tdp.proof.util;

import br.unb.cic.tdp.util.Triplet;
import com.google.common.primitives.Ints;
import lombok.SneakyThrows;
import lombok.val;

import java.util.Arrays;

public class SequenceSearcher {

    @SneakyThrows
    public ListOfCycles search(final ListOfCycles spi,
                               final boolean[] parity,
                               final int[][] spiIndex,
                               final int maxSymbol,
                               final int[] pi,
                               final MovesStack moves,
                               final MoveTreeNode root) {
        if (root.mu == 0) {
            val sorting = analyze0Moves(spi, parity, spiIndex, maxSymbol, pi, moves, root);
            if (!sorting.isEmpty()) {
                return sorting;
            }
        } else {
            var sorting = analyzeOrientedCycles(spi, parity, spiIndex, maxSymbol, pi, moves, root);
            if (!sorting.isEmpty()) {
                return sorting;
            }

            sorting = analyzeOddCycles(spi, parity, spiIndex, maxSymbol, pi, moves, root);
            if (!sorting.isEmpty()) {
                return sorting;
            }
        }

        return ListOfCycles.EMPTY_LIST;
    }

    private ListOfCycles analyzeOddCycles(final ListOfCycles spi,
                                          final boolean[] parity,
                                          final int[][] spiIndex,
                                          final int maxSymbol,
                                          final int[] pi,
                                          final MovesStack moves,
                                          final MoveTreeNode root) {
        for (int i = 0; i < pi.length - 2; i++) {
            if (parity[pi[i]]) continue;
            for (int j = i + 1; j < pi.length - 1; j++) {
                if (parity[pi[j]]) continue;
                for (int k = j + 1; k < pi.length; k++) {
                    if (parity[pi[k]]) continue;

                    int a = pi[i], b = pi[j], c = pi[k];

                    // if it's the same cycle, skip it
                    if (spiIndex[a] == spiIndex[b] && spiIndex[b] == spiIndex[c])
                        continue;

                    val is_2Move = spiIndex[a] != spiIndex[b] &&
                            spiIndex[b] != spiIndex[c] &&
                            spiIndex[a] != spiIndex[c];
                    if (is_2Move)
                        continue;

                    final Triplet<ListOfCycles, ListOfCycles, Integer> triplet = simulate0MoveTwoCycles(spiIndex, a, b, c);

                    if (triplet.third != 2)
                        continue;

                    moves.push(a, b, c);

                    // == APPLY THE MOVE ===
                    spi.removeAll(triplet.first);
                    var numberOfTrivialCycles = 0;
                    spi.removeAll(triplet.first);

                    var current = triplet.second.head;
                    for (int l = 0; l < triplet.second.size; l++) {
                        val cycle = current.data;

                        if (cycle.length > 1) {
                            spi.add(cycle);
                        } else {
                            numberOfTrivialCycles++;
                        }
                        current = current.next;
                    }

                    updateIndex(spiIndex, parity, triplet.second);
                    // ==============================

                    if (root.children.length == 0) {
                        return moves.toListOfCycles();
                    } else {
                        for (val m : root.children) {
                            int[] newPi = applyTransposition(pi, a, b, c, pi.length - numberOfTrivialCycles, spiIndex);
                            val sorting = search(spi, parity, spiIndex, maxSymbol, newPi, moves, m);
                            if (!sorting.isEmpty()) {
                                return moves.toListOfCycles();
                            }
                        }
                    }

                    // ==== ROLLBACK ====
                    current = triplet.second.head;
                    for (int l = 0; l < triplet.second.size; l++) {
                        val cycle = current.data;
                        if (cycle.length > 1) spi.remove(cycle);
                        current = current.next;
                    }
                    spi.addAll(triplet.first);
                    updateIndex(spiIndex, parity, triplet.first);
                    // ==============================

                    moves.pop();
                }
            }
        }

        return ListOfCycles.EMPTY_LIST;
    }

    private ListOfCycles analyzeOrientedCycles(final ListOfCycles spi,
                                               final boolean[] parity,
                                               final int[][] spiIndex,
                                               final int maxSymbol,
                                               final int[] pi,
                                               final MovesStack moves,
                                               final MoveTreeNode root) {
        val piInverseIndex = getPiInverseIndex(pi, maxSymbol);

        val orientedCycles = getOrientedCycles(spi, piInverseIndex);

        var current = orientedCycles.head;
        for (int l = 0; l < orientedCycles.size; l++) {
            val cycle = current.data;

            val before = parity[cycle[0]] ? 1 : 0;

            for (var i = 0; i < cycle.length - 2; i++) {
                for (var j = i + 1; j < cycle.length - 1; j++) {
                    val ab_k = j - i;

                    if (before == 1 && (ab_k & 1) == 0) {
                        continue;
                    }

                    for (var k = j + 1; k < cycle.length; k++) {
                        val bc_k = k - j;

                        if (before == 1 && (bc_k & 1) == 0) {
                            continue;
                        }

                        val ca_k = (cycle.length - k) + i;

                        int a = cycle[i], b = cycle[j], c = cycle[k];

                        var after = ab_k & 1;
                        after += bc_k & 1;
                        after += ca_k & 1;

                        // check if it's applicable
                        if (after - before == 2 && areSymbolsInCyclicOrder(piInverseIndex, a, c, b)) {
                            final int[] symbols = startingBy(cycle, a);
                            val aCycle = new int[ca_k];
                            aCycle[0] = a;
                            System.arraycopy(symbols, ab_k + bc_k + 1, aCycle, 1, ca_k - 1);

                            val bCycle = new int[ab_k];
                            bCycle[0] = b;
                            System.arraycopy(symbols, 1, bCycle, 1, ab_k - 1);

                            val cCycle = new int[bc_k];
                            cCycle[0] = c;
                            System.arraycopy(symbols, ab_k + 1, cCycle, 1, bc_k - 1);

                            moves.push(a, b, c);

                            // == APPLY THE MOVE ===
                            spi.remove(cycle);
                            var numberOfTrivialCycles = 0;
                            if (aCycle.length > 1) spi.add(aCycle); else numberOfTrivialCycles++;
                            if (bCycle.length > 1) spi.add(bCycle); else numberOfTrivialCycles++;
                            if (cCycle.length > 1) spi.add(cCycle); else numberOfTrivialCycles++;
                            update(spiIndex, parity, aCycle, bCycle, cCycle);
                            // =======================

                            if (root.children.length == 0) {
                                return moves.toListOfCycles();
                            } else {
                                for (val m : root.children) {
                                    int[] newPi = applyTransposition(pi, a, b, c, pi.length - numberOfTrivialCycles, spiIndex);
                                    val sorting = search(spi, parity, spiIndex, maxSymbol, newPi, moves, m);
                                    if (!sorting.isEmpty()) {
                                        return moves.toListOfCycles();
                                    }
                                }
                            }

                            moves.pop();

                            // ==== ROLLBACK ====
                            if (aCycle.length > 1) spi.remove(aCycle);
                            if (bCycle.length > 1) spi.remove(bCycle);
                            if (cCycle.length > 1) spi.remove(cCycle);
                            spi.add(cycle);
                            update(spiIndex, parity, cycle);
                            // ====================
                        }
                    }
                }
            }
            current = current.next;
        }

        return ListOfCycles.EMPTY_LIST;
    }

    private static boolean areSymbolsInCyclicOrder(final int[] piInverseIndex, final int a, final int b, final int c) {
        return (piInverseIndex[a] < piInverseIndex[b] && piInverseIndex[b] < piInverseIndex[c]) ||
                (piInverseIndex[b] < piInverseIndex[c] && piInverseIndex[c] < piInverseIndex[a]) ||
                (piInverseIndex[c] < piInverseIndex[a] && piInverseIndex[a] < piInverseIndex[b]);
    }

    private ListOfCycles analyze0Moves(final ListOfCycles spi,
                                       final boolean[] parity,
                                       final int[][] spiIndex,
                                       final int maxSymbol,
                                       final int[] pi,
                                       final MovesStack moves,
                                       final MoveTreeNode root) {
        val cycleIndexes = new int[maxSymbol + 1][];

        for (int i = 0; i < pi.length - 2; i++) {
            for (int j = i + 1; j < pi.length - 1; j++) {
                for (int k = j + 1; k < pi.length; k++) {

                    int a = pi[i], b = pi[j], c = pi[k];

                    val is_2Move = spiIndex[a] != spiIndex[b] &&
                            spiIndex[b] != spiIndex[c] &&
                            spiIndex[a] != spiIndex[c];
                    if (is_2Move)
                        continue;

                    final Triplet<ListOfCycles, ListOfCycles, Integer> triplet;
                    // if it's the same cycle
                    if (spiIndex[a] == spiIndex[b] && spiIndex[b] == spiIndex[c]) {
                        val cycle = spiIndex[a];

                        if (cycleIndexes[a] == null) {
                            val index = cycleIndex(cycle);
                            cycleIndexes[a] = index;
                            cycleIndexes[b] = index;
                            cycleIndexes[c] = index;
                        }

                        val index = cycleIndexes[a];

                        if (areSymbolsInCyclicOrder(index, a, b, c)) {
                            val before = cycle.length & 1;

                            val ab_k = getK(index, cycle, a, b);
                            var after = ab_k & 1;
                            val bc_k = getK(index, cycle, b, c);
                            after += bc_k & 1;
                            val ca_k = getK(index, cycle, c, a);
                            after += ca_k & 1;

                            if (after - before == 2) {
                                // skip, it's a 2-move
                                continue;
                            }

                            after = 0;
                            final int[] symbols = startingBy(cycle, a);
                            val aCycle = new int[ca_k];
                            aCycle[0] = a;
                            System.arraycopy(symbols, ab_k + bc_k + 1, aCycle, 1, ca_k - 1);
                            after += aCycle.length & 1;

                            val bCycle = new int[ab_k];
                            bCycle[0] = b;
                            System.arraycopy(symbols, 1, bCycle, 1, ab_k - 1);
                            after += bCycle.length & 1;

                            val cCycle = new int[bc_k];
                            cCycle[0] = c;
                            System.arraycopy(symbols, ab_k + 1, cCycle, 1, bc_k - 1);
                            after += cCycle.length & 1;

                            triplet = new Triplet<>(ListOfCycles.singleton(cycle), ListOfCycles.asList(aCycle, bCycle, cCycle), after - before);
                        } else {
                            triplet = simulate0MoveSameCycle(spiIndex, a, b, c);
                        }
                    } else {
                        triplet = simulate0MoveTwoCycles(spiIndex, a, b, c);
                    }

                    if (triplet.third != 0)
                        continue;

                    moves.push(a, b, c);

                    // == APPLY THE MOVE ===
                    var numberOfTrivialCycles = 0;
                    spi.removeAll(triplet.first);

                    var current = triplet.second.head;
                    for (int l = 0; l < triplet.second.size; l++) {
                        val cycle = current.data;

                        if (cycle.length > 1) {
                            spi.add(cycle);
                        } else {
                            numberOfTrivialCycles++;
                        }
                        current = current.next;
                    }
                    updateIndex(spiIndex, parity, triplet.second);
                    // ==============================

                    if (root.children.length == 0) {
                        return moves.toListOfCycles();
                    } else {
                        for (val m : root.children) {
                            int[] newPi = applyTransposition(pi, a, b, c, pi.length - numberOfTrivialCycles, spiIndex);
                            val sorting = search(spi, parity, spiIndex, maxSymbol, newPi, moves, m);
                            if (!sorting.isEmpty()) {
                                return moves.toListOfCycles();
                            }
                        }
                    }

                    // ==== ROLLBACK ====
                    current = triplet.second.head;
                    for (int l = 0; l < triplet.second.size; l++) {
                        val cycle = current.data;
                        if (cycle.length > 1) spi.remove(cycle);
                        current = current.next;
                    }
                    spi.addAll(triplet.first);
                    updateIndex(spiIndex, parity, triplet.first);
                    // ==============================

                    moves.pop();
                }
            }
        }

        return ListOfCycles.EMPTY_LIST;
    }

    private static void update(final int[][] index, final boolean[] parity, final int[]... cycles) {
        for (int[] cycle : cycles) {
            val p = (cycle.length & 1) == 1;
            for (int k : cycle) {
                index[k] = cycle;
                parity[k] = p;
            }
        }
    }

    private static int[] getPiInverseIndex(final int[] pi, final int maxSymbol) {
        val piInverseIndex = new int[maxSymbol + 1];
        for (var i = 0; i < pi.length; i++) {
            piInverseIndex[pi[pi.length - i - 1]] = i;
        }
        return piInverseIndex;
    }

    private static ListOfCycles getOrientedCycles(final ListOfCycles spi, final int[] piInverseIndex) {
        val orientedCycles = new ListOfCycles();
        var current = spi.head;
        for (int i = 0; i < spi.size; i++) {
            final int[] cycle = current.data;
            if (!areSymbolsInCyclicOrder(piInverseIndex, cycle))
                orientedCycles.add(cycle);
            current = current.next;
        }
        return orientedCycles;
    }

    private static void updateIndex(final int[][] index, final boolean[] parity, final ListOfCycles cycles) {
        for (var current = cycles.head; current != null; current = current.next) {
            val cycle = current.data;
            updateIndex(index, parity, cycle);
        }
    }

    private static void updateIndex(final int[][] index, final boolean[] parity, final int[]... cycles) {
        for (int[] cycle : cycles) {
            val p = (cycle.length & 1) == 1;
            for (int k : cycle) {
                index[k] = cycle;
                parity[k] = p;
            }
        }
    }

    private static boolean areSymbolsInCyclicOrder(final int[] index, int[] symbols) {
        boolean leap = false;
        for (int i = 0; i < symbols.length; i++) {
            int nextIndex = i + 1;
            if (nextIndex >= symbols.length)
                nextIndex = (i + 1) % symbols.length;
            if (index[symbols[i]] > index[symbols[nextIndex]]) {
                if (!leap) {
                    leap = true;
                } else {
                    return false;
                }
            }
        }

        return true;
    }

    private static Triplet<ListOfCycles, ListOfCycles, Integer> simulate0MoveTwoCycles(final int[][] spiIndex,
                                                                                       final int a,
                                                                                       final int b,
                                                                                       final int c) {
        int numberOfEvenCycles = 0;
        int aPrime, bPrime, cPrime;
        if (spiIndex[a] == spiIndex[c]) {
            aPrime = a;
            bPrime = c;
            cPrime = b;
            numberOfEvenCycles += spiIndex[a].length & 1;
            numberOfEvenCycles += spiIndex[b].length & 1;
        } else if (spiIndex[a] == spiIndex[b]) {
            aPrime = b;
            bPrime = a;
            cPrime = c;
            numberOfEvenCycles += spiIndex[a].length & 1;
            numberOfEvenCycles += spiIndex[c].length & 1;
        } else {
            // spi.getCycle(b) == spi.getCycle(c)
            aPrime = c;
            bPrime = b;
            cPrime = a;
            numberOfEvenCycles += spiIndex[a].length & 1;
            numberOfEvenCycles += spiIndex[c].length & 1;
        }

        val index = cycleIndex(spiIndex[cPrime]);
        val cImage = image(index, spiIndex[cPrime], cPrime);
        val abCycle = startingBy(spiIndex[aPrime], aPrime);
        val cCycle = startingBy(spiIndex[cPrime], cImage);

        val abCycleIndex = cycleIndex(abCycle);

        val ba_k = getK(abCycleIndex, abCycle, bPrime, aPrime);
        val newaCycle = new int[1 + ba_k - 1];
        newaCycle[0] = aPrime;
        val ab_k = getK(abCycleIndex, abCycle, aPrime, bPrime);
        System.arraycopy(abCycle,  ab_k + 1, newaCycle, 1, ba_k - 1);

        val newbCycle = new int[1 + cCycle.length + (ab_k - 1)];
        newbCycle[0] = bPrime;
        System.arraycopy(cCycle, 0, newbCycle, 1, cCycle.length);
        System.arraycopy(abCycle, 1, newbCycle, 1 + cCycle.length, ab_k - 1);

        var newNumberOfEvenCycles = 0;
        newNumberOfEvenCycles += newaCycle.length & 1;
        newNumberOfEvenCycles += newbCycle.length & 1;

        val oldCycles = new ListOfCycles();
        oldCycles.add(spiIndex[a]);
        if (!oldCycles.contains(spiIndex[b]))
            oldCycles.add(spiIndex[b]);
        if (!oldCycles.contains(spiIndex[c]))
            oldCycles.add(spiIndex[c]);

        val newCycles = new ListOfCycles();
        newCycles.add(newaCycle);
        newCycles.add(newbCycle);

        return new Triplet<>(oldCycles, newCycles, newNumberOfEvenCycles - numberOfEvenCycles);
    }

    private static int image(int[] index, int[] cycle, int a) {
        return cycle[(index[a] + 1) % cycle.length];
    }

    private static Triplet<ListOfCycles, ListOfCycles, Integer> simulate0MoveSameCycle(final int[][] cycleIndex,
                                                                                       final int a,
                                                                                       final int b,
                                                                                       final int c) {
        val oldCycle = cycleIndex[a];

        final int[] symbols = startingBy(oldCycle, b);
        val newCycle = new int[oldCycle.length];

        final int[] oldCycleIndex = cycleIndex(oldCycle);

        newCycle[0] = b;
        val ab_k = getK(oldCycleIndex, oldCycle, b, a);
        val bc_k = getK(oldCycleIndex, oldCycle, a, c);
        System.arraycopy(symbols, ab_k + 1, newCycle, 1, bc_k - 1);
        newCycle[bc_k] = c;

        System.arraycopy(symbols, 1, newCycle, 1 + bc_k, ab_k - 1);
        newCycle[ab_k + bc_k] = a;

        val ca_k = getK(oldCycleIndex, oldCycle, c, b);
        System.arraycopy(symbols, ab_k + bc_k + 1,
                newCycle, ab_k + bc_k + 1, ca_k - 1);

        return new Triplet<>(ListOfCycles.singleton(oldCycle), ListOfCycles.singleton(newCycle), 0);
    }

    private static int[] cycleIndex(int[] cycle) {
        val index = new int[Ints.max(cycle) + 1];

        for (int i = 0; i < cycle.length; i++) {
            index[cycle[i]] = i;
        }

        return index;
    }

    private static int getK(int[] cycleIndex, int[] cycle, int a, int b) {
        val aIndex = cycleIndex[a];
        val bIndex = cycleIndex[b];

        if (bIndex >= aIndex)
            return bIndex - aIndex;

        return (cycle.length - aIndex) + bIndex;
    }

    private static int[] startingBy(int[] symbols, int a) {
        if (symbols[0] == a)
            return symbols;

        val result = new int[symbols.length];
        for (int i = 0; i < symbols.length; i++) {
            if (symbols[i] == a) {
                System.arraycopy(symbols, i, result, 0, symbols.length - i);
                System.arraycopy(symbols, 0, result, symbols.length - i, i);
                break;
            }
        }

        return result;
    }

    private static int[] applyTransposition(final int[] pi,
                                            final int a,
                                            final int b,
                                            final int c,
                                            int numberOfSymbols, int[][] spiIndex) {
        val indexes = new int[3];
        Arrays.fill(indexes, -1);

        for (var i = 0; i < pi.length; i++) {
            if (pi[i] == a)
                indexes[0] = i;
            if (pi[i] == b)
                indexes[1] = i;
            if (pi[i] == c)
                indexes[2] = i;

            if (indexes[0] != -1 && indexes[1] != -1 && indexes[2] != -1)
                break;
        }

        // sort indexes - this is CPU efficient
        if (indexes[0] > indexes[2]) {
            val temp = indexes[0];
            indexes[0] = indexes[2];
            indexes[2] = temp;
        }

        if (indexes[0] > indexes[1]) {
            val temp = indexes[0];
            indexes[0] = indexes[1];
            indexes[1] = temp;
        }

        if (indexes[1] > indexes[2]) {
            val temp = indexes[1];
            indexes[1] = indexes[2];
            indexes[2] = temp;
        }

        val result = new int[numberOfSymbols];

        int counter = 0;
        for (int i = 0; i < indexes[0]; i++) {
            if (spiIndex[pi[i]].length == 1) continue;
            result[counter] = pi[i];
            counter++;
        }

        for (int i = 0; i < indexes[2] - indexes[1]; i++) {
            if (spiIndex[pi[indexes[1] + i]].length == 1) continue;
            result[counter] = pi[indexes[1] + i];
            counter++;
        }

        for (int i = 0; i < indexes[1] - indexes[0]; i++) {
            if (spiIndex[pi[indexes[0] + i]].length == 1) continue;
            result[counter] = pi[indexes[0] + i];
            counter++;
        }

        for (int i = 0; i < pi.length - indexes[2]; i++) {
            if (spiIndex[pi[indexes[2] + i]].length == 1) continue;
            result[counter] = pi[indexes[2] + i];
            counter++;
        }

        return result;
    }
}