package br.unb.cic.tdp.util;

import br.unb.cic.tdp.base.CommonOperations;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;
import lombok.val;
import org.apache.commons.lang3.time.StopWatch;

import java.util.concurrent.TimeUnit;

public class VectorizedIntTransposition {

    // --- Vector API setup ---
    private static final VectorSpecies<Integer> SPECIES = IntVector.SPECIES_256;

    public static int[] applyTransposition(final int[] pi, final int a, final int b, final int c) {
        val clone = pi.clone();  // Snapshot to allow safe overwrite

        val indexes = indexOf(pi, a, b, c);
        val ia = indexes[0];
        val ib = indexes[1];
        val ic = indexes[2];

        val len1 = ib - ia;
        val len2 = ic - ib;

        vectorCopy(pi, ib, clone, ia, len2);               // Move second block
        vectorCopy(pi, ia, clone, ia + len2, len1);        // Move first block

        return clone;
    }

    // --- Helper: SIMD copy ---
    private static void vectorCopy(final int[] src, final int srcPos, final int[] dst, final int dstPos, final int length) {
        var i = 0;
        val upperBound = SPECIES.loopBound(length);

        while (i < upperBound) {
            val v = IntVector.fromArray(SPECIES, src, srcPos + i);
            v.intoArray(dst, dstPos + i);
            i += SPECIES.length();
        }
        for (; i < length; i++) {
            dst[dstPos + i] = src[srcPos + i];
        }
    }

    private static final VectorSpecies<Integer> BIG_SPECIES = IntVector.SPECIES_256;

    private static int[] _indexOf(final int[] array, final int a, final int b, final int c) {
        val result = new int[3];

        val aVector = IntVector.broadcast(BIG_SPECIES, a);
        val bVector = IntVector.broadcast(BIG_SPECIES, b);
        val cVector = IntVector.broadcast(BIG_SPECIES, c);

        val mask = BIG_SPECIES.indexInRange(0, array.length);
        val vector = IntVector.fromArray(BIG_SPECIES, array, 0, mask);

        var m = vector.compare(VectorOperators.EQ, aVector);
        if (m.anyTrue()) {
            result[0] = (int) m.firstTrue();
        }

        m = vector.compare(VectorOperators.EQ, bVector);
        if (m.anyTrue()) {
            result[1] = (int) m.firstTrue();
        }

        m = vector.compare(VectorOperators.EQ, cVector);
        if (m.anyTrue()) {
            result[2] = (int) m.firstTrue();
        }

        return result;
    }

    private static int[] indexOf(final int[] array, final int a, final int b, final int c) {
        val result = new int[]{-1, -1, -1};

        val upperBound = SPECIES.loopBound(array.length);
        var i = 0;

        while (i < upperBound) {
            if (allFound(result)) {
                break;
            }
            val vector = IntVector.fromArray(SPECIES, array, i);
            if (result[0] == -1)
                result[0] = getIndex(a, vector, i);
            if (result[1] == -1)
                result[1] = getIndex(b, vector, i);
            if (result[2] == -1)
                result[2] = getIndex(c, vector, i);
            i += SPECIES.length();
        }

        if (!allFound(result)) {
            for (; i < array.length; i++) {
                if (result[0] == -1 && array[i] == a) result[0] = (int) i;
                if (result[1] == -1 && array[i] == b) result[1] = (int) i;
                if (result[2] == -1 && array[i] == c) result[2] = (int) i;
                if (allFound(result)) {
                    break;
                }
            }
        }

        return result;
    }

    private static boolean allFound(int[] result) {
        return result[0] > -1 && result[1] > -1 && result[2] > -1;
    }

    private static int getIndex(final int a, final IntVector vector, final int i) {
        val mask = vector.compare(VectorOperators.EQ, a);
        if (mask.anyTrue()) {
            return (int) (i + mask.firstTrue());
        }
        return -1;
    }

    // --- Demo usage ---
    public static void main(String[] args) throws InterruptedException {
         //Thread.sleep(30_000);

        val pi = CommonOperations.CANONICAL_PI[15].getSymbolIndexes();

        var stopWatch = new StopWatch();
        stopWatch.start();

        val intPi = toIntegerArray(pi);

        for (int i = 0; i < 10000000; i++) {
            applyTransposition(intPi, (int) 0, (int) 9, (int) 14);
        }

        stopWatch.stop();

        System.out.println("Time taken SIMD: " + stopWatch.getTime(TimeUnit.MILLISECONDS) + " ms");

        stopWatch = new StopWatch();
        stopWatch.start();

        for (int i = 0; i < 10000000; i++) {
            CommonOperations.applyTranspositionOptimized(CommonOperations.CANONICAL_PI[32].getSymbolIndexes(),
                    0, 9, 14);
        }

        stopWatch.stop();

        System.out.println("Time taken CPU: " + stopWatch.getTime(TimeUnit.MILLISECONDS) + " ms");
    }

    public static int[] toIntegerArray(int[] input) {
        int[] result = new int[input.length];
        for (int i = 0; i < input.length; i++) {
            result[i] = (int) input[i]; // truncates to lower 8 bits
        }
        return result;
    }
}
