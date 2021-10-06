package br.unb.cic.tdp.proof.seq11_8;

import br.unb.cic.tdp.base.CommonOperations;
import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import com.google.common.base.Throwables;
import com.google.common.collect.Multimap;
import lombok.SneakyThrows;
import org.apache.commons.math3.util.Pair;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class ListCases {

//    public static void main(String[] args) throws IOException {
//        list("/home/luiz/Temp/tdp1375/dfs/", "/home/luiz/Projects/tdp1375/src/main/resources/cases/cases-dfs.txt");
//        list("/home/luiz/Temp/tdp1375/comb/", "/home/luiz/Projects/tdp1375/src/main/resources/cases/cases-comb.txt");
//    }

    public static void list(final String inputDir, final String outputFile) throws IOException {
        try (final var writer = new PrintWriter(outputFile)) {
            Files.list(new File(inputDir).toPath())
                    .forEach(path -> {
                        try {
                            final var reader = new BufferedReader(new FileReader(path.toFile()));
                            var line = reader.readLine();
                            MulticyclePermutation spi = null;
                            while ((line = reader.readLine()) != null) {
                                line = line.trim();

                                if (line.startsWith("<h6>")) {
                                    spi = new MulticyclePermutation(line.trim().replace("<h6>", "")
                                            .replace("</h6>", "").replace(" ", ","));
                                }
                                if (line.equals("THE EXTENSIONS ARE:")) {
                                    return;
                                }
                                final var sorting = new ArrayList<Cycle>();
                                if (line.trim().equals("ALLOWS (11/8)-SEQUENCE")) {
                                    while ((line = reader.readLine()) != null) {
                                        line = line.trim();

                                        if (!line.equals("<div style=\"margin-top: 10px; \">")) {
                                            continue;
                                        }

                                        line = reader.readLine();

                                        final var move = line.split(": ")[1].replace(" ", ",")
                                                .replace("<br>", "");
                                        sorting.add(Cycle.create(move));
                                    }
                                    writer.println(spi + "->" + sorting.toString());
                                }
                            }
                        } catch (IOException e) {
                            Throwables.propagate(e);
                        }
                    });
        }
    }

    @SneakyThrows
    public static void loadKnownSortings(final Path inputPath, final Multimap<Integer, Pair<Configuration, List<Cycle>>> knownSortings) {
        Files.list(inputPath).filter(f -> f.toFile().isDirectory()).flatMap(d -> Arrays.stream(d.toFile().listFiles()))
                .filter(f -> f.getName().contains("(") && f.getName().contains(")") && f.getName().endsWith(".html"))
                .forEach(file -> {
                    try {
                        final var spi = new MulticyclePermutation(file.getName().replace(".html", "").replace(" ", ","));
                        final var config = new Configuration(spi, CommonOperations.CANONICAL_PI[spi.getNumberOfSymbols()]);
                        final var sorting = new ArrayList<Cycle>();
                        final var reader = new BufferedReader(new FileReader(file));
                        var line = reader.readLine();
                        while ((line = reader.readLine()) != null) {
                            if (line.trim().equals("ALLOWS (11/8)-SEQUENCE")) {
                                while ((line = reader.readLine()) != null) {
                                    line = line.trim();

                                    if (!line.equals("<div style=\"margin-top: 10px; \">")) {
                                        continue;
                                    }

                                    line = reader.readLine();

                                    final var move = line.split(": ")[1].replace(" ", ",")
                                            .replace("<br>", "");
                                    sorting.add(Cycle.create(move));
                                }
                            }
                        }

                        knownSortings.put(config.hashCode(), new Pair<>(config, sorting));
                    } catch (IOException e) {
                        Throwables.propagate(e);
                    }
                });
    }
}
