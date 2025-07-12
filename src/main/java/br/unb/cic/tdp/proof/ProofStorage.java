package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.base.PivotedConfiguration;
import br.unb.cic.tdp.permutation.Cycle;
import org.jooq.Cursor;
import org.jooq.Record2;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface ProofStorage {

    boolean isAlreadySorted(PivotedConfiguration pivotedConfiguration);

    boolean isBadCase(PivotedConfiguration pivotedConfiguration);

    boolean tryLock(PivotedConfiguration pivotedConfiguration);

    void unlock(PivotedConfiguration pivotedConfiguration);

    void markBadCase(PivotedConfiguration pivotedConfiguration);

    void saveSorting(PivotedConfiguration pivotedConfiguration, PivotedConfiguration parent, List<Cycle> sorting);

    void markNoSorting(final PivotedConfiguration pivotedConfiguration, final PivotedConfiguration parent);

    boolean markedNoSorting(PivotedConfiguration pivotedConfiguration);

    void saveComponentSorting(Configuration configuration, List<Cycle> cycles);

    Optional<List<Cycle>> findSorting(PivotedConfiguration pivotedConfiguration);

    Optional<List<Cycle>> findCompSorting(String spi);

    List<PivotedConfiguration> findAllNoSortings() throws SQLException;

    Cursor<Record2<String, String>> findAllSortings() throws SQLException;
}
