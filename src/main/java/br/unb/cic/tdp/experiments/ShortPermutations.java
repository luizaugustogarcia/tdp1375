package br.unb.cic.tdp.experiments;

import br.unb.cic.tdp.AbstractSbtAlgorithm;
import br.unb.cic.tdp.EliasAndHartman;
import br.unb.cic.tdp.Silvaetal;
import br.unb.cic.tdp.permutation.Cycle;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.commons.lang.time.StopWatch;

import java.io.BufferedReader;
import java.io.FileReader;
import java.math.BigDecimal;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class ShortPermutations {

    public static void main(final String[] args) {
        System.out.println("= EH =");
        audit(args[0], new EliasAndHartman());
        System.out.println("= Silva et al. =");
        audit(args[0], new Silvaetal());
    }

    @SneakyThrows
    static void audit(final String exactDistancesRoot, final AbstractSbtAlgorithm algorithm) {
        for (var i = 2; i <= 12; i++) {
            val pool = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(),
                    Runtime.getRuntime().availableProcessors(), 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(100));
            pool.setRejectedExecutionHandler(new BlockPolicy());

            try (val br = new BufferedReader(new FileReader(exactDistancesRoot + "exact" + i + ".txt"), 1024 * 1024 * 10)) {
                final float[] maxRatio = {0};
                val lock = new ReentrantLock(true);
                final BigDecimal[] sumRatios = {BigDecimal.ZERO};
                final long[] wrongAnswers = {0};
                final int[] total1_5 = {0};

                final long[] sumDistance = {0};
                val stopWatch = new StopWatch();
                stopWatch.start();

                final int[] totalOfPermutations = {0};
                String line;
                while ((line = br.readLine()) != null) {
                    val split = line.split(" ");
                    if (!split[1].equals("0")) {
                        pool.submit(() -> {
                            val pi = Cycle.create("0," + split[0]);
                            val exact = Integer.parseInt(split[1]);

                            val sorting = algorithm.sort(pi);

                            try {
                                lock.lock();

                                totalOfPermutations[0]++;

                                sumDistance[0] += sorting.getSecond().size();

                                sumRatios[0] = sumRatios[0].add(new BigDecimal(sorting.getSecond().size()).divide(new BigDecimal(exact)));

                                final float ratio = sorting.getSecond().size() / (float) exact;
                                if (ratio < 1f)
                                    throw new RuntimeException();

                                if (ratio > maxRatio[0]) {
                                    maxRatio[0] = ratio;
                                }

                                if (ratio > 1.375f) {
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
                        + "(" + sumRatios[0].floatValue() + "/" + totalOfPermutations[0] + ")=" + sumRatios[0].floatValue() / totalOfPermutations[0] + ","
                        + ((totalOfPermutations[0] - wrongAnswers[0]) / (float) totalOfPermutations[0]) * 100 + ","
                        + total1_5[0] + ","
                        + sumDistance[0] / (float) totalOfPermutations[0] + ","
                        + stopWatch.getTime());
            }
        }
    }

    public static class BlockPolicy implements RejectedExecutionHandler {

        @SneakyThrows
        public void rejectedExecution(final Runnable r, final ThreadPoolExecutor executor) {
            executor.getQueue().put(r);
        }
    }
}
