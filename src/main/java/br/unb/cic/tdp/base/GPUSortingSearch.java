package br.unb.cic.tdp.base;

import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.permutation.OneLinePermutation;
import lombok.SneakyThrows;
import lombok.val;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

public class GPUSortingSearch {

    static {
        System.loadLibrary("tdp1375_jni");
    }

    private final ArrayBlockingQueue<Integer> slotPool;

    public GPUSortingSearch(int devicesCount, int slotsPerDevice, long queueBytesBudget, int dedupTableSize) {
        int totalSlots = devicesCount * slotsPerDevice;
        this.slotPool = new ArrayBlockingQueue<>(totalSlots);
        for (int i = 0; i < totalSlots; i++) {
            slotPool.add(i);
        }
        initSlots(totalSlots, devicesCount, queueBytesBudget, dedupTableSize);
    }

    private static native void initSlots(int totalSlots, int devicesCount, long queueBytesBudget, int dedupTableSize);

    public static native void destroySlots();

    private static native int[][] searchForSortingSeq(byte[] pi, byte[] spi, int initialEvenCycles, float minRate, int maxMoves, boolean fullSorting, int slot);

    public static native void cancelSort(int slot);

    public static native byte[][] filterPiPermutations(
            byte[] cycle,
            int cycleLen,
            byte[] orientedTriple,
            int tripleLen,
            int n,
            boolean skip2MoveCheck
    );

    @SneakyThrows
    public List<int[]> search(
            final byte[] pi,
            final OneLinePermutation spi,
            final int initialEvenCycles,
            final float minRate,
            final int maxMoves,
            final boolean fullSorting
    ) {
        val slot = slotPool.take();

        try {
            val result = searchForSortingSeq(
                    pi, spi.getOneLine(),
                    initialEvenCycles, minRate, maxMoves,
                    fullSorting, slot);
            if (result == null || result.length == 0) return Collections.emptyList();

            val currentPi = pi.clone();
            val moves = new ArrayList<int[]>(result.length);
            for (val posMove : result) {
                int i = posMove[0], j = posMove[1], k = posMove[2];
                int a = currentPi[i] & 0xFF;
                int b = currentPi[j] & 0xFF;
                int c = currentPi[k] & 0xFF;
                moves.add(new int[]{a, b, c});
                applyTransposition(currentPi, i, j, k);
            }
            return moves;
        } finally {
            slotPool.add(slot);
        }
    }

    private static void applyTransposition(final byte[] pi, final int i, final int j, final int k) {
        val len = k - i;
        val temp = new byte[len];
        System.arraycopy(pi, i, temp, 0, len);
        System.arraycopy(temp, j - i, pi, i, k - j);
        System.arraycopy(temp, 0, pi, i + (k - j), j - i);
    }

    public static void main(String[] args) {
        val spi = new MulticyclePermutation("(0 22 20)(1 23 3)(2 6 4)(5 9 7)(8 12 10)(11 15 13)(14 18 16)(17 21 19)");
        val pi = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23};

        val gpu = new GPUSortingSearch(1, 2, 1L << 30, 1 << 20);
        long start = System.nanoTime();
        val moves = gpu.search(pi, new OneLinePermutation(spi.getOneLineNotation()), spi.getNumberOfEvenCycles(), 2 / 1.375F, 11, true);
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
