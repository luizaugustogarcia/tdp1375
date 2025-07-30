package br.unb.cic.tdp.util;

import br.unb.cic.tdp.base.CommonOperations;
import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;
import lombok.val;
import org.apache.commons.lang3.time.StopWatch;

import java.util.concurrent.TimeUnit;

public class VectorizedByteTransposition {

    // --- Vector API setup ---
    private static final VectorSpecies<Byte> SPECIES = ByteVector.SPECIES_128;

    public static byte[] applyTransposition(final byte[] pi, final byte a, final byte b, final byte c) {
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
    private static void vectorCopy(final byte[] src, final int srcPos, final byte[] dst, final int dstPos, final int length) {
        var i = 0;
        val upperBound = SPECIES.loopBound(length);

        while (i < upperBound) {
            val v = ByteVector.fromArray(SPECIES, src, srcPos + i);
            v.intoArray(dst, dstPos + i);
            i += SPECIES.length();
        }
        for (; i < length; i++) {
            dst[dstPos + i] = src[srcPos + i];
        }
    }

    private static byte[] indexOf(final byte[] array, final byte a, final byte b, final byte c) {
        val result = new byte[]{-1, -1, -1};

        val upperBound = SPECIES.loopBound(array.length);
        var i = 0;

        while (i < upperBound) {
            if (allFound(result)) {
                break;
            }
            val vector = ByteVector.fromArray(SPECIES, array, i);
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
                if (result[0] == -1 && array[i] == a) result[0] = (byte) i;
                if (result[1] == -1 && array[i] == b) result[1] = (byte) i;
                if (result[2] == -1 && array[i] == c) result[2] = (byte) i;
                if (allFound(result)) {
                    break;
                }
            }
        }

        return result;
    }

    private static boolean allFound(byte[] result) {
        return result[0] > -1 && result[1] > -1 && result[2] > -1;
    }

    private static byte getIndex(final byte a, final ByteVector vector, final int i) {
        val mask = vector.compare(VectorOperators.EQ, a);
        if (mask.anyTrue()) {
            return (byte) (i + mask.firstTrue());
        }
        return -1;
    }

    // --- Demo usage ---
    public static void main(String[] args) throws InterruptedException {
         //Thread.sleep(30_000);

        val pi = CommonOperations.CANONICAL_PI[15].getSymbolIndexes();

        var stopWatch = new StopWatch();
        stopWatch.start();

        val bytePi = toByteArray(pi);

        for (int i = 0; i < 10000000; i++) {
            applyTransposition(bytePi, (byte) 0, (byte) 9, (byte) 14);
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

    public static byte[] toByteArray(int[] input) {
        byte[] result = new byte[input.length];
        for (int i = 0; i < input.length; i++) {
            result[i] = (byte) input[i]; // truncates to lower 8 bits
        }
        return result;
    }
}
