package br.unb.cic.tdp.proof.util;

import br.unb.cic.tdp.base.Configuration;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

import java.io.File;
import java.io.FileWriter;
import java.io.RandomAccessFile;
import java.util.concurrent.RecursiveAction;

import static br.unb.cic.tdp.proof.ProofGenerator.renderSorting;
import static br.unb.cic.tdp.proof.ProofGenerator.searchForSorting;

@AllArgsConstructor
public abstract class SortOrExtend extends RecursiveAction {
    protected final Configuration configuration;
    protected final String outputDir;

    @SneakyThrows
    @Override
    protected void compute() {
        final var canonical = configuration.getCanonical();

        final var sortingFile = new File(outputDir + "/" + canonical.getSpi() + ".html");
        if (sortingFile.exists()) {
            // if it's already sorted, return
            return;
        }

        final var badCaseFile = new File(outputDir + "/bad-cases/" + canonical.getSpi());

        if (!badCaseFile.exists()) {
            try {
                try (final var workingFile = new RandomAccessFile(new File(outputDir + "/working/" + canonical.getSpi()), "rw")) {
                    final var buffer = new StringBuffer();
                    while (workingFile.getFilePointer() < workingFile.length()) {
                        buffer.append(workingFile.readLine());
                    }

                    if (buffer.toString().equals("working")) {
                        // some thread already is working on this case, skipping
                        return;
                    }

                    workingFile.write("working".getBytes());

                    final var sorting = searchForSorting(canonical);
                    if (sorting.isPresent()) {
                        try (final var file = new RandomAccessFile(outputDir + "/" + canonical.getSpi() + ".html", "rw")) {
                            try (final var writer = new FileWriter(file.getFD())) {
                                renderSorting(canonical, sorting.get(), writer);
                                return;
                            }
                        }
                    } else {
                        try (final var writer = new FileWriter(outputDir + "/bad-cases/" + canonical.getSpi())) {
                            // create the base case
                        }
                    }
                }
            } finally {
                new File(outputDir + "/working/" + canonical.getSpi()).delete();
            }
        }

        extend(configuration);
    }

    protected abstract void extend(Configuration configuration);
}