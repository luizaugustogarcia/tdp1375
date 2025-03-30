package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.lang.NotImplementedException;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultProofStorage implements ProofStorage {
    private final Map<String, Boolean> working = new ConcurrentHashMap<>();
    private final Map<String, Boolean> sortedCache = Collections.synchronizedMap(new LRUMap(10_000));
    private final Map<String, Boolean> badCasesCache = Collections.synchronizedMap(new LRUMap(10_000));
    private final String outputDir;

    @SneakyThrows
    public DefaultProofStorage(final String outputDir) {
        this.outputDir = outputDir;

        val dfsDir = outputDir + "/dfs/";

        Files.createDirectories(Paths.get(dfsDir));
        Files.createDirectories(Paths.get(dfsDir + "/working/"));
        Files.createDirectories(Paths.get(dfsDir + "/bad-cases/"));
    }


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

    @Override
    public void saveSorting(final Configuration configuration, final Set<Integer> pivots, final List<Cycle> sorting) {
        throw new NotImplementedException();
    }

    @Override
    public void markNoSorting(Configuration configuration) {
        throw new NotImplementedException();
    }

    @Override
    public boolean hasNoSorting(Configuration configuration) {
        throw new NotImplementedException();
    }

    @Override
    public void saveComponentSorting(Configuration configuration, List<Cycle> cycles) {
        throw new NotImplementedException();
    }

    @Override
    public Optional<List<Cycle>> findBySorting(String spi) {
        throw new NotImplementedException();
    }
}
