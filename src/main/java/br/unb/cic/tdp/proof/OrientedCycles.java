package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.CommonOperations;
import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static br.unb.cic.tdp.base.CommonOperations.*;
import static br.unb.cic.tdp.base.Configuration.ofSignature;

public class OrientedCycles {

    @SneakyThrows
    public static void main(String[] args) {
        val pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
        pool.execute(new OrientedCyclesSortOrExtend(new Configuration("(0 2 1)", "(0 2 1)"), List.of(0, 1, 2),
                new CustomProofStorage("192.168.68.114", "luiz", "luiz", "oriented"), 1.6));
        pool.shutdown();
        // boundless
        pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }

    @RequiredArgsConstructor
    private static class OrientedCyclesSortOrExtend extends RecursiveAction {

        private final Configuration configuration;
        private final List<Integer> pivots;
        private final CustomProofStorage storage;
        private final double minRate;

        @Override
        protected void compute() {
            if (storage.isAlreadySorted(configuration, pivots)) {
                return;
            }

            if (!storage.isBadCase(configuration, pivots)) {
                if (storage.tryLock(configuration, pivots)) {
                    try {
                        val sorting = searchForSorting(configuration);
                        if (sorting.isPresent()) {
                            storage.saveSorting(configuration, pivots, sorting.get());
                            return;
                        } else {
                            storage.markNoSorting(configuration, pivots);
                            storage.markBadCase(configuration, pivots);
                        }
                        extend(configuration);
                    } finally {
                        storage.unlock(configuration, pivots);
                    }
                } // else: another thread is already working on this configuration
            }
        }

        protected void extend(final Configuration config) {
            val openGates = config.getOpenGates().size();

            val extensions = new ArrayList<Configuration>();

            val spi = config.getSpi().asNCycle().startingBy(pivots.get(0));
            val pi = config.getPi();

            for (int i = 1; i <= spi.indexOf(pivots.get(1)); i++) {
                val newSpi = Cycle.of(SortOrExtend.insertAtPosition(spi.getSymbols(), spi.size(), i));
                for (int j = 0; j < pi.size(); j++) {
                    val newPi = Cycle.of(SortOrExtend.insertAtPosition(pi.getSymbols(), pi.size(), j));
                    val extension = new Configuration(new MulticyclePermutation(newSpi), newPi);
                    if (openGates == 0 || (openGates == 1 && extension.getOpenGates().isEmpty())) {
                        extensions.add(extension);
                    }
                }
            }

            for (int i = spi.indexOf(pivots.get(1)) + 1; i <= spi.indexOf(pivots.get(2)); i++) {
                val newSpi = Cycle.of(SortOrExtend.insertAtPosition(spi.getSymbols(), spi.size(), i));
                for (int j = 0; j < pi.size(); j++) {
                    val newPi = Cycle.of(SortOrExtend.insertAtPosition(pi.getSymbols(), pi.size(), j));
                    val extension = new Configuration(new MulticyclePermutation(newSpi), newPi);
                    if (openGates == 0 || (openGates == 1 && extension.getOpenGates().isEmpty())) {
                        extensions.add(extension);
                    }
                }
            }

            for (int i = spi.indexOf(pivots.get(2)) + 1; i <= spi.size(); i++) {
                val newSpi = Cycle.of(SortOrExtend.insertAtPosition(spi.getSymbols(), spi.size(), i));
                for (int j = 0; j < pi.size(); j++) {
                    val newPi = Cycle.of(SortOrExtend.insertAtPosition(pi.getSymbols(), pi.size(), j));
                    val extension = new Configuration(new MulticyclePermutation(newSpi), newPi);
                    if (openGates == 0 || (openGates == 1 && extension.getOpenGates().isEmpty())) {
                        extensions.add(extension);
                    }
                }
            }

            extensions.stream()
                    .map(extension -> new OrientedCyclesSortOrExtend(extension, pivots, storage, minRate)).
                    forEach(ForkJoinTask::fork);
        }

        protected Optional<List<Cycle>> searchForSorting(Configuration configuration) {
            if (storage.hasNoSorting(configuration, pivots)) {
                return Optional.empty();
            }
            return searchForSorting(storage, new HashSet<>(pivots), configuration, minRate);
        }

