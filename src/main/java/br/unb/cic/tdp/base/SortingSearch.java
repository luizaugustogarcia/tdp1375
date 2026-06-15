package br.unb.cic.tdp.base;

import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.permutation.OneLinePermutation;
import br.unb.cic.tdp.proof.ProofGenerator;
import lombok.val;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SortingSearch {

    /**
     * Find a full sorting sequence (fixing all symbols in spi) whose approximation ratio is at least <code>minRate</code>.
     */
    public static List<int[]> searchForSortingSeq(
            final byte[] pi,
            final OneLinePermutation spi,
            final int initialNumberOfEvenCycles,
            final float minRate,
            final int maxMoves
    ) {
        val state = new DynamicCycleState(spi);
        val movesFlat = new int[maxMoves * 3];
        val depth = searchForSortingSeq(pi, movesFlat, 0, initialNumberOfEvenCycles, minRate, maxMoves, state);
        if (depth == 0) return Collections.emptyList();
        val result = new java.util.ArrayList<int[]>(depth);
        for (int m = 0; m < depth; m++) {
            result.add(new int[]{movesFlat[m * 3], movesFlat[m * 3 + 1], movesFlat[m * 3 + 2]});
        }
        return result;
    }

    private static int searchForSortingSeq(
            final byte[] pi,
            final int[] movesFlat,
            final int depth,
            final int initialNumberOfEvenCycles,
            final float minRate,
            final int maxMoves,
            final DynamicCycleState state
    ) {
        val numberOfEvenCycles = state.evenCycles;

        if (numberOfEvenCycles == pi.length) {
            val numberOfEvenCyclesCreatedSoFar = numberOfEvenCycles - initialNumberOfEvenCycles;
            val achievedRatio = numberOfEvenCyclesCreatedSoFar / (float) depth;
            if (achievedRatio >= minRate) {
                return depth;
            }
            return 0;
        }

        if (depth == maxMoves) {
            return 0;
        }

        val numberOfEvenCyclesCreatedSoFar = numberOfEvenCycles - initialNumberOfEvenCycles;
        val movesLeft = maxMoves - depth;
        val maxPossibleEvenCycles = pi.length - numberOfEvenCycles;

        if (numberOfEvenCyclesCreatedSoFar + movesLeft * 2 < Math.ceil(minRate * (depth + Math.ceil(maxPossibleEvenCycles / 2.0)))) {
            return 0;
        }

        val lowerBound = (int) Math.ceil(maxPossibleEvenCycles / 2.0);
        if (depth + lowerBound > maxMoves) {
            return 0;
        }

        val temp = state.temp;

        for (var i = 0; i < pi.length - 2; i++) {
            val a = pi[i] & 0xFF;

            for (var j = i + 1; j < pi.length - 1; j++) {
                val b = pi[j] & 0xFF;

                for (var k = j + 1; k < pi.length; k++) {
                    val c = pi[k] & 0xFF;

                    state.applyMove(a, b, c);

                    val base = depth * 3;
                    movesFlat[base] = a;
                    movesFlat[base + 1] = b;
                    movesFlat[base + 2] = c;

                    applyInPlace(pi, i, j, k, temp);

                    val result = searchForSortingSeq(pi, movesFlat, depth + 1, initialNumberOfEvenCycles, minRate, maxMoves, state);
                    if (result > 0) {
                        return result;
                    }

                    undoInPlace(pi, i, j, k, temp);
                    state.undoMove(a, b, c);
                }
            }
        }

        return 0;
    }

    private static void applyInPlace(final byte[] pi, final int i, final int j, final int k, final byte[] temp) {
        val len = k - i;
        System.arraycopy(pi, i, temp, 0, len);
        System.arraycopy(temp, j - i, pi, i, k - j);
        System.arraycopy(temp, 0, pi, i + (k - j), j - i);
    }

    private static void undoInPlace(final byte[] pi, final int i, final int j, final int k, final byte[] temp) {
        val len = k - i;
        System.arraycopy(pi, i, temp, 0, len);
        System.arraycopy(temp, k - j, pi, i, j - i);
        System.arraycopy(temp, 0, pi, j, k - j);
    }

    private static boolean isEvenCycle(final int cycleSize) {
        return (cycleSize & 1) == 1;
    }

    private static final class DynamicCycleState {
        private final byte[] oneLine;
        private final int[] cycleId;
        private final int[] cycleSize;
        int evenCycles;

        private final int[] visitedStamp;
        private int stamp;
        private final int[] touched;
        private int touchedSize;

        private final int[] cycleStamp;
        private int cycleStampVal;

        private long[] undoStack;
        private int undoTop;

        final byte[] temp;

        private DynamicCycleState(final OneLinePermutation spi) {
            this.oneLine = spi.getOneLine();
            this.cycleId = new int[oneLine.length];
            this.cycleSize = new int[oneLine.length];
            this.visitedStamp = new int[oneLine.length];
            this.touched = new int[oneLine.length];
            this.cycleStamp = new int[oneLine.length];
            this.undoStack = new long[64];
            this.temp = new byte[oneLine.length];
            Arrays.fill(this.cycleId, -1);
            buildInitialCycles();
        }

        private void buildInitialCycles() {
            val visited = new boolean[oneLine.length];
            for (int s = 0; s < oneLine.length; s++) {
                if (visited[s]) continue;

                val id = s;
                var size = 0;
                var cur = s;
                do {
                    visited[cur] = true;
                    cycleId[cur] = id;
                    size++;
                    cur = oneLine[cur] & 0xFF;
                } while (cur != s);

                cycleSize[id] = size;
                if (isEvenCycle(size)) evenCycles++;
            }
        }

        void applyMove(final int a, final int b, final int c) {
            val av = oneLine[a];
            val bv = oneLine[b];
            val cv = oneLine[c];
            val previousEvenCycles = evenCycles;

            // push undo data
            if (undoTop == undoStack.length) {
                undoStack = Arrays.copyOf(undoStack, undoStack.length * 2);
            }
            undoStack[undoTop++] = (av & 0xFFL) | ((bv & 0xFFL) << 8) | ((cv & 0xFFL) << 16) | ((long) previousEvenCycles << 24);

            val oldLocalEven = collectLocalEvenAndTouched(a, b, c);
            oneLine[a] = cv;
            oneLine[b] = av;
            oneLine[c] = bv;
            val newLocalEven = relabelTouchedAndCountEven();
            evenCycles += (newLocalEven - oldLocalEven);
        }

        void undoMove(final int a, final int b, final int c) {
            val packed = undoStack[--undoTop];
            val av = (byte) (packed & 0xFF);
            val bv = (byte) ((packed >>> 8) & 0xFF);
            val cv = (byte) ((packed >>> 16) & 0xFF);
            val previousEvenCycles = (int) (packed >>> 24);

            oneLine[a] = av;
            oneLine[b] = bv;
            oneLine[c] = cv;

            collectLocalEvenAndTouched(a, b, c);
            relabelTouchedAndCountEven();
            evenCycles = previousEvenCycles;
        }

        private int collectLocalEvenAndTouched(final int a, final int b, final int c) {
            stamp++;
            touchedSize = 0;
            cycleStampVal++;
            var localEven = 0;

            touchedSize = collectComponent(a, touchedSize);
            touchedSize = collectComponent(b, touchedSize);
            touchedSize = collectComponent(c, touchedSize);

            for (int i = 0; i < touchedSize; i++) {
                val id = cycleId[touched[i]];
                if (cycleStamp[id] != cycleStampVal) {
                    cycleStamp[id] = cycleStampVal;
                    if (isEvenCycle(cycleSize[id])) localEven++;
                }
            }
            return localEven;
        }

        private int relabelTouchedAndCountEven() {
            var localEven = 0;

            for (int i = 0; i < touchedSize; i++) {
                cycleId[touched[i]] = -1;
            }

            for (int i = 0; i < touchedSize; i++) {
                val s = touched[i];
                if (cycleId[s] != -1) continue;

                val id = s;
                var size = 0;
                var cur = s;
                do {
                    cycleId[cur] = id;
                    size++;
                    cur = oneLine[cur] & 0xFF;
                } while (cur != s);

                cycleSize[id] = size;
                if (isEvenCycle(size)) localEven++;
            }
            return localEven;
        }

        private int collectComponent(final int start, int touchedSize) {
            var cur = start;
            while (visitedStamp[cur] != stamp) {
                visitedStamp[cur] = stamp;
                touched[touchedSize++] = cur;
                cur = oneLine[cur] & 0xFF;
            }
            return touchedSize;
        }
    }

    public static void main(String[] args) {
        val spi = new MulticyclePermutation("(0 3 4 1 5 2 6)");
        val pi = new byte[]{0, 4, 5, 6, 3, 1, 2};

        long start = System.nanoTime();
        val moves = searchForSortingSeq(pi, new OneLinePermutation(spi.getOneLineNotation()), spi.getNumberOfEvenCycles(), 2 / 1.375F, 4);
        long end = System.nanoTime();

        if (moves.isEmpty()) {
            System.out.println("No sorting sequence found.");
        } else {
            System.out.println("Found " + moves.size() + " moves:");
            for (val move : moves) {
                System.out.println("  (" + move[0] + ", " + move[1] + ", " + move[2] + ")");
            }
        }

        System.out.printf("Execution time: %.3f ms%n", (end - start) / 1_000_000.0);
    }
}
