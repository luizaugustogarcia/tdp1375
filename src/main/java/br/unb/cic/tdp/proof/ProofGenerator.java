package br.unb.cic.tdp.proof;

import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
public class ProofGenerator {

    public static void main(final String[] args) throws Throwable {
        val outputDir = args[0];

        Files.createDirectories(Paths.get(outputDir));

        val minRate = Double.parseDouble(args[1]);
        log.info("Min rate: {}", minRate);

        //TwoCycles.generate(outputDir, minRate);
        Extensions.generate(outputDir, minRate);

        System.exit(0);
    }
}