        public static Optional<List<Cycle>> searchForSorting(final CustomProofStorage proofStorage, final Set<Integer> pivots, final Configuration configuration, final double minRate) {
            val _2move = lookFor2Move(configuration, pivots);
            if (_2move.isPresent()) {
                return _2move;
            }

            var sorting = CommonOperations.searchForSorting(configuration, pivots, twoLinesNotation(configuration.getSpi()), configuration.getPi().getSymbols(), new Stack<>(), minRate).map(moves -> moves.stream().map(Cycle::of).collect(Collectors.toList()));

            if (sorting.isEmpty() && configuration.isFull()) {
                val sigma = configuration.getSpi().times(configuration.getPi());
                if (sigma.size() == 1 && sigma.asNCycle().size() == configuration.getPi().size()) {
                    sorting = CommonOperations.searchForSorting(configuration, Set.of(), twoLinesNotation(configuration.getSpi()), configuration.getPi().getSymbols(), new Stack<>(), minRate).map(moves -> moves.stream().map(Cycle::of).collect(Collectors.toList()));
                    if (sorting.isEmpty()) {
                        System.out.println("bad component -> " + configuration);
                    } else if (proofStorage != null) {
                        proofStorage.saveComponentSorting(configuration, sorting.get());
                    }
                    return Optional.empty();
                }
            }

            return sorting;
        }
    }

    public static class CustomProofStorage {

        private BasicDataSource dataSource;

        @SneakyThrows
        public CustomProofStorage(final String host, final String userName, final String password, final String schema) {
            dataSource = new BasicDataSource();

            dataSource.setUrl("jdbc:mysql://" + host + ":3306/" + schema + "?allowPublicKeyRetrieval=true&useSSL=false");
            dataSource.setUsername(userName);
            dataSource.setPassword(password);
            dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
            dataSource.setMaxTotal(Runtime.getRuntime().availableProcessors());

            new QueryRunner(dataSource).update("CREATE TABLE IF NOT EXISTS working (config VARCHAR(255) primary key);");
            new QueryRunner(dataSource).update("CREATE TABLE IF NOT EXISTS bad_case (config VARCHAR(255) primary key);");
            new QueryRunner(dataSource).update("CREATE TABLE IF NOT EXISTS sorting (config VARCHAR(255), hash_code INTEGER, pivots VARCHAR(255), sorting VARCHAR(255), PRIMARY KEY (config, pivots));");
            new QueryRunner(dataSource).update("CREATE TABLE IF NOT EXISTS no_sorting (config VARCHAR(255) primary key);");
            new QueryRunner(dataSource).update("CREATE TABLE IF NOT EXISTS comp_sorting (config VARCHAR(255) primary key, hash_code INTEGER, sorting VARCHAR(255));");

            new QueryRunner(dataSource).update("TRUNCATE TABLE working;");
            new QueryRunner(dataSource).update("TRUNCATE TABLE bad_case;");
        }

        private static String getId(final Configuration configuration, final List<Integer> pivots) {
            val pi = configuration.getPi();
            val spi = configuration.getSpi().asNCycle();

            val equivalent = new ArrayList<String>();

            // TODO this gives the id, but effectively, the permutation being sort is another one - FIX
            equivalent.add(getIdStartingBy(configuration, pivots, pivots.get(0)));
            equivalent.add(getIdStartingBy(configuration, pivots, pivots.get(1)));
            equivalent.add(getIdStartingBy(configuration, pivots, pivots.get(2)));

            if (equivalent.contains("(0 1 6 4 10 11 8 5 3 2 9 7)#[0, 1, 11]")) {
                System.out.println("");
            }
//
//            val canonical = new Configuration(equivalent.stream().findFirst().get().split("#")[0]);
//            val mirror = Configuration.mirror(canonical.getSpi(), canonical.getPi());
//            val mirrorConfig = ofSignature(mirror.getContent());
//            val mirrorSpi = mirrorConfig.getSpi().asNCycle();
//            val mirrorPivots = List.of(
//                    mirrorSpi.get(reflectedValue(pi.size(), spi.indexOf(pivots.get(0)))),
//                    mirrorSpi.get(reflectedValue(pi.size(), spi.indexOf(pivots.get(1)))),
//                    mirrorSpi.get(reflectedValue(pi.size(), spi.indexOf(pivots.get(2))))
//            );
//
//            equivalent.add(getIdStartingBy(mirrorConfig, mirrorPivots, mirrorPivots.get(0)));
//            equivalent.add(getIdStartingBy(mirrorConfig, mirrorPivots, mirrorPivots.get(1)));
//            equivalent.add(getIdStartingBy(mirrorConfig, mirrorPivots, mirrorPivots.get(2)));

            return equivalent.stream().min(Comparator.naturalOrder()).get();
        }

