package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class H2ProofStorage implements ProofStorage {

    private BasicDataSource dataSource;

    @SneakyThrows
    public H2ProofStorage(final String outputDir) {
        dataSource = new BasicDataSource();
        dataSource.setUrl("jdbc:h2:file:" + outputDir + "/proof.db;CACHE_SIZE=200000");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setMaxTotal(Runtime.getRuntime().availableProcessors());

        new QueryRunner(dataSource).update("CREATE TABLE IF NOT EXISTS working (config VARCHAR(255) primary key);");

        new QueryRunner(dataSource).update("CREATE TABLE IF NOT EXISTS bad_case (config VARCHAR(255) primary key);");

        new QueryRunner(dataSource).update("CREATE TABLE IF NOT EXISTS sorting (config VARCHAR(255), hash_code INTEGER, pivots VARCHAR(255), sorting VARCHAR(255), PRIMARY KEY (config, pivots));");
        new QueryRunner(dataSource).update("CREATE INDEX IF NOT EXISTS idx_sorting ON sorting (hash_code);");

        new QueryRunner(dataSource).update("CREATE TABLE IF NOT EXISTS no_sorting (config VARCHAR(255) primary key);");

        new QueryRunner(dataSource).update("CREATE TABLE IF NOT EXISTS comp_sorting (config VARCHAR(255) primary key, hash_code INTEGER, sorting VARCHAR(255));");
        new QueryRunner(dataSource).update("CREATE INDEX IF NOT EXISTS idx_comp_sorting ON comp_sorting (hash_code);");

        new QueryRunner(dataSource).update("TRUNCATE TABLE working;");
        new QueryRunner(dataSource).update("TRUNCATE TABLE bad_case;");
    }

    private static String getId(final Configuration configuration) {
        return configuration.getSignature().toString();
    }

    @SneakyThrows
    @Override
    public boolean isAlreadySorted(final Configuration configuration) {
        return new QueryRunner(dataSource).query("SELECT COUNT(1) FROM sorting WHERE config = '" + getId(configuration) + "'", new ScalarHandler<Long>(1)) > 0;
    }

    @SneakyThrows
    @Override
    public boolean isBadCase(final Configuration configuration) {
        return new QueryRunner(dataSource).query("SELECT 1 FROM bad_case WHERE config = '" + getId(configuration) + "'", new ScalarHandler<String>(1)) != null;
    }

    @SneakyThrows
    @Override
    public boolean tryLock(final Configuration configuration) {
        val sql = "INSERT INTO working (config) SELECT ? FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM working WHERE config = ?)";
        val id = getId(configuration);
        return new QueryRunner(dataSource).update(sql, id, id) > 0;
    }

    @SneakyThrows
    @Override
    public void unlock(final Configuration configuration) {
        new QueryRunner(dataSource).update("DELETE FROM working WHERE config = ?", getId(configuration));
    }

    @SneakyThrows
    @Override
    public void markBadCase(final Configuration configuration) {
        new QueryRunner(dataSource).update("INSERT INTO bad_case(config) VALUES (?)", getId(configuration));
    }

    @SneakyThrows
    @Override
    public void saveSorting(final Configuration configuration, final Set<Integer> pivots, final List<Cycle> sorting) {
        new QueryRunner(dataSource).update("INSERT INTO sorting(config, hash_code, pivots, sorting) VALUES (?, ?, ?, ?)", getId(configuration), configuration.hashCode(), pivots.toString(), sorting.toString());
    }

    @SneakyThrows
    @Override
    public void noSorting(final Configuration configuration) {
        new QueryRunner(dataSource).update("MERGE INTO no_sorting(config) KEY(config) VALUES (?)", getId(configuration));
    }

    @SneakyThrows
    @Override
    public boolean hasNoSorting(final Configuration configuration) {
        return new QueryRunner(dataSource).query("SELECT 1 FROM no_sorting WHERE config = '" + getId(configuration) + "'", new ScalarHandler<String>(1)) != null;
    }

    @SneakyThrows
    @Override
    public List<Pair<Configuration, Pair<Set<Integer>, List<Cycle>>>> findBySortingsByHashCode(final int hashCode) {
        val sortings = new QueryRunner(dataSource).query("SELECT * FROM sorting WHERE hash_code = ?", new MapListHandler(), hashCode);
        return sortings.stream().map(map -> {
            val config = new Configuration(map.get("config").toString());
            val pivots = Arrays.stream(map.get("pivots").toString()
                            .replace("[", "")
                            .replace("]", "")
                            .split(", "))
                    .map(Integer::parseInt)
                    .collect(Collectors.toSet());
            val sorting = Arrays.stream(map.get("sorting").toString()
                            .replace("[", "")
                            .replace("]", "")
                            .split(", "))
                    .map(Cycle::of)
                    .collect(Collectors.toList());
            return Pair.of(config, Pair.of(pivots, sorting));
        }).collect(Collectors.toList());
    }

    @SneakyThrows
    @Override
    public void saveComponentSorting(final Configuration configuration, final List<Cycle> sorting) {
        new QueryRunner(dataSource).update("INSERT INTO comp_sorting(config, hash_code, sorting) VALUES (?, ?)", getId(configuration), configuration.hashCode(), sorting.toString());
    }

    @SneakyThrows
    @Override
    public List<Cycle> findByCompSortingByHashCode(int hashCode) {
        val map = new QueryRunner(dataSource).query("SELECT * FROM comp_sorting WHERE hash_code = ?", new MapHandler(), hashCode);
        return Arrays.stream(map.get("sorting").toString()
                        .replace("[", "")
                        .replace("]", "")
                        .split(", "))
                .map(Cycle::of)
                .collect(Collectors.toList());
    }
}
