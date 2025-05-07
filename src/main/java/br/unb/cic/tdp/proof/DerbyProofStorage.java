package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DerbyProofStorage implements ProofStorage {

    private HikariDataSource dataSource;

    @SneakyThrows
    public DerbyProofStorage(final String dbPath, final String database) {
        val config = new HikariConfig();

        config.setJdbcUrl("jdbc:derby:%s/db;databaseName=%s;create=true".formatted(dbPath, database));
        config.setDriverClassName("org.apache.derby.iapi.jdbc.AutoloadedDriver");
        config.setMaximumPoolSize(50);
        config.setConnectionTimeout(0);
        config.setAutoCommit(true);

        dataSource = new HikariDataSource(config);
        val runner = new QueryRunner(dataSource);

        try {
            runner.update("CREATE TABLE working (config VARCHAR(255) PRIMARY KEY)");
        } catch (SQLException ignored) {
        }
        try {
            runner.update("CREATE TABLE bad_case (config VARCHAR(255) PRIMARY KEY)");
        } catch (SQLException ignored) {
        }
        try {
            runner.update("CREATE TABLE sorting (config VARCHAR(255) PRIMARY KEY, hash_code INTEGER, sorting VARCHAR(255))");
        } catch (SQLException ignored) {
        }
        try {
            runner.update("CREATE TABLE no_sorting (config VARCHAR(255) PRIMARY KEY, parent VARCHAR(255))");
        } catch (SQLException ignored) {
        }
        try {
            runner.update("CREATE TABLE comp_sorting (config VARCHAR(255) PRIMARY KEY, hash_code INTEGER, sorting VARCHAR(255))");
        } catch (SQLException ignored) {
        }

        runner.update("DELETE FROM working");
        runner.update("DELETE FROM bad_case");
    }

    private static String getId(final Configuration configuration) {
        return configuration.getSpi().toString();
    }

    @SneakyThrows
    @Override
    public boolean isAlreadySorted(final Configuration configuration) {
        return new QueryRunner(dataSource).query(
                "SELECT COUNT(1) FROM sorting WHERE config = ?",
                new ScalarHandler<Integer>(1),
                getId(configuration)
        ) > 0;
    }

    @SneakyThrows
    @Override
    public boolean isBadCase(final Configuration configuration) {
        return new QueryRunner(dataSource).query(
                "SELECT 1 FROM bad_case WHERE config = ?",
                new ScalarHandler<Object>(),
                getId(configuration)
        ) != null;
    }

    @SneakyThrows
    @Override
    public boolean tryLock(final Configuration configuration) {
        val id = getId(configuration);
        try {
            new QueryRunner(dataSource).update("INSERT INTO working (config) VALUES (?)", id);
            return true;
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) return false;  // Primary key violation
            throw e;
        }
    }

    @SneakyThrows
    @Override
    public void unlock(final Configuration configuration) {
        new QueryRunner(dataSource).update("DELETE FROM working WHERE config = ?", getId(configuration));
    }

    @SneakyThrows
    @Override
    public void markBadCase(final Configuration configuration) {
        try {
            new QueryRunner(dataSource).update("INSERT INTO bad_case(config) VALUES (?)", getId(configuration));
        } catch (SQLException e) {
            if (!"23505".equals(e.getSQLState())) throw e;
        }
    }

    @SneakyThrows
    @Override
    public void saveSorting(final Configuration configuration, final List<Cycle> sorting) {
        try {
            new QueryRunner(dataSource).update("INSERT INTO sorting(config, hash_code, sorting) VALUES (?, ?, ?)",
                    getId(configuration), configuration.hashCode(), sorting.toString());
        } catch (SQLException e) {
            if (!"23505".equals(e.getSQLState())) throw e;
        }
    }

    @SneakyThrows
    @Override
    public void markNoSorting(final Configuration configuration, Configuration parent) {
        try {
            new QueryRunner(dataSource).update("INSERT INTO no_sorting(config, parent) VALUES (?, ?)",
                    getId(configuration), getId(parent));
        } catch (SQLException e) {
            if (!"23505".equals(e.getSQLState())) throw e;
        }
    }

    @SneakyThrows
    @Override
    public boolean markedNoSorting(final Configuration configuration) {
        return new QueryRunner(dataSource).query(
                "SELECT 1 FROM no_sorting WHERE config = ?",
                new ScalarHandler<>(),
                getId(configuration)
        ) != null;
    }

    @SneakyThrows
    @Override
    public void saveComponentSorting(final Configuration configuration, final List<Cycle> sorting) {
        try {
            new QueryRunner(dataSource).update("INSERT INTO comp_sorting(config, hash_code, sorting) VALUES (?, ?, ?)",
                    getId(configuration), configuration.hashCode(), sorting.toString());
        } catch (SQLException e) {
            if (!"23505".equals(e.getSQLState())) throw e;
        }
    }

    @SneakyThrows
    @Override
    public Optional<List<Cycle>> findSorting(final String spi) {
        return findSorting("SELECT * FROM sorting WHERE config = ?", spi);
    }

    private Optional<List<Cycle>> findSorting(String query, String spi) throws SQLException {
        val sorting = new QueryRunner(dataSource).query(query, new MapListHandler(), spi);
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
}
