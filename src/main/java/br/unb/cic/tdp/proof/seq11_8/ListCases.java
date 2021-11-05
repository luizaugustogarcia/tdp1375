package br.unb.cic.tdp.proof.seq11_8;

import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import com.google.common.base.Throwables;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;

public class ListCases {

    public static void main(String[] args) throws IOException {
        list("C:\\Users\\Luiz Silva\\Temp\\proof1.375\\dfs\\", "C:\\Users\\Luiz Silva\\Projects\\tdp1375\\src\\main\\resources\\cases\\cases-dfs.txt");
        list("C:\\Users\\Luiz Silva\\Temp\\proof1.375\\comb\\", "C:\\Users\\Luiz Silva\\Projects\\tdp1375\\src\\main\\resources\\cases\\cases-comb.txt");
    }

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
}
