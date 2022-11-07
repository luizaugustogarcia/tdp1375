package br.unb.cic.tdp.permutation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PermutationGroupsTest {

    @Test
    void testComputeProduct() {
        assertEquals(new MulticyclePermutation(Cycle.create("0 1 2 3 4 5 6")), PermutationGroups.computeProduct(Cycle.create("0 4 1"), Cycle.create("0 4 5 6 1 2 3")));
    }

    @Test
    void testComputeProductNotIncluding1Cycles() {
        assertEquals(new MulticyclePermutation(), PermutationGroups.computeProduct(false, Cycle.create("0 1 2 3 4 5"), Cycle.create("0 1 2 3 4 5").getInverse()));
    }

    @Test
    void testComputeProductIncluding1Cycles() {
        assertEquals(new MulticyclePermutation("(0)(1)(2)(3)(4)(5)"), PermutationGroups.computeProduct(true, Cycle.create("0 1 2 3 4 5"), Cycle.create("0 1 2 3 4 5").getInverse()));
    }
}
