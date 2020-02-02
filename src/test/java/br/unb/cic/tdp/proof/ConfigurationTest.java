package br.unb.cic.tdp.proof;

import br.unb.cic.tdp.permutation.Cycle;
import br.unb.cic.tdp.permutation.MulticyclePermutation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ConfigurationTest {

    @Test
    void testHashCode() {
        assertEquals(new Configuration(new Cycle("0,1,2,3"), new MulticyclePermutation("(0,3)(1,2)")).hashCode(),
                new Configuration(new Cycle("0,1,2,3").getInverse(), new MulticyclePermutation("(0,1)(2,3)")).hashCode());
        assertEquals(new Configuration(new Cycle("0,1,2,3").getInverse(), new MulticyclePermutation("(0,3)(1,2)")).hashCode(),
                new Configuration(new Cycle("0,1,2,3").getInverse(), new MulticyclePermutation("(0,1)(2,3)")).hashCode());
        assertNotEquals(new Configuration(new Cycle("0,1,2,3").getInverse(), new MulticyclePermutation("(0,1)(2,3)")).hashCode(),
                new Configuration(new Cycle("0,1,2,3").getInverse(), new MulticyclePermutation("(0,2)(1,3)")).hashCode());
    }

    @Test
    void testEquals() {
        assertEquals(new Configuration(new Cycle("0,1,2,3"), new MulticyclePermutation("(0,3)(1,2)")),
                new Configuration(new Cycle("0,1,2,3").getInverse(), new MulticyclePermutation("(0,1)(2,3)")));
        assertEquals(new Configuration(new Cycle("0,1,2,3").getInverse(), new MulticyclePermutation("(0,3)(1,2)")),
                new Configuration(new Cycle("0,1,2,3").getInverse(), new MulticyclePermutation("(0,1)(2,3)")));
        assertNotEquals(new Configuration(new Cycle("0,1,2,3").getInverse(), new MulticyclePermutation("(0,1)(2,3)")),
                new Configuration(new Cycle("0,1,2,3").getInverse(), new MulticyclePermutation("(0,2)(1,3)")));
    }
}