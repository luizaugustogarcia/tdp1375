package br.unb.cic.tdp.base;

import br.unb.cic.tdp.permutation.Cycle;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static br.unb.cic.tdp.base.CommonOperations.applyTransposition;
import static br.unb.cic.tdp.base.CommonOperations.simplify;
import static org.junit.jupiter.api.Assertions.*;

class CommonOperationsTest {

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
        // TODO
    }

    @Test
    void testIsNotOpenGate() {
        // TODO
    }

    @Test
    void testOpenGatesPerCycle() {
        // TODO
    }

    @Test
    void testIs11_8() {
        // TODO
    }

    @Test
    void testIsNot11_8() {
        // TODO
    }

    @Test
    void testAreSymbolsInCyclicOrder() {
        // TODO
    }

    @Test
    void testAreNotSymbolsInCyclicOrder() {
        // TODO
    }

    @Test
    void testSearchForSortingSeq() {
        // TODO
    }

    @Test
    void testSearchFor2MoveFromOrientedCycle() {
        // TODO
    }

    @Test
    void testGenerateAll0And2Moves() {
        // TODO
    }

    @Test
    void testIsOriented() {
        // TODO
    }

    @Test
    void testIsNotOriented() {
        // TODO
    }
}