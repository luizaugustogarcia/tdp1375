package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ScalarHandler;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class H2ProofStorage implements ProofStorage {
    private BasicDataSource dataSource;

    private int n = 0;

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
        new QueryRunner(dataSource).update("CREATE TABLE IF NOT EXISTS sorting (config VARCHAR(255) primary key, sorting VARCHAR(255));");
        new QueryRunner(dataSource).update("CREATE TABLE IF NOT EXISTS no_sorting (config VARCHAR(255) primary key);");

        new QueryRunner(dataSource).update("TRUNCATE TABLE working;");
        new QueryRunner(dataSource).update("TRUNCATE TABLE bad_case;");
    }

    @SneakyThrows
    private void report() {
        System.out.println("sortings: " + new QueryRunner(dataSource).query("SELECT COUNT(1) FROM sorting", new ScalarHandler<String>(1)));
        System.out.println("bad_cases: " + new QueryRunner(dataSource).query("SELECT COUNT(1) FROM bad_case", new ScalarHandler<String>(1)));
        System.out.println("n: " + n);
        System.out.println();
    }

    private static String getId(final Configuration configuration) {
        return configuration.getSpi().toString();
    }

    @SneakyThrows
    @Override
    public boolean isAlreadySorted(final Configuration configuration) {
        return new QueryRunner(dataSource)
                .query("SELECT 1 FROM sorting WHERE config = '" + getId(configuration) + "'",
                        new ScalarHandler<String>(1)) != null;
    }

    @SneakyThrows
    @Override
    public boolean isBadCase(final Configuration configuration) {
        return new QueryRunner(dataSource)
                .query("SELECT 1 FROM bad_case WHERE config = '" + getId(configuration) + "'",
                        new ScalarHandler<String>(1)) != null;
    }

    @SneakyThrows
    @Override
    public boolean tryLock(final Configuration configuration) {
        val sql = "INSERT INTO working (config) " +
                "SELECT ? FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM working WHERE config = ?)";
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
    public void saveSorting(final Configuration configuration, final List<Cycle> sorting) {
        new QueryRunner(dataSource).update("INSERT INTO sorting(config, sorting) VALUES (?, ?)", getId(configuration), sorting.toString());
    }

    @SneakyThrows
    @Override
    public void noSorting(final Configuration configuration) {
        new QueryRunner(dataSource).update("MERGE INTO no_sorting(config) KEY(config) VALUES (?)", getId(configuration));
    }

    @SneakyThrows
    @Override
    public boolean hasNoSorting(final Configuration configuration) {
        if (configuration.getPi().getMaxSymbol() > n) {
            n = configuration.getPi().getMaxSymbol();
        }
        return new QueryRunner(dataSource)
                .query("SELECT 1 FROM no_sorting WHERE config = '" + getId(configuration) + "'",
                        new ScalarHandler<String>(1)) != null;
    }
}
