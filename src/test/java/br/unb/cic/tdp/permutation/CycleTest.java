package br.unb.cic.tdp.permutation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CycleTest {

    private final Cycle cycle = new Cycle("0 1 2 3 4 5");

    @Test
    void getSymbols() {
        assertArrayEquals(new byte[] {0, 1, 2, 3, 4, 5}, cycle.getSymbols());
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
    void redefine() {
        // TODO
    }

    @Test
    void testToString() {
        // TODO
    }

    @Test
    void getInverse() {
        assertEquals(new Cycle("0 5 4 3 2 1"), cycle.getInverse());
    }

    @Test
    void getNorm() {
        assertEquals(5, cycle.getNorm());
    }

    @Test
    void isEven() {
        assertTrue(new Cycle("0 1 2 3 4").isEven());
        assertFalse(cycle.isEven());
    }

    @Test
    void image() {
        assertEquals(1, cycle.image((byte) 0));
    }

    @Test
    void pow() {
        assertEquals(2, cycle.pow((byte) 0, 2));
    }

    @Test
    void getK() {
        assertEquals(2, cycle.getK((byte) 0, (byte) 2));
    }

    @Test
    void getStartingBy() {
        // TODO
    }

    @Test
    void getNumberOfEvenCycles() {
        // TODO
    }

    @Test
    void indexOf() {
        // TODO
    }

    @Test
    void contains() {
        // TODO
    }

    @Test
    void isApplicable() {
        // TODO
    }

    @Test
    void isOriented() {
        // TODO
    }

    @Test
    void isLong() {
        // TODO
    }
}
