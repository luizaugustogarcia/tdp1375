package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import org.apache.commons.lang3.tuple.Pair;
import org.jooq.Cursor;
import org.jooq.Record2;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ProofStorage {

    boolean isAlreadySorted(Pair<Configuration, Set<Integer>> configurationPair);

    boolean isBadCase(Pair<Configuration, Set<Integer>> configurationPair);

    boolean tryLock(Pair<Configuration, Set<Integer>> configurationPair);

    void unlock(Pair<Configuration, Set<Integer>> configurationPair);

    void markBadCase(Pair<Configuration, Set<Integer>> configurationPair);

    void saveSorting(Pair<Configuration, Set<Integer>> configurationPair, Pair<Configuration, Set<Integer>> parent, List<Cycle> sorting);

    void markNoSorting(final Pair<Configuration, Set<Integer>> configurationPair, final Pair<Configuration, Set<Integer>> parent);

    boolean markedNoSorting(Pair<Configuration, Set<Integer>> configurationPair);

    void saveComponentSorting(Configuration configuration, List<Cycle> cycles);

    Optional<List<Cycle>> findSorting(Pair<Configuration, Set<Integer>> configurationPair);

    Optional<List<Cycle>> findCompSorting(String spi);

    List<Pair<Configuration, Set<Integer>>> findAllNoSortings() throws SQLException;

    Cursor<Record2<String, String>> findAllSortings() throws SQLException;
}
