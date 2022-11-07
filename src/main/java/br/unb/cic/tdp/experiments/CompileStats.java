package br.unb.cic.tdp.experiments;

import lombok.SneakyThrows;
import lombok.val;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;

public class CompileStats {

    @SneakyThrows
    public static void main(String[] args) {
        for (var i = 2; i <= 50; i++) {
            val resource = LongerPermutations.class.getResource("/stats/stats" + i * 10 + ".txt");
            val path = Paths.get(resource.toURI());

            final float[] maxRatioSilva = {0};
            final float[] maxRatioEh = {0};

            final BigDecimal[] sumRatiosSilva = {BigDecimal.ZERO};

            final BigDecimal[] sumRatiosEh = {BigDecimal.ZERO};

            Files.lines(path).forEach(line -> {
                val split = line.split(",");

                val ratioSilva = Float.parseFloat(split[1]) / Float.parseFloat(split[0]);
                if (ratioSilva > maxRatioSilva[0]) {
                    maxRatioSilva[0] = ratioSilva;
                }

                sumRatiosSilva[0] = sumRatiosSilva[0].add(new BigDecimal(ratioSilva));

                val ratioEh = Float.parseFloat(split[3]) / Float.parseFloat(split[0]);
                if (ratioEh > maxRatioEh[0]) {
                    maxRatioEh[0] = ratioEh;
                }

                sumRatiosEh[0] = sumRatiosEh[0].add(new BigDecimal(ratioEh));
            });

            System.out.println(i * 10 + "," +
                    maxRatioSilva[0] + "," +
                    sumRatiosSilva[0].floatValue() / 1000 + "," +
                    maxRatioEh[0] + "," +
                    sumRatiosEh[0].floatValue() / 1000);
        }

        System.out.println(">>>>>");

        for (int i = 2; i <= 50; i++) {
            val resource = LongerPermutations.class.getResource("/stats/stats" + i * 10 + ".txt");
            val path = Paths.get(resource.toURI());

            final int[] timeTotalSilva = {0};

            final int[] timeTotalEh = {0};

            final int[] maxEh = {0};

            final int[] maxSilva = {0};

            Files.lines(path).forEach(line -> {
                val split = line.split(",");

                if (Integer.parseInt(split[2]) > maxSilva[0]) {
                    maxSilva[0] = Integer.parseInt(split[2]);
                }

                if (Integer.parseInt(split[2]) > maxEh[0]) {
                    maxEh[0] = Integer.parseInt(split[2]);
                }

                timeTotalSilva[0] += Integer.parseInt(split[2]);
                timeTotalEh[0] += Integer.parseInt(split[4]);
            });

            System.out.println(i * 10 + "," +
                    (float) timeTotalSilva[0] / 1000 / 60 + "," +
                    (float) timeTotalEh[0] / 1000 / 60  + "," +
                    maxSilva[0]  + "," +
                    maxEh[0]);
        }
    }

}
