package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.base.CommonOperations;
import br.unb.cic.tdp.base.Configuration;
import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import br.unb.cic.tdp.util.Pair;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.paukov.combinatorics3.Generator;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static br.unb.cic.tdp.Application.CONFIGS_QUEUE;
import static br.unb.cic.tdp.proof.SortOrExtend.insertAtPosition;
import static br.unb.cic.tdp.proof.SortOrExtend.unorientedExtension;
import static java.lang.String.format;
import static java.util.stream.Stream.concat;

@RequiredArgsConstructor
public class SortOrExtendNew {

    /*
    starting with a 3-cycle (1 2 3)

    type 1)	(a b c)$(d e f)$ -> add new 3-cycle, new symbols
    type 2)	(a b c)$(d e)(f g)$ -> add new 2-cycles, at least 1 new symbol - each new 2-cycle
    type 3) (a_)(b_)(c_)$(a b c)$ -> -2-move (up to 2 symbols can be new, i.e. they were fixed - cannot be 3, or it will generate a permutation allowing a 2-move creating 2-bonds)

    if there is a 2-cycle, only type 2) and 3) should be possible (affecting at least one 2-cycle) because there is no 2-cycle in the permutation being sorted

    * never leave an open gate?
    * never leave a odd spi?
     */

    private final RabbitTemplate rabbitTemplate;
    private final Configuration parent;
    private final Configuration configuration;
    private final ProofStorage storage;
    private final double minRate;

    public void compute() {
        val configuration = getCanonical(this.configuration);

        if (storage.isAlreadySorted(configuration)) {
            return;
        }

        if (!storage.isBadCase(configuration)) {
            if (storage.tryLock(configuration)) {
                try {
                    val sorting = searchForSorting(configuration);
                    if (sorting.isPresent()) {
                        storage.saveSorting(configuration, Set.of(), sorting.get());
                        return;
                    } else {
                        storage.markNoSorting(configuration, getCanonical(parent));
                        storage.markBadCase(configuration);
                    }
                    extend(configuration);
                } finally {
                    storage.unlock(configuration);
                }
            } // else: another thread is already working on this configuration
        }
    }

    private static Configuration getCanonical(final Configuration configuration) {
        return Configuration.ofSignature(configuration.getSignature().getContent());
    }

    private Optional<List<Cycle>> searchForSorting(Configuration configuration) {
        if (storage.hasNoSorting(configuration)) {
            return Optional.empty();
        }

        val pivots = configuration.getSpi().stream()
                .map(Cycle::getMinSymbol)
                .collect(Collectors.toSet());

        return CommonOperations.searchForSorting(storage, configuration, minRate, pivots);
    }

    /*
     * Type 1 extension.
     */
    static List<Pair<String, Configuration>> type1Extensions(final Configuration configuration) {
        if (configuration.getSpi().stream().anyMatch(Cycle::isTwoCycle)) {
            return List.of();
        }

        val result = new ArrayList<Pair<String, Configuration>>();

        val n = configuration.getPi().getSymbols().length;

        for (var a = 0; a < n; a++) {
            for (var b = 0; b < n; b++) {
                for (var c = b; c < n; c++) {
                    if (!(a == b && b == c)) {
                        val newCycle = format("(%d %d %d)", n, n + 2, n + 1);
                        val extendedPi = unorientedExtension(configuration.getPi().getSymbols(), n, a, b, c).elements();
                        val extension = new Configuration(new MulticyclePermutation(configuration.getSpi() + newCycle), Cycle.of(extendedPi));
                        if (extension.isFull()) {
                            result.add(new Pair<>(format("a=%d b=%d c=%d", a, b, c), extension));
                        }
                    }
                }
            }
        }

        return result;
    }

    /*
     * Type 2 extension.
     */
    static List<Pair<String, Configuration>> type2Extensions(final Configuration configuration) {
        val result = new ArrayList<Pair<String, Configuration>>();

        val n = configuration.getPi().getSymbols().length;

        // adds a new 2-cycle with two new symbols
        for (var a = 0; a < n; a++) {
            var extendedPi = unorientedExtension(configuration.getPi().getSymbols(), n, a).elements();
            for (var b = 0; b < n + 1; b++) {
                extendedPi = unorientedExtension(extendedPi, n + 1, b).elements();
                val newCycle = format("(%d %d)", n, n + 1);
                val extension = new Configuration(new MulticyclePermutation(configuration.getSpi() + newCycle), Cycle.of(extendedPi));
                if (extension.isFull()) {
                    result.add(new Pair<>(format("a=%d b=%d", a, b), extension));
                }
            }
        }

        // grows an existing cycle
        for (var cycle : configuration.getSpi()) {
            for (var a = 0; a < n; a++) {
                val newCycle = format("(%s %d)", cycle.toString().substring(0, cycle.toString().length() - 1), n);
                val extendedPi = unorientedExtension(configuration.getPi().getSymbols(), n, a).elements();
                val extension = new Configuration(new MulticyclePermutation(configuration.getSpi().toString().replace(cycle.toString(), newCycle.toString())), Cycle.of(extendedPi));
                if (extension.isFull()) {
                    result.add(new Pair<>(format("cycle=%s a=%d", cycle, a), extension));
                }
            }
        }

        return result;
    }

