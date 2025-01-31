package br.unb.cic.tdp.proof.util;

import br.unb.cic.tdp.base.Configuration;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;

import java.io.File;
import java.io.FileWriter;
import java.io.RandomAccessFile;
import java.util.concurrent.RecursiveAction;

import static br.unb.cic.tdp.proof.ProofGenerator.renderSorting;
import static br.unb.cic.tdp.proof.ProofGenerator.searchForSorting;

@AllArgsConstructor
public abstract class SortOrExtend extends RecursiveAction {
    protected final Configuration extendedFrom;
    protected final Configuration configuration;
    protected final String outputDir;

    @Override
    protected void compute() {
        try {
            val canonical = configuration.getCanonical();

            val sortingFile = new File(outputDir + "/" + canonical.getSpi() + ".html");
            if (sortingFile.exists()) {
                // if it's already sorted, return
                return;
            }

            val badCaseFile = new File(outputDir + "/bad-cases/" + canonical.getSpi());

            if (!badCaseFile.exists()) {
                try {
                    try (val workingFile = new RandomAccessFile(new File(outputDir + "/working/" + canonical.getSpi()), "rw")) {
                        var buffer = new StringBuilder();
                        while (workingFile.getFilePointer() < workingFile.length()) {
                            buffer.append(workingFile.readLine());
                        }

                        if (buffer.toString().equals("working")) {
                            // some thread is already working on this case, skipping
                            return;
                        }

                        workingFile.write("working".getBytes());

                        val sorting = searchForSorting(canonical);
                        if (sorting.isPresent()) {
                            try (val file = new RandomAccessFile(outputDir + "/" + canonical.getSpi() + ".html", "rw")) {
                                try (val writer = new FileWriter(file.getFD())) {
                                    renderSorting(extendedFrom, canonical, sorting.get(), writer);
                                    return;
                                }
                            }
                        } else {
                            try (val writer = new FileWriter(outputDir + "/bad-cases/" + canonical.getSpi())) {
                                // create the bad case
                            }
                        }
                    }
                } finally {
                    new File(outputDir + "/working/" + canonical.getSpi()).delete();
                }
            }

            extend(configuration);
        } catch (final Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    protected abstract void extend(Configuration configuration);
}