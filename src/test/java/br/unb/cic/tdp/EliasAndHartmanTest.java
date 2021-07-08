package br.unb.cic.tdp;

import br.unb.cic.tdp.permutation.Cycle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EliasAndHartmanTest {

    private EliasAndHartman eliasAndHartman = new EliasAndHartman();

    @Test
    void with2_2Seq() {
        assertEquals(2, eliasAndHartman.sort(Cycle.create("0 3 2 1")).size());
    }

    @Test
    void with3_2Seq() {
        assertEquals(3, eliasAndHartman.sort(Cycle.create("0 5 4 3 2 1")).size());
    }

    @Test
    void with11_8Seq() {
        assertEquals(7, eliasAndHartman.sort(Cycle.create("0 4 8 3 7 2 6 1 5 9 14 13 12 11 10")).size());
    }

    @Test
    void withCombinationsOfBadSmallComponents() {
        assertEquals(6, eliasAndHartman.sort(Cycle.create("0 5 4 3 2 1 6 11 10 9 8 7")).size());
    }
}