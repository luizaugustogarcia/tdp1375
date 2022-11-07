package br.unb.cic.tdp.proof.seq11_8;

import br.unb.cic.tdp.base.CommonOperations;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.util.Pair;
import com.google.common.base.Throwables;
import lombok.SneakyThrows;
import lombok.val;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static br.unb.cic.tdp.base.CommonOperations.is11_8;

public class ListCases {

    public static void main(String[] args) throws IOException {
        list("/home/luiz/Temp/proof1.375/dfs/", "/home/luiz/Projects/tdp1375/src/main/resources/cases/cases-dfs.txt");
        list("/home/luiz/Temp/proof1.375/comb/", "/home/luiz/Projects/tdp1375/src/main/resources/cases/cases-comb.txt");
    }

    public static void list(final String inputDir, final String outputFile) throws IOException {
        try (val writer = new PrintWriter(outputFile)) {
            val dir = new File(inputDir);
            File[] files = dir.listFiles(file -> file.getName().endsWith(".html"));

            if (files != null && files.length > 0)
                Arrays.stream(files)
                        .forEach(file -> {
                            try {
                                val reader = new BufferedReader(new FileReader(file));
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
                                    val sorting = new ArrayList<Cycle>();
                                    if (line.trim().equals("ALLOWS (11/8)-SEQUENCE")) {
                                        while ((line = reader.readLine()) != null) {
                                            line = line.trim();

                                            if (!line.equals("<div style=\"margin-top: 10px; \">")) {
                                                continue;
                                            }

                                            line = reader.readLine();

                                            val move = line.split(": ")[1].replace(" ", ",")
                                                    .replace("<br>", "");
                                            sorting.add(Cycle.create(move));
                                        }
                                        writer.println(spi + "->" + sorting);
                                        is11_8(spi, CommonOperations.CANONICAL_PI[spi.getNumberOfSymbols()], sorting);
                                    }
                                }
                            } catch (IOException e) {
                                Throwables.propagate(e);
                            }
                        });
        }
    }

    @SneakyThrows
    public static Pair<MulticyclePermutation, List<Cycle>> getSorting(final Path path) {
        val reader = new BufferedReader(new FileReader(path.toFile()));
        var line = reader.readLine();
        MulticyclePermutation spi = null;

        while ((line = reader.readLine()) != null) {
            line = line.trim();

            if (line.startsWith("<h6>")) {
                spi = new MulticyclePermutation(line.trim().replace("<h6>", "")
                        .replace("</h6>", "").replace(" ", ","));
            }

            if (line.equals("THE EXTENSIONS ARE:")) {
                return new Pair<>(spi, null);
            }

            val sorting = new ArrayList<Cycle>();
            if (line.trim().equals("ALLOWS (11/8)-SEQUENCE")) {
                while ((line = reader.readLine()) != null) {
                    line = line.trim();

                    if (!line.equals("<div style=\"margin-top: 10px; \">")) {
                        continue;
                    }

                    line = reader.readLine();

                    val move = line.split(": ")[1].replace(" ", ",")
                            .replace("<br>", "");
                    sorting.add(Cycle.create(move));
                }
                return new Pair<>(spi, sorting);
            }
        }

        throw new IllegalStateException("Invalid file " + path.toFile());
    }
}
