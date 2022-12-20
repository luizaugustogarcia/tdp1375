package br.unb.cic.tdp.permutation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CycleTest {

    private final Cycle cycle = Cycle.create("0 1 2 3 4 5");

    @Test
    void getSymbols() {
        assertArrayEquals(new int[] {0, 1, 2, 3, 4, 5}, cycle.getSymbols());
    }

    @Test
    void getMaxSymbol() {
        assertEquals(5, cycle.getMaxSymbol());
    }

    @Test
    void getMinSymbol() {
        assertEquals(0, cycle.getMinSymbol());
    }

    @Test
    void getInverse() {
        assertEquals(Cycle.create("0 5 4 3 2 1"), cycle.getInverse());
    }

    @Test
    void getNorm() {
        assertEquals(5, cycle.getNorm());
    }

    @Test
    void isEven() {
        assertTrue(Cycle.create("0 1 2 3 4").isEven());
        assertFalse(cycle.isEven());
    }

    @Test
    void image() {
        assertEquals(1, cycle.image(0));
    }

    @Test
    void pow() {
        assertEquals(2, cycle.pow(0, 2));
    }

    @Test
    void getK() {
        assertEquals(2, cycle.getK(0, 2));
    }

    @Test
    void getStartingBy() {
        assertArrayEquals(new int[] {2, 3, 4, 5, 0, 1}, cycle.startingBy(2).getSymbols());
    }

    @Test
    void getNumberOfEvenCycles() {
        assertEquals(0, cycle.getNumberOfEvenCycles());
        assertEquals(1, Cycle.create("0 1 2").getNumberOfEvenCycles());
    }

    @Test
    void indexOf() {
        assertEquals(3, cycle.indexOf(3));
    }

    @Test
    void contains() {
        assertTrue(cycle.contains(3));
        assertFalse(cycle.contains(6));
    }

    @Test
    void isLong() {
        assertTrue(cycle.isLong());
        assertFalse(Cycle.create("0 1 2").isLong());
    }
}
