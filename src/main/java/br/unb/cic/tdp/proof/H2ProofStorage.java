package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class H2ProofStorage implements ProofStorage {

    private final BasicDataSource dataSource;

    @SneakyThrows
    public H2ProofStorage(final String path) {
        dataSource = new BasicDataSource();
        dataSource.setUrl("jdbc:h2:file:%s;DB_CLOSE_DELAY=-1;CACHE_SIZE=100000".formatted(path));
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setMaxTotal(Runtime.getRuntime().availableProcessors());

        val queryRunner = new QueryRunner(dataSource);

        queryRunner.update("CREATE TABLE IF NOT EXISTS working (config VARCHAR(255) PRIMARY KEY);");
        queryRunner.update("CREATE TABLE IF NOT EXISTS bad_case (config VARCHAR(255) PRIMARY KEY);");
        queryRunner.update("CREATE TABLE IF NOT EXISTS sorting (config VARCHAR(255), hash_code INT, sorting VARCHAR(255), PRIMARY KEY (config));");
        queryRunner.update("CREATE TABLE IF NOT EXISTS no_sorting (config VARCHAR(255) PRIMARY KEY, parent VARCHAR(255));");
        queryRunner.update("CREATE TABLE IF NOT EXISTS comp_sorting (config VARCHAR(255) PRIMARY KEY, hash_code INT, sorting VARCHAR(255));");

        queryRunner.update("DELETE FROM working;");
        queryRunner.update("DELETE FROM bad_case;");
    }

    private static String getId(final Configuration configuration) {
        return configuration.getSpi().toString();
    }

    @SneakyThrows
    @Override
    public boolean isAlreadySorted(final Configuration configuration) {
        return new QueryRunner(dataSource).query(
                "SELECT COUNT(1) FROM sorting WHERE config = ?",
                new ScalarHandler<Long>(1),
                getId(configuration)
        ) > 0;
    }

    @SneakyThrows
    @Override
    public boolean isBadCase(final Configuration configuration) {
        return new QueryRunner(dataSource).query(
                "SELECT 1 FROM bad_case WHERE config = ?",
                new ScalarHandler<String>(1),
                getId(configuration)
        ) != null;
    }

    @SneakyThrows
    @Override
    public boolean tryLock(final Configuration configuration) {
        val sql = "MERGE INTO working (config) KEY(config) VALUES (?)";
        val id = getId(configuration);
        try {
            return new QueryRunner(dataSource).update(sql, id) > 0;
        } catch (SQLException e) {
            return false;
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
        new QueryRunner(dataSource).update("MERGE INTO bad_case(config) KEY(config) VALUES (?)", getId(configuration));
    }

    @SneakyThrows
    @Override
    public void saveSorting(final Configuration configuration, final List<Cycle> sorting) {
        new QueryRunner(dataSource).update(
                "INSERT INTO sorting(config, hash_code, sorting) VALUES (?, ?, ?, ?)",
                getId(configuration), configuration.hashCode(), sorting.toString()
        );
    }

    @SneakyThrows
    @Override
    public void markNoSorting(final Configuration configuration, Configuration parent) {
        new QueryRunner(dataSource).update(
                "MERGE INTO no_sorting(config, parent) KEY(config) VALUES (?, ?)",
                getId(configuration), getId(parent)
        );
    }

    @SneakyThrows
    @Override
    public boolean hasNoSorting(final Configuration configuration) {
        return new QueryRunner(dataSource).query(
                "SELECT 1 FROM no_sorting WHERE config = ?",
                new ScalarHandler<String>(1),
                getId(configuration)
        ) != null;
    }

    @SneakyThrows
    @Override
    public void saveComponentSorting(final Configuration configuration, final List<Cycle> sorting) {
        new QueryRunner(dataSource).update(
                "INSERT INTO comp_sorting(config, hash_code, sorting) VALUES (?, ?, ?)",
                getId(configuration), configuration.hashCode(), sorting.toString()
        );
    }

    @SneakyThrows
    @Override
    public Optional<List<Cycle>> findSorting(final String spi) {
        return findSorting("SELECT * FROM sorting WHERE config = ?", spi);
    }

    private Optional<List<Cycle>> findSorting(final String query, final String spi) throws SQLException {
        val sorting = new QueryRunner(dataSource).query(query, new MapListHandler(), spi);
        return sorting.stream()
                .map(map -> Arrays.stream(map.get("sorting").toString()
                                .replace("[", "")
                                .replace("]", "")
                                .split(", "))
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

