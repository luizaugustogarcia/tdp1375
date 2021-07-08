package br.unb.cic.tdp;

import br.unb.cic.tdp.permutation.Cycle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SilvaetalTest {

    private Silvaetal silvaetal = new Silvaetal();

    @Test
    void with2_2Seq() {
        assertEquals(4, silvaetal.sort(Cycle.create("0 4 3 2 1 8 7 6 5")).size());
    }

    @Test
    void with2MoveFromOrientedCycle() {
        assertEquals(3, silvaetal.sort(Cycle.create("0 3 5 2 6 4 1")).size());
    }

    @Test
    void with3_2Seq() {
        assertEquals(6, silvaetal.sort(Cycle.create("0 5 4 3 2 1 6 11 10 9 8 7")).size());
    }

    @Test
    void with11_8Seq() {
        assertEquals(7, silvaetal.sort(Cycle.create("0 4 8 3 7 2 6 1 5 9 14 13 12 11 10")).size());
    }

    @Test
    void withBadOriented5Cycle() {
        assertEquals(3, silvaetal.sort(Cycle.create("0 4 3 2 1")).size());
    }

    @Test
    void withOriented7Cycle() {
        assertEquals(4, silvaetal.sort(Cycle.create("0 3 6 2 5 1 4")).size());
    }

    @Test
    void withCombinationsOfBadSmallComponents() {
        assertEquals(6, silvaetal.sort(Cycle.create("0 4 3 2 1 5 9 8 7 6")).size());
    }
}