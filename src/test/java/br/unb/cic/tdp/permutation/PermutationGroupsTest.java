package br.unb.cic.tdp.permutation;

import org.junit.jupiter.api.Test;

import static br.unb.cic.tdp.base.CommonOperations.applyTransposition;
import static org.junit.jupiter.api.Assertions.*;

class PermutationGroupsTest {

    @Test
    void testComputeProduct() {
        assertEquals(new MulticyclePermutation(new Cycle("0 1 2 3 4 5 6")), PermutationGroups.computeProduct(new Cycle("0 4 1"), new Cycle("0 4 5 6 1 2 3")));
    }

    @Test
    void testComputeProductNotIncluding1Cycles() {
        assertEquals(new MulticyclePermutation(""), PermutationGroups.computeProduct(false, new Cycle("0 1 2 3 4 5"), new Cycle("0 1 2 3 4 5").getInverse()));
    }

    @Test
    void testComputeProductIncluding1Cycles() {
        assertEquals(new MulticyclePermutation("(0)(1)(2)(3)(4)(5)"), PermutationGroups.computeProduct(true, new Cycle("0 1 2 3 4 5"), new Cycle("0 1 2 3 4 5").getInverse()));
    }
}
