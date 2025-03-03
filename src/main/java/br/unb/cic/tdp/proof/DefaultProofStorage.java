package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.commons.collections.map.LRUMap;

import java.io.File;
import java.io.FileWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static br.unb.cic.tdp.proof.ProofGenerator.renderSorting;

@RequiredArgsConstructor
public class DefaultProofStorage implements ProofStorage {
    private final Map<String, Boolean> working = new ConcurrentHashMap<>();
    private final Map<String, Boolean> sortedCache = Collections.synchronizedMap(new LRUMap(10_000));
    private final Map<String, Boolean> badCasesCache = Collections.synchronizedMap(new LRUMap(10_000));
    private final String outputDir;

    @Override
    public boolean isAlreadySorted(final Configuration configuration) {
        return sortedCache.computeIfAbsent(getId(configuration), c -> new File(outputDir + "/" + getId(configuration) + ".html").exists());
    }

    @Override
    public boolean isBadCase(final Configuration configuration) {
        return badCasesCache.computeIfAbsent(getId(configuration), c -> new File(outputDir + "/bad-cases/" + getId(configuration)).exists());
    }

    private static String getId(final Configuration configuration) {
        return configuration.getSpi() + "#" + configuration.getPi();
    }

    @SneakyThrows
    @Override
    public boolean tryLock(final Configuration configuration) {
        val locked = working.putIfAbsent(getId(configuration), Boolean.TRUE) == null;
        if (locked) {
            new File(outputDir + "/working/" + getId(configuration)).createNewFile();
        }
        return locked;
    }

    @SneakyThrows
    @Override
    public void unlock(final Configuration configuration) {
        new File(outputDir + "/working/" + getId(configuration)).delete();
        working.remove(configuration);
    }

    @SneakyThrows
    @Override
    public void markBadCase(final Configuration configuration) {
        new File(outputDir + "/bad-cases/" + getId(configuration)).createNewFile();
        badCasesCache.put(getId(configuration), Boolean.TRUE);
    }

    @SneakyThrows
    @Override
    public void saveSorting(final Configuration configuration,
                            final List<Cycle> sorting) {
        val file = new File(outputDir + "/" + getId(configuration) + ".html");
        try (val writer = new FileWriter(file)) {
            renderSorting(configuration, sorting, writer);
        }
        sortedCache.put(getId(configuration), Boolean.TRUE);
    }
}
