package br.unb.cic.tdp.base;

import lombok.val;
import org.apache.commons.lang3.time.StopWatch;

import java.util.concurrent.atomic.AtomicInteger;

public class GPUSorter {
    static {
        System.loadLibrary("tdp_jni");
    }

    public static int DEVICES_COUNT = 1;
    public static AtomicInteger CURRENT_DEVICE = new AtomicInteger(0);

    private static long totalTime = 0;
    private static int callCount = 0;

    private static native short[][] sort(short[] pi, short[] piv, short[] spi, double minRate, int maxDepth, int batchSize, int device);

    public static short[][] syncSort(short[] pi, short[] piv, short[] spi, double minRate, int maxDepth) {
        val stopWatch = new StopWatch();
        stopWatch.start();
        try {
            val device = CURRENT_DEVICE.getAndUpdate(i -> (i + 1) % DEVICES_COUNT);
            return sort(pi, piv, spi, minRate, maxDepth, 20000, device);
        } finally {
            stopWatch.stop();
            synchronized (GPUSorter.class) {
                totalTime += stopWatch.getTime();
                callCount++;
                if (callCount % 1000 == 0) {
                    System.out.printf("Average GPU Sort time per 1000 calls: %.2f ms%n", totalTime / 1000.0);
                    totalTime = 0;
                }
            }
        }
    }
}
