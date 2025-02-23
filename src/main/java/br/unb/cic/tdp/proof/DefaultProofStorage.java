package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.commons.collections.map.LRUMap;

import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static br.unb.cic.tdp.proof.ProofGenerator.renderSorting;

@RequiredArgsConstructor
public class DefaultProofStorage implements ProofStorage {
    private final Map<Configuration, Boolean> working = new ConcurrentHashMap<>();
    private final Map<Configuration, Boolean> sortedCache = new LRUMap(10_000);
    private final Map<Configuration, Boolean> badCasesCache = new LRUMap(10_000);
    private final String outputDir;

    @Override
    public boolean isAlreadySorted(final Configuration configuration) {
        return sortedCache.computeIfAbsent(configuration, c -> new File(outputDir + "/" + configuration.getSpi() + ".html").exists());
    }

    @Override
    public boolean isBadCase(final Configuration configuration) {
        return badCasesCache.computeIfAbsent(configuration, c -> new File(outputDir + "/bad-cases/" + configuration.getSpi()).exists());
    }

    @SneakyThrows
    @Override
    public boolean tryLock(final Configuration configuration) {
        val locked = working.putIfAbsent(configuration, Boolean.TRUE) == null;
        if (locked) {
            new File(outputDir + "/working/" + configuration.getSpi()).createNewFile();
        }
        return locked;
    }

    @SneakyThrows
    @Override
    public void unlock(final Configuration configuration) {
        new File(outputDir + "/working/" + configuration.getSpi()).delete();
        working.remove(configuration);
    }

    @SneakyThrows
    @Override
    public void markBadCase(final Configuration configuration) {
        new File(outputDir + "/bad-cases/" + configuration.getSpi()).createNewFile();
        badCasesCache.put(configuration, Boolean.TRUE);
    }

    @SneakyThrows
    @Override
    public void saveSorting(final Configuration extendedFrom,
                            final Configuration configuration,
                            final List<Cycle> sorting) {
        val file = new File(outputDir + "/" + configuration.getSpi() + ".html");
        try (val writer = new FileWriter(file)) {
            renderSorting(extendedFrom, configuration, sorting, writer);
        }
        sortedCache.put(configuration, Boolean.TRUE);
    }
}