    /*
     * Type 3 extension.
     */
    static List<Pair<String, Configuration>> type3Extensions(final Configuration configuration) {
        val result = new ArrayList<Pair<String, Configuration>>();

        val pi = configuration.getPi();
        val spi = configuration.getSpi();
        val n = pi.getSymbols().length;

        // no new symbol
        for (val cycles : Generator.permutation(configuration.getSpi()).k(3)) {
            for (val triple : Generator.cartesianProduct(cycles.get(0).getSymbolsAsList(),
                    cycles.get(1).getSymbolsAsList(), cycles.get(2).getSymbolsAsList())) {
                val move = Cycle.of(triple.get(0), triple.get(1), triple.get(2));
                if (CommonOperations.isOriented(pi, move)) {
                    result.add(new Pair<>("", new Configuration(spi.times(move.getInverse()), move.times(pi).asNCycle())));
                }
            }
        }

        // one new symbol
        var extendedSpi = new MulticyclePermutation(configuration.getSpi());
        extendedSpi.add(Cycle.of(n));

        for (var a = 0; a <= n; a++) {
            val extendedPi = Cycle.of(insertAtPosition(pi.getSymbols(), n, a));

            for (val cycles : Generator.permutation(configuration.getSpi()).k(2)) {
                for (val pair : Generator.cartesianProduct(cycles.get(0).getSymbolsAsList(),
                        cycles.get(1).getSymbolsAsList())) {
                    val move = Cycle.of(pair.get(0), pair.get(1), n);
                    if (CommonOperations.isOriented(extendedPi, move)) {
                        result.add(new Pair<>("", new Configuration(spi.times(move.getInverse()), move.times(extendedPi).asNCycle())));
                    }
                }
            }
        }

        // two new symbols
        extendedSpi = new MulticyclePermutation(configuration.getSpi());
        extendedSpi.add(Cycle.of(n));
        extendedSpi.add(Cycle.of(n + 1));

        for (var a = 0; a <= n; a++) {
            var extendedPi = Cycle.of(insertAtPosition(pi.getSymbols(), n, a));
            for (var b = 0; b <= n + 1; b++) {
                extendedPi = Cycle.of(insertAtPosition(extendedPi.getSymbols(), n + 1, b));

                for (val cycle : configuration.getSpi()) {
                    for (val c : cycle.getSymbols()) {
                        var move = Cycle.of(n, n + 1, c);
                        if (CommonOperations.isOriented(extendedPi, move)) {
                            result.add(new Pair<>("", new Configuration(spi.times(move.getInverse()), move.times(extendedPi).asNCycle())));
                        }
                        move = Cycle.of(n + 1, n, c);
                        if (CommonOperations.isOriented(extendedPi, move)) {
                            result.add(new Pair<>("", new Configuration(spi.times(move.getInverse()), move.times(extendedPi).asNCycle())));
                        }
                    }
                }
            }
        }

        return result;
    }

    private static boolean isProductOfTwoNCycles(final Configuration configuration) {
        return configuration.getSigma().size() == 1 && configuration.getSigma().asNCycle().size() == configuration.getPi().size();
    }

    private static Stream<Configuration> getExtensions(final Configuration configuration) {
        val numberOf2Cycles = configuration.getSpi().stream().filter(Cycle::isTwoCycle).count();

        return concat(
                type1Extensions(configuration).stream().map(Pair::getSecond),
                concat(
                        type2Extensions(configuration).stream().map(Pair::getSecond).flatMap(extension -> type2Extensions(extension).stream()).map(Pair::getSecond),
                        type3Extensions(configuration).stream().map(Pair::getSecond)
                ))
                .filter(extension -> numberOf2Cycles == 0 || extension.getSpi().stream().noneMatch(Cycle::isTwoCycle));
        //.filter(SortOrExtendNew::isProductOfTwoNCycles);
    }

    private void extend(final Configuration noSortingConfig) {
        getExtensions(noSortingConfig)
                .forEach(extension -> rabbitTemplate.convertAndSend(
                                CONFIGS_QUEUE,
                                String.format(
                                        "%s#%s;%s#%s",
                                        extension.getSpi().toString(),
                                        extension.getPi().toString(),
                                        configuration.getSpi().toString(),
                                        configuration.getPi().toString())
                        )
                );
    }

    public static void main(String[] args) {
        getExtensions(new Configuration("(0 3)(1 2)"))
                .map(SortOrExtendNew::getCanonical).forEach(System.out::println);
    }
}
