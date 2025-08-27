package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.base.PivotedConfiguration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.util.ShortArrayPacker;
import com.google.common.collect.Sets;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.openhft.chronicle.map.ChronicleMap;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static br.unb.cic.tdp.base.PivotedConfiguration.of;

/**
 * ProofStorage implementation backed by Chronicle Map.
 */
@Slf4j
public class ChronicleMapProofStorage implements ProofStorage {

    private final ConcurrentHashMap<String, Boolean> working = new ConcurrentHashMap<>();
    private final ChronicleMap<String, byte[]> sortingMap;
    private final ChronicleMap<String, String> noSortingMap;
    private final ChronicleMap<String, String> compSortingMap;

    @SneakyThrows
    public ChronicleMapProofStorage(final String dir, final String prefix) {
        File baseDir = new File(dir);
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            throw new IOException("Could not create directory " + dir);
        }

        sortingMap = ChronicleMap
                .of(String.class, byte[].class)
                .name(prefix + "sorting")
                .entries(1_000_000)
                .createPersistedTo(new File(baseDir, prefix + "sorting.dat"));

        noSortingMap = ChronicleMap
                .of(String.class, String.class)
                .name(prefix + "nosorting")
                .entries(1_000_000)
                .createPersistedTo(new File(baseDir, prefix + "nosorting.dat"));

        compSortingMap = ChronicleMap
                .of(String.class, String.class)
                .name(prefix + "compsorting")
                .entries(1_000_000)
                .createPersistedTo(new File(baseDir, prefix + "compsorting.dat"));
    }

    private static String getId(final PivotedConfiguration pivotedConfiguration) {
        return pivotedConfiguration.toString();
    }

    @Override
    public boolean isAlreadySorted(final PivotedConfiguration pivotedConfiguration) {
        return sortingMap.containsKey(pivotedConfiguration.getPackedSpi() + ":" + pivotedConfiguration.getPackedPivots());
    }

    @Override
    public boolean tryLock(final PivotedConfiguration pivotedConfiguration) {
        return working.putIfAbsent(getId(pivotedConfiguration), Boolean.TRUE) == null;
    }

    @Override
    public void unlock(final PivotedConfiguration pivotedConfiguration) {
        working.remove(getId(pivotedConfiguration));
    }

    @Override
    public void saveSorting(final PivotedConfiguration pivotedConfiguration, final PivotedConfiguration parent, final List<Cycle> sorting) {
        sortingMap.putIfAbsent(
                pivotedConfiguration.getPackedSpi() + ":" + pivotedConfiguration.getPackedPivots(),
                ShortArrayPacker.encode(flatten(sorting.stream().map(Cycle::getSymbols).toList()))
        );
    }

    private static short[] flatten(List<int[]> list) {
        int totalLength = list.size() * 3;
        short[] result = new short[totalLength];
        int pos = 0;
        for (int[] arr : list) {
            for (int val : arr) {
                result[pos++] = (short) val;
            }
        }
        return result;
    }

    @Override
    public void markNoSorting(final PivotedConfiguration pivotedConfiguration, final PivotedConfiguration parent) {
        noSortingMap.putIfAbsent(getId(pivotedConfiguration), getId(parent));
    }

    @Override
    public boolean markedNoSorting(final PivotedConfiguration pivotedConfiguration) {
        return noSortingMap.containsKey(getId(pivotedConfiguration));
    }

    @Override
    public void saveComponentSorting(final Configuration configuration, final List<Cycle> sorting) {
        compSortingMap.putIfAbsent(getId(of(configuration, Sets.newTreeSet())), sorting.toString());
    }

    @Override
    public Optional<List<Cycle>> findSorting(final PivotedConfiguration pivotedConfiguration) {
        byte[] bytes = sortingMap.get(pivotedConfiguration.getPackedSpi() + ":" + pivotedConfiguration.getPackedPivots());
        if (bytes == null) {
            return Optional.empty();
        }
        short[] decoded = ShortArrayPacker.decode(bytes);
        List<Cycle> cycles = new ArrayList<>();
        for (int i = 0; i < decoded.length; i += 3) {
            cycles.add(Cycle.of(decoded[i], decoded[i + 1], decoded[i + 2]));
        }
        return Optional.of(cycles);
    }

    @Override
    public Optional<List<Cycle>> findCompSorting(final String spi) {
        String s = compSortingMap.get(spi);
        if (s == null) {
            return Optional.empty();
        }
        List<Cycle> cycles = Arrays.stream(s.replace("[", "").replace("]", "").split(", "))
                .map(Cycle::of)
                .toList();
        return Optional.of(cycles);
    }
}
