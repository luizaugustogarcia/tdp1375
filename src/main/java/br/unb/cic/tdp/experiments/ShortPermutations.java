package br.unb.cic.tdp.experiments;

import br.unb.cic.tdp.EliasAndHartman;
import br.unb.cic.tdp.Silvaetal;
import br.unb.cic.tdp.permutation.Cycle;
import org.apache.commons.lang.time.StopWatch;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ShortPermutations {

    public static void main(String[] args) throws IOException, InterruptedException {
        final var algorithm = new EliasAndHartman();

        for (int i = 2; i <= 12; i++) {
            final var pool = new ThreadPoolExecutor(8, 8,
                    0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(100));
            pool.setRejectedExecutionHandler(new BlockPolicy());


            try (final var br = new BufferedReader(new FileReader("C:\\Users\\Luiz Silva\\Temp\\distances\\exact" + i + ".txt"))) {
                final double[] maxRatio = {0};
                final var lock = new Lock();
                final double[] sumRatios = {0};
                final int[] wrongAnswers = {0};

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
                            final var ratio = sorting.size() / (double) exact;

                            try {
                                lock.lock();
                                sumRatios[0] += ratio;

                                if (ratio > maxRatio[0]) {
                                    maxRatio[0] = ratio;
                                }

                                if (sorting.size() != exact) {
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

                System.out.println(i + "," + maxRatio[0] + "," + sumRatios[0] / (factorial(i) - 1) + "," + ((factorial(i) - wrongAnswers[0]) / (double)factorial(i)) * 100 + "," + stopWatch.getTime());
            }
        }
    }

    static int factorial(final int n){
        if (n == 0)
            return 1;
        else
            return(n * factorial(n-1));
    }

    public static class BlockPolicy implements RejectedExecutionHandler {

        public BlockPolicy() { }

        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            try {
                executor.getQueue().put( r );
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static class Lock {

        private AtomicBoolean locked = new AtomicBoolean(false);

        public void lock() {
            while (!locked.compareAndSet(false, true));
        }

        public void unlock() {
            locked.set(false);
        }
    }
}
