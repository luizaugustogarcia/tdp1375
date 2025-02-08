package br.unb.cic.tdp.proof.seq11_8;

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
    private final LRUMap sortedCache = new LRUMap(10_000);
    private final LRUMap badCasesCache = new LRUMap(10_000);
    private final String outputDir;

    @Override
    public boolean isAlreadySorted(final Configuration configuration) {
        if (!sortedCache.containsKey(configuration)) {
            return new File(outputDir + "/working/" + configuration.getSpi()).exists();
        }
        return true;
    }

    @Override
    public boolean isBadCase(final Configuration configuration) {
        if (!badCasesCache.containsKey(configuration)) {
            return new File(outputDir + "/bad-cases/" + configuration.getSpi()).exists();
        }
        return true;
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
        working.remove(configuration);
        new File(outputDir + "/working/" + configuration.getSpi()).delete();
    }

    @SneakyThrows
    @Override
    public void markBadCase(final Configuration configuration) {
        badCasesCache.put(configuration, Boolean.TRUE);
        new File(outputDir + "/bad-cases/" + configuration.getSpi()).createNewFile();
    }

    @SneakyThrows
    @Override
    public void saveSorting(final Configuration extendedFrom,
                            final Configuration configuration,
                            final List<Cycle> sorting) {
        sortedCache.put(configuration, Boolean.TRUE);

        val file = new File(outputDir + "/" + configuration.getSpi() + ".html");
        try (val writer = new FileWriter(file)) {
            renderSorting(extendedFrom, configuration, sorting, writer);
        }
    }
}
