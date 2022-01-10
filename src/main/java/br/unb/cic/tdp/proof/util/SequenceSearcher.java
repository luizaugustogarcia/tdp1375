package br.unb.cic.tdp.proof.util;

import br.unb.cic.tdp.util.Triplet;
import com.google.common.primitives.Ints;
import lombok.SneakyThrows;
import org.apache.commons.lang.ArrayUtils;

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
            final var sorting = analyze0Moves(spi, parity, spiIndex, maxSymbol, pi, moves, root);
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

                    final var is_2Move = spiIndex[a] != spiIndex[b] &&
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
                        final var cycle = current.data;

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
                        for (final var m : root.children) {
                            int[] newPi = applyTransposition(pi, a, b, c, pi.length - numberOfTrivialCycles, spiIndex);
                            final var sorting = search(spi, parity, spiIndex, maxSymbol, newPi, moves, m);
                            if (!sorting.isEmpty()) {
                                return moves.toListOfCycles();
                            }
                        }
                    }

                    // ==== ROLLBACK ====
                    current = triplet.second.head;
                    for (int l = 0; l < triplet.second.size; l++) {
                        final var cycle = current.data;
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

    private static boolean contains(final String[] array, final String string) {
        for (String s : array) {
            if (s.equals(string))
                return true;
        }
        return false;
    }

    private static String getCanonicalSignature(final ListOfCycles spi,
                                                final int[] pi,
                                                final int[][] spiIndex,
                                                final int maxSymbol) {
        var leastHashCode = Integer.MAX_VALUE;
        float[] canonical = null;

        for (int symbol : pi) {
            final var shifting = startingBy(pi, symbol);

            var signature = signature(spi, shifting, spiIndex, maxSymbol);

            var hashCode = Arrays.hashCode(signature);

            if (hashCode < leastHashCode) {
                leastHashCode = hashCode;
                canonical = signature;
            } else if (hashCode == leastHashCode) {
                canonical = least(signature, canonical);
            }

            final var mirroredSignature = signature.clone();
            ArrayUtils.reverse(mirroredSignature);

            final var labelLabelMapping = new int[spi.size + 1];
            final var orientedIndexMapping = new int[spi.size + 1][];

            var nextLabel = 1;
            for (int j = 0; j < mirroredSignature.length; j++) {
                final var label = mirroredSignature[j];

                if (labelLabelMapping[(int) label] == 0) {
                    labelLabelMapping[(int) label] = nextLabel++;
                }

                final var newLabel = labelLabelMapping[(int) label];

                if (label % 1 > 0) {
                    if (orientedIndexMapping[newLabel] == null) {
                        final var index = Math.abs(j - shifting.length) - 1;
                        final int[] cycle = startingBy(spiIndex[shifting[index]], shifting[index]);
                        orientedIndexMapping[newLabel] = cycleIndex(cycle);
                    }

                    final var index = Math.abs(j - shifting.length) - 1;
                    final var orientationIndex = orientedIndexMapping[newLabel][shifting[index]] + 1;
                    mirroredSignature[j] = newLabel + ((float) orientationIndex / 100);
                } else {
                    mirroredSignature[j] = newLabel;
                }
            }

            hashCode = Arrays.hashCode(mirroredSignature);
            if (hashCode < leastHashCode) {
                leastHashCode = hashCode;
                canonical = signature;
            } else if (hashCode == leastHashCode) {
                canonical = least(signature, canonical);
            }
        }

        return toString(canonical);
    }

    private static float[] least(final float[] signature, final float[] canonical) {
        for (int i = 0; i < signature.length; i++) {
            if (signature[i] != canonical[i]) {
                if (signature[i] < canonical[i])
                    return signature;
                else
                    return canonical;
            }
        }
        return canonical;
    }

    public static float[] signature(final ListOfCycles spi, final int[] pi, final int[][] spiIndex, final int maxSymbol) {
        final var piInverseIndex = getPiInverseIndex(pi, maxSymbol);
        final var orientedCycles = getOrientedCycles(spi, piInverseIndex);

        final var orientationByCycle = new boolean[maxSymbol + 1];
        Arrays.fill(orientationByCycle, false);

        var current = orientedCycles.head;
        for (int l = 0; l < orientedCycles.size; l++) {
            orientationByCycle[current.data[0]] = true;
        }

        final var labelByCycle = new float[maxSymbol + 1];
        Arrays.fill(labelByCycle, -1);

        final var symbolIndexByOrientedCycle = new int[maxSymbol + 1][];

        final var signature = new float[pi.length];

        // Pi index
        final var piIndex = new int[maxSymbol + 1];
        for (var i = 0; i < pi.length; i++) {
            piIndex[pi[i]] = i;
        }

        var currentLabel = 1f;

        for (var i = 0; i < signature.length; i++) {
            final int symbol = pi[i];
            final var cycle = spiIndex[symbol];

            if (orientationByCycle[cycle[0]]) {
                final var symbolIndex = new int[maxSymbol + 1];

                var symbolMinIndex = Integer.MAX_VALUE;
                for (int s : cycle) {
                    if (piIndex[s] < symbolMinIndex)
                        symbolMinIndex = piIndex[s];
                }

                for (int j = 0; j < cycle.length; j++) {
                    if (cycle[j] == symbolMinIndex) {
                        for (int k = 0; k < cycle.length; k++) {
                            symbolIndex[cycle[(j + k) % cycle.length]] = k + 1;
                        }
                        break;
                    }
                }
                symbolIndexByOrientedCycle[cycle[0]] = symbolIndex;
            }

            if (labelByCycle[cycle[0]] == -1) {
                labelByCycle[cycle[0]] = currentLabel;
                currentLabel++;
            }

            signature[i] = orientationByCycle[cycle[0]] ?
                    labelByCycle[cycle[0]] + (float) symbolIndexByOrientedCycle[cycle[0]][symbol] / 100 : labelByCycle[cycle[0]];
        }

        return signature;
    }

    private static String toString(final float[] signature) {
        final var builder = new StringBuilder();
        for (float v : signature) {
            if (v % 1 == 0) {
                builder.append((int) v);
            } else {
                builder.append(v);
            }
            builder.append(',');
        }
        return builder.toString();
    }

    private ListOfCycles analyzeOrientedCycles(final ListOfCycles spi,
                                               final boolean[] parity,
                                               final int[][] spiIndex,
                                               final int maxSymbol,
                                               final int[] pi,
                                               final MovesStack moves,
                                               final MoveTreeNode root) {
        final var piInverseIndex = getPiInverseIndex(pi, maxSymbol);

        final var orientedCycles = getOrientedCycles(spi, piInverseIndex);

        var current = orientedCycles.head;
        for (int l = 0; l < orientedCycles.size; l++) {
            final var cycle = current.data;

            final var before = parity[cycle[0]] ? 1 : 0;

            for (var i = 0; i < cycle.length - 2; i++) {
                for (var j = i + 1; j < cycle.length - 1; j++) {
                    final var ab_k = j - i;

                    if (before == 1 && (ab_k & 1) == 0) {
                        continue;
                    }

                    for (var k = j + 1; k < cycle.length; k++) {
                        final var bc_k = k - j;

                        if (before == 1 && (bc_k & 1) == 0) {
                            continue;
                        }

                        final var ca_k = (cycle.length - k) + i;

                        int a = cycle[i], b = cycle[j], c = cycle[k];

                        var after = ab_k & 1;
                        after += bc_k & 1;
                        after += ca_k & 1;

                        // check if it's applicable
                        if (after - before == 2 && areSymbolsInCyclicOrder(piInverseIndex, a, c, b)) {
                            final int[] symbols = startingBy(cycle, a);
                            final var aCycle = new int[ca_k];
                            aCycle[0] = a;
                            System.arraycopy(symbols, ab_k + bc_k + 1, aCycle, 1, ca_k - 1);

                            final var bCycle = new int[ab_k];
                            bCycle[0] = b;
                            System.arraycopy(symbols, 1, bCycle, 1, ab_k - 1);

                            final var cCycle = new int[bc_k];
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
                                for (final var m : root.children) {
                                    int[] newPi = applyTransposition(pi, a, b, c, pi.length - numberOfTrivialCycles, spiIndex);
                                    final var sorting = search(spi, parity, spiIndex, maxSymbol, newPi, moves, m);
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
        final var cycleIndexes = new int[maxSymbol + 1][];

        for (int i = 0; i < pi.length - 2; i++) {
            for (int j = i + 1; j < pi.length - 1; j++) {
                for (int k = j + 1; k < pi.length; k++) {

                    int a = pi[i], b = pi[j], c = pi[k];

                    final var is_2Move = spiIndex[a] != spiIndex[b] &&
                            spiIndex[b] != spiIndex[c] &&
                            spiIndex[a] != spiIndex[c];
                    if (is_2Move)
                        continue;

                    final Triplet<ListOfCycles, ListOfCycles, Integer> triplet;
                    // if it's the same cycle
                    if (spiIndex[a] == spiIndex[b] && spiIndex[b] == spiIndex[c]) {
                        final var cycle = spiIndex[a];

                        if (cycleIndexes[a] == null) {
                            final var index = cycleIndex(cycle);
                            cycleIndexes[a] = index;
                            cycleIndexes[b] = index;
                            cycleIndexes[c] = index;
                        }

                        final var index = cycleIndexes[a];

                        if (areSymbolsInCyclicOrder(index, a, b, c)) {
                            final var before = cycle.length & 1;

                            final var ab_k = getK(index, cycle, a, b);
                            var after = ab_k & 1;
                            final var bc_k = getK(index, cycle, b, c);
                            after += bc_k & 1;
                            final var ca_k = getK(index, cycle, c, a);
                            after += ca_k & 1;

                            if (after - before == 2) {
                                // skip, it's a 2-move
                                continue;
                            }

                            after = 0;
                            final int[] symbols = startingBy(cycle, a);
                            final var aCycle = new int[ca_k];
                            aCycle[0] = a;
                            System.arraycopy(symbols, ab_k + bc_k + 1, aCycle, 1, ca_k - 1);
                            after += aCycle.length & 1;

                            final var bCycle = new int[ab_k];
                            bCycle[0] = b;
                            System.arraycopy(symbols, 1, bCycle, 1, ab_k - 1);
                            after += bCycle.length & 1;

                            final var cCycle = new int[bc_k];
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
                        final var cycle = current.data;

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
                        for (final var m : root.children) {
                            int[] newPi = applyTransposition(pi, a, b, c, pi.length - numberOfTrivialCycles, spiIndex);
                            final var sorting = search(spi, parity, spiIndex, maxSymbol, newPi, moves, m);
                            if (!sorting.isEmpty()) {
                                return moves.toListOfCycles();
                            }
                        }
                    }

                    // ==== ROLLBACK ====
                    current = triplet.second.head;
                    for (int l = 0; l < triplet.second.size; l++) {
                        final var cycle = current.data;
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
            final var p = (cycle.length & 1) == 1;
            for (int k : cycle) {
                index[k] = cycle;
                parity[k] = p;
            }
        }
    }

    private static int[] getPiInverseIndex(final int[] pi, final int maxSymbol) {
        final var piInverseIndex = new int[maxSymbol + 1];
        for (var i = 0; i < pi.length; i++) {
            piInverseIndex[pi[pi.length - i - 1]] = i;
        }
        return piInverseIndex;
    }

    private static ListOfCycles getOrientedCycles(final ListOfCycles spi, final int[] piInverseIndex) {
        final var orientedCycles = new ListOfCycles();
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
            final var cycle = current.data;
            updateIndex(index, parity, cycle);
        }
    }

    private static void updateIndex(final int[][] index, final boolean[] parity, final int[]... cycles) {
        for (int[] cycle : cycles) {
            final var p = (cycle.length & 1) == 1;
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
        int a_, b_, c_;
        if (spiIndex[a] == spiIndex[c]) {
            a_ = a;
            b_ = c;
            c_ = b;
            numberOfEvenCycles += spiIndex[a].length & 1;
            numberOfEvenCycles += spiIndex[b].length & 1;
        } else if (spiIndex[a] == spiIndex[b]) {
            a_ = b;
            b_ = a;
            c_ = c;
            numberOfEvenCycles += spiIndex[a].length & 1;
            numberOfEvenCycles += spiIndex[c].length & 1;
        } else {
            // spi.getCycle(b) == spi.getCycle(c)
            a_ = c;
            b_ = b;
            c_ = a;
            numberOfEvenCycles += spiIndex[a].length & 1;
            numberOfEvenCycles += spiIndex[c].length & 1;
        }

        final var index = cycleIndex(spiIndex[c_]);
        final var cImage = image(index, spiIndex[c_], c_);
        final var abCycle = startingBy(spiIndex[a_], a_);
        final var cCycle = startingBy(spiIndex[c_], cImage);

        final var abCycleIndex = cycleIndex(abCycle);

        final var ba_k = getK(abCycleIndex, abCycle, b_, a_);
        final var newaCycle = new int[1 + ba_k - 1];
        newaCycle[0] = a_;
        final var ab_k = getK(abCycleIndex, abCycle, a_, b_);
        System.arraycopy(abCycle,  ab_k + 1, newaCycle, 1, ba_k - 1);

        final var newbCycle = new int[1 + cCycle.length + (ab_k - 1)];
        newbCycle[0] = b_;
        System.arraycopy(cCycle, 0, newbCycle, 1, cCycle.length);
        System.arraycopy(abCycle, 1, newbCycle, 1 + cCycle.length, ab_k - 1);

        var newNumberOfEvenCycles = 0;
        newNumberOfEvenCycles += newaCycle.length & 1;
        newNumberOfEvenCycles += newbCycle.length & 1;

        final var oldCycles = new ListOfCycles();
        oldCycles.add(spiIndex[a]);
        if (!oldCycles.contains(spiIndex[b]))
            oldCycles.add(spiIndex[b]);
        if (!oldCycles.contains(spiIndex[c]))
            oldCycles.add(spiIndex[c]);

        final var newCycles = new ListOfCycles();
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
        final int a_ = b, b_ = a, c_ = c;

        final var oldCycle = cycleIndex[a];

        final int[] symbols = startingBy(oldCycle, a_);
        final var newCycle = new int[oldCycle.length];

        final int[] oldCycleIndex = cycleIndex(oldCycle);

        newCycle[0] = a_;
        final var ab_k = getK(oldCycleIndex, oldCycle, a_, b_);
        final var bc_k = getK(oldCycleIndex, oldCycle, b_, c_);
        System.arraycopy(symbols, ab_k + 1, newCycle, 1, bc_k - 1);
        newCycle[bc_k] = c_;

        System.arraycopy(symbols, 1, newCycle, 1 + bc_k, ab_k - 1);
        newCycle[ab_k + bc_k] = b_;

        final var ca_k = getK(oldCycleIndex, oldCycle, c_, a_);
        System.arraycopy(symbols, ab_k + bc_k + 1,
                newCycle, ab_k + bc_k + 1, ca_k - 1);

        return new Triplet<>(ListOfCycles.singleton(oldCycle), ListOfCycles.singleton(newCycle), 0);
    }

    private static int[] cycleIndex(int[] cycle) {
        final var index = new int[Ints.max(cycle) + 1];

        for (int i = 0; i < cycle.length; i++) {
            index[cycle[i]] = i;
        }

        return index;
    }

    private static int getK(int[] cycleIndex, int[] cycle, int a, int b) {
        final var aIndex = cycleIndex[a];
        final var bIndex = cycleIndex[b];

        if (bIndex >= aIndex)
            return bIndex - aIndex;

        return (cycle.length - aIndex) + bIndex;
    }

    private static int[] startingBy(int[] symbols, int a) {
        if (symbols[0] == a)
            return symbols;

        final var result = new int[symbols.length];
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
        final var indexes = new int[3];
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
            final var temp = indexes[0];
            indexes[0] = indexes[2];
            indexes[2] = temp;
        }

        if (indexes[0] > indexes[1]) {
            final var temp = indexes[0];
            indexes[0] = indexes[1];
            indexes[1] = temp;
        }

        if (indexes[1] > indexes[2]) {
            final var temp = indexes[1];
            indexes[1] = indexes[2];
            indexes[2] = temp;
        }

        final var result = new int[numberOfSymbols];

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