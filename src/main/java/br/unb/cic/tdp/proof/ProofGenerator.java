package br.unb.cic.tdp.proof;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.velocity.app.Velocity;

import java.nio.file.Files;
import java.nio.file.Paths;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Slf4j
public class ProofGenerator {

    public static void main(final String[] args) throws Throwable {
        Velocity.setProperty("resource.loader", "class");
        Velocity.setProperty("parser.pool.size", Runtime.getRuntime().availableProcessors());
        Velocity.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        Velocity.init();

        val outputDir = args[0];

        Files.createDirectories(Paths.get("%s/search".formatted(outputDir)));
        Files.createDirectories(Paths.get(outputDir));

        Files.copy(ProofGenerator.class.getClassLoader().getResourceAsStream("index.html"),
                Paths.get("%s/index.html".formatted(outputDir)), REPLACE_EXISTING);
        Files.copy(ProofGenerator.class.getClassLoader().getResourceAsStream("explain.html"),
                Paths.get("%s/explain.html".formatted(outputDir)), REPLACE_EXISTING);
        Files.copy(ProofGenerator.class.getClassLoader().getResourceAsStream("draw-config.js"),
                Paths.get("%s/draw-config.js".formatted(outputDir)), REPLACE_EXISTING);

        val minRate = Double.parseDouble(args[1]);
        log.info("Min rate: {}", minRate);

        //TwoCycles.generate(outputDir, minRate);
        Extensions.generate(outputDir, minRate);

        System.exit(0);
    }
}
