package br.unb.cic.tdp.proof;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.velocity.app.Velocity;

import java.nio.file.Files;
import java.nio.file.Paths;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.apache.commons.io.IOUtils.resourceToURL;

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

        val classLoader = ProofGenerator.class.getClassLoader();
        Files.copy(resourceToURL("index.html", classLoader).openStream(),
                Paths.get("%s/index.html".formatted(outputDir)), REPLACE_EXISTING);
        Files.copy(resourceToURL("explain.html", classLoader).openStream(),
                Paths.get("%s/explain.html".formatted(outputDir)), REPLACE_EXISTING);
        Files.copy(resourceToURL("draw-config.js", classLoader).openStream(),
                Paths.get("%s/draw-config.js".formatted(outputDir)), REPLACE_EXISTING);

        val minRate = Double.parseDouble(args[1]);
        log.info("Min rate: {}", minRate);

        TwoCycles.generate(outputDir, minRate);
        Extensions.generate(outputDir, minRate);

        System.exit(0);
    }
}
