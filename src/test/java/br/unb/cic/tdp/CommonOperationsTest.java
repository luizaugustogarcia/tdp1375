package br.unb.cic.tdp;

import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static br.unb.cic.tdp.CommonOperations.*;
import static org.junit.jupiter.api.Assertions.*;

class CommonOperationsTest {

    @Test
    void areNotIntersectingTest() {
        assertTrue(areNotIntersecting(new MulticyclePermutation("(0,1)(2,3)"), new Cycle("0,1,2,3")));
        assertFalse(areNotIntersecting(new MulticyclePermutation("(0,2)(1,3)"), new Cycle("0,1,2,3")));
    }

    @Test
    void simplifyTest() {
        assertEquals(simplify(new Cycle("0,3,6,2,5,1,4,10,9,8,7")), new Cycle("0,4,8,3,7,2,6,1,5,9,14,13,12,11,10"));
        assertEquals(simplify(new Cycle("0,4,3,2,1,8,7,6,5")), new Cycle("0,5,4,3,2,1,6,11,10,9,8,7"));
    }

    @Test
    void areSymbolsInCyclicOrderTest() {
        assertTrue(areSymbolsInCyclicOrder(new byte[]{5, 6, 7, 0, 1}, new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8}));
        assertFalse(areSymbolsInCyclicOrder(new byte[]{5, 7, 6, 0, 1}, new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8}));
    }

    @Test
    void signatureTest() {
        assertTrue(Arrays.equals(signature(new MulticyclePermutation("(0,1)(2,3)"), new Cycle("0,1,2,3")), new byte[]{1, 1, 2, 2}));
        assertTrue(Arrays.equals(signature(new MulticyclePermutation("(0,2)(1,3)"), new Cycle("0,1,2,3")), new byte[]{1, 2, 1, 2}));
    }
}