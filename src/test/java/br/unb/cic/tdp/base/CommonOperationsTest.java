package br.unb.cic.tdp.base;

import br.unb.cic.tdp.permutation.Cycle;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static br.unb.cic.tdp.base.CommonOperations.*;
import static org.junit.jupiter.api.Assertions.*;

class CommonOperationsTest {
    final private Cycle alpha = Cycle.create("0 2 7");
    final private Cycle pi = Cycle.create("0 5 4 3 8 7 6 2 1");

    @Test
    void testSimplify() {
        assertEquals(Cycle.create("0 4 8 3 7 2 6 1 5 9 14 13 12 11 10"), simplify(Cycle.create("0 3 6 2 5 1 4 10 9 8 7")));
    }

    @Test
    void testApplyTransposition() {
        assertEquals(Cycle.create("0 1 2 3 4 5 6"), applyTranspositionOptimized(Cycle.create("0 4 5 6 1 2 3"), Cycle.create("0 4 1")));
    }

    @Test
    void testCycleIndex() {
        val pi = Cycle.create("0 5 4 3 2 1");
        val c0 = Cycle.create("0 2 4");
        val c1 = Cycle.create("1 3 5");

        val index = CommonOperations.cycleIndex(Arrays.asList(c0, c1), pi);
        assertArrayEquals(new Cycle[]{c0, c1, c0, c1, c0, c1}, index);
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