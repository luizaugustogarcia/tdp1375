package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.base.PivotedConfiguration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.util.SingleConnectionDataSource;
import com.google.common.collect.Sets;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static br.unb.cic.tdp.base.PivotedConfiguration.of;

@Slf4j
public class Backup implements ProofStorage {
    private static final ThreadLocal<Map<String, QueryRunner>> CONNECTION = new ThreadLocal<>();

    private final ConcurrentHashMap<String, Boolean> working = new ConcurrentHashMap<>();
    private final HikariDataSource dataSource;
    private final String tablePrefix;

    private final String dbPath;

    @SneakyThrows
    public Backup(final String dbPath, final String tablePrefix) {
        this.tablePrefix = tablePrefix;
        this.dbPath = dbPath;

        val config = new HikariConfig();

        config.setJdbcUrl("jdbc:derby:%s/%s;create=true".formatted(dbPath, "extensions"));
        config.setDriverClassName("org.apache.derby.iapi.jdbc.AutoloadedDriver");
        config.setMaximumPoolSize(50);
        config.setConnectionTimeout(0);
        config.setAutoCommit(true);

        dataSource = new HikariDataSource(config);
        val runner = getQueryRunner();

        try {
            runner.update("CREATE TABLE %ssorting (config VARCHAR(255) PRIMARY KEY, parent VARCHAR(255), sorting VARCHAR(255))".formatted(tablePrefix));
        } catch (final SQLException ignored) {
            log.error("Table already exists");
        }
        try {
            runner.update("CREATE TABLE %sno_sorting (config VARCHAR(255) PRIMARY KEY, parent VARCHAR(255))".formatted(tablePrefix));
        } catch (final SQLException ignored) {
            log.error("Table already exists");
        }
        try {
            runner.update("CREATE TABLE %scomp_sorting (config VARCHAR(255) PRIMARY KEY, sorting VARCHAR(255))".formatted(tablePrefix));
        } catch (final SQLException ignored) {
            log.error("Table already exists");
        }
    }

    private static String getId(final PivotedConfiguration pivotedConfiguration) {
        return pivotedConfiguration.toString();
    }

    @SneakyThrows
    @Override
    public boolean isAlreadySorted(final PivotedConfiguration pivotedConfiguration) {
        return getQueryRunner().query(
                "SELECT COUNT(1) FROM %ssorting WHERE config = ?".formatted(tablePrefix),
                new ScalarHandler<Integer>(1),
                getId(pivotedConfiguration)
        ) > 0;
    }

    private QueryRunner getQueryRunner() throws SQLException {
        if (CONNECTION.get() == null) {
            CONNECTION.set(new HashMap<>());
        }

        return CONNECTION.get().computeIfAbsent(dbPath, database ->
        {
            try {
                return new QueryRunner(new SingleConnectionDataSource(dataSource.getConnection()));
            } catch (final SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @SneakyThrows
    @Override
    public boolean tryLock(final PivotedConfiguration pivotedConfiguration) {
        return working.putIfAbsent(getId(pivotedConfiguration), Boolean.TRUE) == null;
    }

    @SneakyThrows
    @Override
    public void unlock(final PivotedConfiguration pivotedConfiguration) {
        working.remove(getId(pivotedConfiguration));
    }

    @SneakyThrows
    @Override
    public void saveSorting(final PivotedConfiguration pivotedConfiguration, PivotedConfiguration parent, final List<Cycle> sorting) {
        try {
            getQueryRunner().update("INSERT INTO %ssorting(config, parent, sorting) VALUES (?, ?, ?)".formatted(tablePrefix),
                    getId(pivotedConfiguration), getId(parent), sorting.toString());
        } catch (final SQLException e) {
            if (!"23505".equals(e.getSQLState())) throw e;
        }
    }

    @SneakyThrows
    @Override
    public void markNoSorting(final PivotedConfiguration pivotedConfiguration, final PivotedConfiguration parent) {
        try {
            getQueryRunner().update("INSERT INTO %sno_sorting(config, parent) VALUES (?, ?)".formatted(tablePrefix),
                    getId(pivotedConfiguration), getId(parent));
        } catch (final SQLException e) {
            if (!"23505".equals(e.getSQLState())) throw e;
        }
    }

    @SneakyThrows
    @Override
    public boolean markedNoSorting(final PivotedConfiguration pivotedConfiguration) {
        return getQueryRunner().query(
                "SELECT 1 FROM %sno_sorting WHERE config = ?".formatted(tablePrefix),
                new ScalarHandler<>(),
                getId(pivotedConfiguration)
        ) != null;
    }

    @SneakyThrows
    @Override
    public void saveComponentSorting(final Configuration configuration, final List<Cycle> sorting) {
        try {
            getQueryRunner().update("INSERT INTO %scomp_sorting(config, sorting) VALUES (?, ?)".formatted(tablePrefix),
                    getId(of(configuration, Sets.newTreeSet())), sorting.toString());
        } catch (final SQLException e) {
            if (!"23505".equals(e.getSQLState())) throw e;
        }
    }

    @SneakyThrows
    @Override
    public Optional<List<Cycle>> findSorting(final PivotedConfiguration pivotedConfiguration) {
        return findSorting("SELECT * FROM %ssorting WHERE config = ?".formatted(tablePrefix), getId(pivotedConfiguration));
    }

    private Optional<List<Cycle>> findSorting(String query, String spi) throws SQLException {
        val sorting = getQueryRunner().query(query, new MapListHandler(), spi);
        return sorting.stream()
                .map(map -> Arrays.stream(
                                map.get("sorting").toString().replace("[", "").replace("]", "").split(", "))
                        .map(Cycle::of)
                        .collect(Collectors.toList()))
                .findFirst();
    }

    @SneakyThrows
    @Override
    public Optional<List<Cycle>> findCompSorting(final String spi) {
        return findSorting("SELECT * FROM %scomp_sorting WHERE config = ?".formatted(tablePrefix), spi);
    }
}