        public static int reflectedValue(int n, int x) {
            return (n - 1) - x;
        }

        private static String getIdStartingBy(final Configuration configuration, final List<Integer> pivots, final int startingBy) {
            val spi = configuration.getSpi().asNCycle().startingBy(startingBy);

            val canonical = ofSignature(new Configuration(configuration.getSpi(), configuration.getPi().asNCycle().startingBy(startingBy)).getSignature().getContent());
            val canonicalSpi = canonical.getSpi().asNCycle();
            val canonicalPivots = new ArrayList<>(List.of(canonicalSpi.get(spi.indexOf(pivots.get(0))), canonicalSpi.get(spi.indexOf(pivots.get(1))), canonicalSpi.get(spi.indexOf(pivots.get(2)))));

            Collections.sort(canonicalPivots);

            return canonicalSpi + "#" + canonicalPivots;
        }

        @SneakyThrows
        public boolean isAlreadySorted(final Configuration configuration, final List<Integer> pivots) {
            return new QueryRunner(dataSource).query("SELECT COUNT(1) FROM sorting WHERE config = '" + getId(configuration, pivots) + "'", new ScalarHandler<Long>(1)) > 0;
        }

        @SneakyThrows
        public boolean isBadCase(final Configuration configuration, final List<Integer> pivots) {
            return new QueryRunner(dataSource).query("SELECT 1 FROM bad_case WHERE config = '" + getId(configuration, pivots) + "'", new ScalarHandler<String>(1)) != null;
        }

        @SneakyThrows
        public boolean tryLock(final Configuration configuration, final List<Integer> pivots) {
            val sql = "INSERT INTO working (config) SELECT ? FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM working WHERE config = ?)";
            val id = getId(configuration, pivots);
            return new QueryRunner(dataSource).update(sql, id, id) > 0;
        }

        @SneakyThrows
        public void unlock(final Configuration configuration, final List<Integer> pivots) {
            new QueryRunner(dataSource).update("DELETE FROM working WHERE config = ?", getId(configuration, pivots));
        }

        @SneakyThrows
        public void markBadCase(final Configuration configuration, final List<Integer> pivots) {
            new QueryRunner(dataSource).update("INSERT INTO bad_case(config) VALUES (?)", getId(configuration, pivots));
        }

        @SneakyThrows
        public void saveSorting(final Configuration configuration, final List<Integer> pivots, final List<Cycle> sorting) {
            new QueryRunner(dataSource).update("INSERT INTO sorting(config, hash_code, pivots, sorting) VALUES (?, ?, ?, ?)", getId(configuration, pivots), configuration.hashCode(), pivots.toString(), sorting.toString());
        }

        @SneakyThrows
        public void markNoSorting(final Configuration configuration, final List<Integer> pivots) {
            new QueryRunner(dataSource).update("INSERT IGNORE INTO no_sorting(config) VALUES (?);", getId(configuration, pivots));
        }

        @SneakyThrows
        public boolean hasNoSorting(final Configuration configuration, final List<Integer> pivots) {
            return new QueryRunner(dataSource).query("SELECT 1 FROM no_sorting WHERE config = '" + getId(configuration, pivots) + "'", new ScalarHandler<String>(1)) != null;
        }

        @SneakyThrows
        public void saveComponentSorting(final Configuration configuration, final List<Cycle> sorting) {
            // TODO
            // new QueryRunner(dataSource).update("INSERT INTO comp_sorting(config, hash_code, sorting) VALUES (?, ?, ?)", getId(configuration, pivots), configuration.hashCode(), sorting.toString());
        }

        @SneakyThrows
        public Optional<List<Cycle>> findSorting(final String spi, final List<Integer> pivots) {
            return findSorting("SELECT * FROM sorting WHERE config = ?", spi);
        }

        private Optional<List<Cycle>> findSorting(String query, String spi) throws SQLException {
            val sorting = new QueryRunner(dataSource).query(query, new MapListHandler(), spi);
            return sorting.stream().map(map -> Arrays.stream(map.get("sorting").toString()
                            .replace("[", "")
                            .replace("]", "")
                            .split(", "))
                    .map(Cycle::of)
                    .collect(Collectors.toList())).findFirst();
        }

        @SneakyThrows
        public Optional<List<Cycle>> findCompSorting(final String spi) {
            // TODO
            return findSorting("SELECT * FROM comp_sorting WHERE config = ?", spi);
        }
    }
}
