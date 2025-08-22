package br.unb.cic.tdp.base;

import lombok.val;
import org.apache.commons.lang3.time.StopWatch;

import java.util.concurrent.locks.ReentrantLock;

public class GPUSorter {
    static {
        System.loadLibrary("tdp_jni");
    }

    public static ReentrantLock lock = new ReentrantLock();
    private static long totalTime = 0;
    private static int callCount = 0;

    private static native short[][] sort(short[] pi, short[] piv, short[] spi, double minRate, int maxDepth, int batchSize);

    public static short[][] syncSort(short[] pi, short[] piv, short[] spi, double minRate, int maxDepth) {
        lock.lock();
        try {
            val stopWatch = new StopWatch();
            stopWatch.start();
            try {
                return sort(pi, piv, spi, minRate, maxDepth, 20000);
            } finally {
                stopWatch.stop();
                synchronized (GPUSorter.class) {
                    totalTime += stopWatch.getTime();
                    callCount++;
                    if (callCount % 1000 == 0) {
                        System.out.printf("Average GPU Sort time per 100 calls: %.2f ms%n", totalTime / 1000.0);
                        totalTime = 0;
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }
}
