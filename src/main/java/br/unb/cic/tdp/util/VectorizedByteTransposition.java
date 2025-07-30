package br.unb.cic.tdp.util;

import br.unb.cic.tdp.base.CommonOperations;
import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.VectorSpecies;
import lombok.val;
import org.apache.commons.lang3.time.StopWatch;

import java.util.concurrent.TimeUnit;

public class VectorizedByteTransposition {

    // --- Vector API setup ---
    private static final VectorSpecies<Byte> SPECIES = ByteVector.SPECIES_128;

    public static byte[] applyTransposition(final byte[] pi, final byte i, final byte j, final byte k) {
        val clone = pi.clone();  // Snapshot to allow safe overwrite

        val len1 = j - i;
        val len2 = k - j;

        vectorCopy(pi, j, clone, i, len2);               // Move second block
        vectorCopy(pi, i, clone, i + len2, len1);        // Move first block

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
            CommonOperations.applyTranspositionOptimized(CommonOperations.CANONICAL_PI[32].getSymbolIndexes(), 0, 9, 14);
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
