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
        assertArrayEquals(new byte[] {2, 3, 4, 5, 0, 1}, cycle.getStartingBy((byte) 2).getSymbols());
    }

    @Test
    void getNumberOfEvenCycles() {
        assertEquals(0, cycle.getNumberOfEvenCycles());
        assertEquals(1, new Cycle("0 1 2").getNumberOfEvenCycles());
    }

    @Test
    void indexOf() {
        assertEquals(3, cycle.indexOf((byte) 3));
    }

    @Test
    void contains() {
        assertTrue(cycle.contains((byte) 3));
        assertFalse(cycle.contains((byte) 6));
    }

    @Test
    void isApplicable() {
        assertTrue(cycle.isApplicable(new Cycle("0 1 2")));
        assertFalse(cycle.isApplicable(new Cycle("0 1 2").getInverse()));
    }

    @Test
    void isOriented() {
        assertTrue(cycle.isOriented((byte)0, (byte)1, (byte)2));
        assertFalse(cycle.isOriented((byte)0, (byte)2, (byte)1));
    }

    @Test
    void isLong() {
        assertTrue(cycle.isLong());
        assertFalse(new Cycle("0 1 2").isLong());
    }
}
