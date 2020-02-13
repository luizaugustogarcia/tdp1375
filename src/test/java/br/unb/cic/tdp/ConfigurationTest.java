package br.unb.cic.tdp;

import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import org.junit.jupiter.api.Test;

import static br.unb.cic.tdp.CommonOperations.CANONICAL_PI;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ConfigurationTest {

    @Test
    void testHashCode() {
        assertEquals(new Configuration(new MulticyclePermutation("(0,3)(1,2)"), new Cycle("0,1,2,3")).hashCode(),
                new Configuration(new MulticyclePermutation("(0,1)(2,3)"), new Cycle("0,1,2,3").getInverse()).hashCode());
        assertEquals(new Configuration(new MulticyclePermutation("(0,3)(1,2)"), new Cycle("0,1,2,3").getInverse()).hashCode(),
                new Configuration(new MulticyclePermutation("(0,1)(2,3)"), new Cycle("0,1,2,3").getInverse()).hashCode());
    }

    @Test
    void testEquals() {
        assertEquals(new Configuration(new MulticyclePermutation("(0,3)(1,2)"), new Cycle("0,1,2,3")),
                new Configuration(new MulticyclePermutation("(0,1)(2,3)"), new Cycle("0,1,2,3").getInverse()));
        assertEquals(new Configuration(new MulticyclePermutation("(0,3)(1,2)"), new Cycle("0,1,2,3").getInverse()),
                new Configuration(new MulticyclePermutation("(0,1)(2,3)"), new Cycle("0,1,2,3").getInverse()));

        assertNotEquals(new Configuration(new MulticyclePermutation("(0,1)(2,3)"), new Cycle("0,1,2,3").getInverse()),
                new Configuration(new MulticyclePermutation("(0,2)(1,3)"), new Cycle("0,1,2,3").getInverse()));
        assertNotEquals(new Configuration(new MulticyclePermutation("(1,5,3)(4,8,6)(7,11,9)(10,14,12)(0,13,2)"), CANONICAL_PI[15]),
                new Configuration(new MulticyclePermutation("(1,5,3)(4,8,6)(7,11,9)(10,14,12)(13,17,15)(0,16,2)"), CANONICAL_PI[18]));
    }
}