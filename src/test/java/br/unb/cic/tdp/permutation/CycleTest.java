package br.unb.cic.tdp.permutation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CycleTest {

    @Test
    void testEquals() {
        assertEquals(new Cycle("0,1,2,3,4"), new Cycle("2,3,4,0,1"));
        assertNotEquals(new Cycle("0,1,2,3"), new Cycle("2,3,4,0,1"));
        assertNotEquals(new Cycle("0,1,2,3,4"), new Cycle("0,1,2,3"));
    }
}