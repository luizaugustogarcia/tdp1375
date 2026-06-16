package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.proof.seq11_8.Combinations;
import br.unb.cic.tdp.proof.seq11_8.Extensions;
import lombok.val;
import org.apache.velocity.app.Velocity;

import java.nio.file.Files;
import java.nio.file.Paths;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class ProofGenerator {

    public static void main(String[] args) throws Throwable {
        Velocity.setProperty("resource.loader", "class");
        Velocity.setProperty("parser.pool.size", Runtime.getRuntime().availableProcessors());
        Velocity.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        Velocity.init();

        Files.createDirectories(Paths.get(args[0]));

        val devicesCount = Integer.parseInt(args[1]);
        val slotsPerDevice = Integer.parseInt(args[2]);
        val queueBytesBudget = Long.parseLong(args[3]) * 1024 * 1024;
        val dedupTableSize = Integer.parseInt(args[4]);
        val maxRatio = Float.parseFloat(args[5]);
        val maxDepth = Integer.parseInt(args[6]);
        SortOrExtend.init(devicesCount, slotsPerDevice, queueBytesBudget, dedupTableSize, maxRatio, maxDepth);

        Files.copy(ProofGenerator.class.getClassLoader().getResourceAsStream("index.html"),
                Paths.get(args[0] + "/index.html"), REPLACE_EXISTING);
        Files.copy(ProofGenerator.class.getClassLoader().getResourceAsStream("explain.html"),
                Paths.get(args[0] + "/explain.html"), REPLACE_EXISTING);
        Files.copy(ProofGenerator.class.getClassLoader().getResourceAsStream("draw-config.js"),
                Paths.get(args[0] + "/draw-config.js"), REPLACE_EXISTING);

        Extensions.generate(args[0]);
        Combinations.generate(args[0]);
    }
}
