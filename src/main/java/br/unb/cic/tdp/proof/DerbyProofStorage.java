package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.util.SingleConnectionDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.AbstractListHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.apache.commons.lang3.tuple.Pair;
import org.jooq.Cursor;
import org.jooq.Record2;
import org.jooq.impl.DSL;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

public class DerbyProofStorage implements ProofStorage {
    private static final ThreadLocal<Map<String, QueryRunner>> CONNECTION = new ThreadLocal<>();

    private final ConcurrentHashMap<String, Boolean> working = new ConcurrentHashMap<>();
    private final HikariDataSource dataSource;
    private final String database;

    @SneakyThrows
    public DerbyProofStorage(final String dbPath, final String database) {
        val config = new HikariConfig();

        System.setProperty("derby.storage.pageCacheSize", "100000");

        this.database = database;
        config.setJdbcUrl("jdbc:derby:%s/%s;create=true".formatted(dbPath, this.database));
        config.setDriverClassName("org.apache.derby.iapi.jdbc.AutoloadedDriver");
        config.setMaximumPoolSize(50);
        config.setConnectionTimeout(0);
        config.setAutoCommit(true);

        dataSource = new HikariDataSource(config);
        val runner = getQueryRunner();

        try {
            runner.update("CREATE TABLE working (config VARCHAR(255) PRIMARY KEY)");
        } catch (SQLException ignored) {
        }
        try {
            runner.update("CREATE TABLE bad_case (config VARCHAR(255) PRIMARY KEY)");
        } catch (SQLException ignored) {
        }
        try {
            runner.update("CREATE TABLE sorting (config VARCHAR(255) PRIMARY KEY, parent VARCHAR(255), sorting VARCHAR(255))");
        } catch (SQLException ignored) {
        }
        try {
            runner.update("CREATE TABLE no_sorting (config VARCHAR(255) PRIMARY KEY, parent VARCHAR(255))");
        } catch (SQLException ignored) {
        }
        try {
            runner.update("CREATE TABLE comp_sorting (config VARCHAR(255) PRIMARY KEY, sorting VARCHAR(255))");
        } catch (SQLException ignored) {
        }

        runner.update("DELETE FROM working");
        runner.update("DELETE FROM bad_case");
    }

    private static String getId(final Pair<Configuration, Set<Integer>> configurationPair) {
        return "%s#%s".formatted(configurationPair.getLeft().getSpi(), configurationPair.getRight());
    }

    @SneakyThrows
    @Override
    public boolean isAlreadySorted(final Pair<Configuration, Set<Integer>> configurationPair) {
        return getQueryRunner().query(
                "SELECT COUNT(1) FROM sorting WHERE config = ?",
                new ScalarHandler<Integer>(1),
                getId(configurationPair)
        ) > 0;
    }

    @SneakyThrows
    @Override
    public boolean isBadCase(final Pair<Configuration, Set<Integer>> configurationPair) {
        return getQueryRunner().query(
                "SELECT 1 FROM bad_case WHERE config = ?",
                new ScalarHandler<Object>(),
                getId(configurationPair)
        ) != null;
    }

    private QueryRunner getQueryRunner() throws SQLException {
        if (CONNECTION.get() == null) {
            CONNECTION.set(new HashMap<>());
        }

        return CONNECTION.get().computeIfAbsent(database, database ->
        {
            try {
                return new QueryRunner(new SingleConnectionDataSource(dataSource.getConnection()));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @SneakyThrows
    @Override
    public boolean tryLock(final Pair<Configuration, Set<Integer>> configurationPair) {
        return working.putIfAbsent(getId(configurationPair), Boolean.TRUE) == null;
    }

    @SneakyThrows
    @Override
    public void unlock(final Pair<Configuration, Set<Integer>> configurationPair) {
        working.remove(getId(configurationPair));
    }

    @SneakyThrows
    @Override
    public void markBadCase(final Pair<Configuration, Set<Integer>> configuration) {
        getQueryRunner().update("INSERT INTO bad_case(config) VALUES (?)", getId(configuration));
    }

    @SneakyThrows
    @Override
    public void saveSorting(final Pair<Configuration, Set<Integer>> configurationPair, Pair<Configuration, Set<Integer>> parent, final List<Cycle> sorting) {
        try {
            getQueryRunner().update("INSERT INTO sorting(config, parent, sorting) VALUES (?, ?, ?)",
                    getId(configurationPair), getId(parent), sorting.toString());
        } catch (SQLException e) {
            if (!"23505".equals(e.getSQLState())) throw e;
        }
    }

    @SneakyThrows
    @Override
    public void markNoSorting(final Pair<Configuration, Set<Integer>> configurationPair, final Pair<Configuration, Set<Integer>> parent) {
        try {
            getQueryRunner().update("INSERT INTO no_sorting(config, parent) VALUES (?, ?)",
                    getId(configurationPair), getId(parent));
        } catch (SQLException e) {
            if (!"23505".equals(e.getSQLState())) throw e;
        }
    }

    @SneakyThrows
    @Override
    public boolean markedNoSorting(final Pair<Configuration, Set<Integer>> configurationPair) {
        return getQueryRunner().query(
                "SELECT 1 FROM no_sorting WHERE config = ?",
                new ScalarHandler<>(),
                getId(configurationPair)
        ) != null;
    }

    @SneakyThrows
    @Override
    public void saveComponentSorting(final Configuration configuration, final List<Cycle> sorting) {
        try {
            getQueryRunner().update("INSERT INTO comp_sorting(config, sorting) VALUES (?, ?)",
                    getId(Pair.of(configuration, Set.of())), sorting.toString());
        } catch (SQLException e) {
            if (!"23505".equals(e.getSQLState())) throw e;
        }
    }

    @SneakyThrows
    @Override
    public Optional<List<Cycle>> findSorting(final Pair<Configuration, Set<Integer>> configurationPair) {
        return findSorting("SELECT * FROM sorting WHERE config = ?", getId(configurationPair));
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
        return findSorting("SELECT * FROM comp_sorting WHERE config = ?", spi);
    }

    @Override
    public List<Pair<Configuration, Set<Integer>>> findAllNoSortings() throws SQLException {
        return getQueryRunner().query(
                "SELECT config FROM no_sorting",
                new AbstractListHandler<>() {

                    @Override
                    protected Pair<Configuration, Set<Integer>> handleRow(final ResultSet resultSet) throws SQLException {
                        val config = resultSet.getString("config").split("#");
                        return Pair.of(
                                new Configuration(config[0]),
                                Arrays.stream(config[1].replaceAll("[\\[\\]\\s]", "").split(","))
                                        .map(Integer::parseInt)
                                        .collect(Collectors.toCollection(TreeSet::new))
                        );
                    }
                }
        );
    }

    @Override
    public Cursor<Record2<String, String>> findAllSortings() throws SQLException {
        return DSL.using(dataSource.getConnection())
                .select(field("config", String.class), field("sorting", String.class))
                .from(table("sorting"))
                .fetchLazy();
    }
}