package br.unb.cic.tdp;

import br.unb.cic.tdp.permutation.Cycle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EliasAndHartmanTest {

    private final EliasAndHartman eliasAndHartman = new EliasAndHartman();

    @Test
    void with2_2Seq() {
        assertEquals(2, eliasAndHartman.sort(Cycle.of("0 3 2 1")).getSecond().size());
    }

    @Test
    void with3_2Seq() {
        assertEquals(3, eliasAndHartman.sort(Cycle.of("0 5 4 3 2 1")).getSecond().size());
    }

    @Test
    void with11_8Seq() {
        assertEquals(7, eliasAndHartman.sort(Cycle.of("0 4 8 3 7 2 6 1 5 9 14 13 12 11 10")).getSecond().size());
    }

    @Test
    void withCombinationsOfBadSmallComponents() {
        assertEquals(6, eliasAndHartman.sort(Cycle.of("0 5 4 3 2 1 6 11 10 9 8 7")).getSecond().size());
    }

    @Test
    void testAdHoc1() {
        assertEquals(11, eliasAndHartman.sort(Cycle.of("0,9,13,17,11,19,3,12,8,7,16,6,2,14,4,18,5,15,10,1,20")).getSecond().size());
    }

    @Test
    void testAdHoc2() {
        assertEquals(9, eliasAndHartman.sort(Cycle.of("0,11,20,17,3,18,15,12,19,9,1,14,8,16,5,2,13,6,10,7,4")).getSecond().size());
    }
}