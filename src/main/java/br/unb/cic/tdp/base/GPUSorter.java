package br.unb.cic.tdp.base;

import lombok.val;
import org.apache.commons.lang3.time.StopWatch;

import java.util.concurrent.locks.ReentrantLock;

public class GPUSorter {
    static {
        System.loadLibrary("tdp_jni");
    }

    public static ReentrantLock lock = new ReentrantLock();

    private static native short[][] sort(short[] pi, short[] piv, short[] spi, double minRate, int maxDepth);

    public static short[][] syncSort(short[] pi, short[] piv, short[] spi, double minRate, int maxDepth) {
        lock.lock();
        try {
            val stopWatch = new StopWatch();
            stopWatch.start();
            try {
                return sort(pi, piv, spi, minRate, maxDepth);
            } finally {
                stopWatch.stop();
                System.out.printf("GPU Sort took %d ms%n", stopWatch.getTime());
            }
        } finally {
            lock.unlock();
        }
    }
}
