package br.unb.cic.tdp.base;

import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.PermutationGroups;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static br.unb.cic.tdp.base.CommonOperations.CANONICAL_PI;
import static br.unb.cic.tdp.base.CommonOperations.applyTransposition;
import static br.unb.cic.tdp.base.CommonOperations.areSymbolsInCyclicOrder;
import static br.unb.cic.tdp.base.CommonOperations.is11_8;
import static br.unb.cic.tdp.base.CommonOperations.isOpenGate;
import static br.unb.cic.tdp.base.CommonOperations.isOriented;
import static br.unb.cic.tdp.base.CommonOperations.openGatesPerCycle;
import static br.unb.cic.tdp.base.CommonOperations.simplify;
import static org.junit.jupiter.api.Assertions.*;

class CommonOperationsTest {
    final private Cycle alpha = new Cycle("0 2 7");
    final private Cycle beta = new Cycle("1 3 5");
    final private List<Cycle> bigGamma = Arrays.asList(alpha, beta);
    final private Cycle pi = new Cycle("0 5 4 3 8 7 6 2 1");
    final private Cycle[] cycleIndex = new Cycle[]{alpha, beta, null, beta, null, alpha, null, alpha, beta};

    @Test
    void testSimplify() {
        assertEquals(new Cycle("0 4 8 3 7 2 6 1 5 9 14 13 12 11 10"), simplify(new Cycle("0 3 6 2 5 1 4 10 9 8 7")));
    }

    @Test
    void testApplyTransposition() {
        assertEquals(new Cycle("0 1 2 3 4 5 6"), applyTransposition(new Cycle("0 4 5 6 1 2 3"), new Cycle("0 4 1")));
    }

    @Test
    void testCycleIndex() {
        final var pi = new Cycle("0 5 4 3 2 1");
        final var c0 = new Cycle("0 2 4");
        final var c1 = new Cycle("1 3 5");

        final var index = CommonOperations.cycleIndex(Arrays.asList(c0, c1), pi);
        assertArrayEquals(new Cycle[]{c0, c1, c0, c1, c0, c1}, index);
    }

    @Test
    void testIsOpenGate() {
        assertTrue(isOpenGate(1, alpha, pi.getInverse(), cycleIndex));
        assertTrue(isOpenGate(1, beta, pi.getInverse(), cycleIndex));
        assertFalse(isOpenGate(0, alpha, pi.getInverse(), cycleIndex));
        assertFalse(isOpenGate(0, beta, pi.getInverse(), cycleIndex));
    }

    @Test
    void testOpenGatesPerCycle() {
        final var openGates = openGatesPerCycle(bigGamma, pi.getInverse());
        assertEquals(2, openGates.size());
        assertEquals(1, openGates.get(alpha));
        assertEquals(1, openGates.get(beta));
    }

    @Test
    void testIs11_8() {
        final var pi = new Cycle("0 4 8 3 7 2 6 1 5 9 14 13 12 11");
        final var spi = PermutationGroups.computeProduct(CANONICAL_PI[15], pi.getInverse());
        assertTrue(is11_8(spi, pi, Arrays.asList(new Cycle("1 4 7"), new Cycle("2 8 5"), new Cycle("1 4 7"), new Cycle("3 9 6"))));
    }

    @Test
    void testAreSymbolsInCyclicOrder() {
        assertTrue(areSymbolsInCyclicOrder(pi.getInverse(), alpha.getSymbols()));
        assertFalse(areSymbolsInCyclicOrder(pi, alpha.getSymbols()));
    }

    @Test
    void testIsOriented() {
        assertTrue(isOriented(pi, alpha.getInverse()));
        assertFalse(isOriented(pi, alpha));
    }
}