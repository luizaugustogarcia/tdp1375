package br.unb.cic.tdp.experiments;

import br.unb.cic.tdp.BaseAlgorithm;
import br.unb.cic.tdp.EliasAndHartman;
import br.unb.cic.tdp.Silvaetal;
import br.unb.cic.tdp.permutation.Cycle;
import lombok.SneakyThrows;
import org.apache.commons.lang.time.StopWatch;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class ShortPermutations {

    public static void main(String[] args) throws IOException, InterruptedException {
//        System.out.println("= EH =");
//        audit(args[0], new EliasAndHartman());
        System.out.println("= Silva et al. =");
        audit(args[0], new Silvaetal());
    }

    @SneakyThrows
    static void audit(final String exactDistancesRoot, final BaseAlgorithm algorithm) {
        for (int i = 2; i <= 12; i++) {
            final var pool = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(),
                    Runtime.getRuntime().availableProcessors(), 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(100));
            pool.setRejectedExecutionHandler(new BlockPolicy());

            try (final var br = new BufferedReader(new FileReader(exactDistancesRoot + "exact" + i + ".txt"), 1024 * 1024 * 10)) {
                final float[] maxRatio = {0};
                final var lock = new ReentrantLock(true);
                final BigDecimal[] sumRatios = {BigDecimal.ZERO};
                final long[] wrongAnswers = {0};
                final int[] total1_5 = {0};

                final long[] sumDistance = {0};
                final var stopWatch = new StopWatch();
                stopWatch.start();

                String line;
                while ((line = br.readLine()) != null) {
                    final var split = line.split(" ");
                    if (!split[1].equals("0")) {
                        pool.submit(() -> {
                            final var pi = Cycle.create("0," + split[0]);
                            final var exact = Integer.parseInt(split[1]);

                            final var sorting = algorithm.sort(pi);

                            final Float ratio = sorting.getSecond().size() / (float) exact;
                            if (ratio < 1)
                                throw new RuntimeException();

                            try {
                                lock.lock();

                                sumDistance[0] += sorting.getSecond().size();

                                sumRatios[0] = sumRatios[0].add(new BigDecimal(ratio));

                                if (ratio > maxRatio[0]) {
                                    maxRatio[0] = ratio;
                                }

                                if (ratio == 1.5f) {
                                    total1_5[0]++;
                                }

                                if (sorting.getSecond().size() != exact) {
                                    wrongAnswers[0]++;
                                }
                            } finally {
                                lock.unlock();
                            }
                        });
                    }
                }

                pool.shutdown();
                pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);

                stopWatch.stop();

                System.out.println(i + ","
                        + maxRatio[0] + ","
                        + sumRatios[0].floatValue() / (factorial(i) - 1) + ","
                        + ((factorial(i) - wrongAnswers[0]) / (float)factorial(i)) * 100 + ","
                        + total1_5[0] + ","
                        + sumDistance[0] / (float)(factorial(i) - 1) + ","
                        + stopWatch.getTime());
            }
        }
    }

    static int factorial(final int n){
        if (n == 0)
            return 1;
        else
            return(n * factorial(n - 1));
    }

    public static class BlockPolicy implements RejectedExecutionHandler {

        @SneakyThrows
        public void rejectedExecution(final Runnable r, final ThreadPoolExecutor executor) {
            executor.getQueue().put(r);
        }
    }
}
