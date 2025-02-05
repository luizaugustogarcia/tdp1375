package br.unb.cic.tdp.base;

import br.unb.cic.tdp.permutation.Cycle;
import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static br.unb.cic.tdp.base.CommonOperations.CANONICAL_PI;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConfigurationTest {

    @Test
    public void isFullTests() {
        assertTrue(new Configuration("(0 2 7 4)(1 5 3 6)").isFull());
        assertTrue(new Configuration("(0 4 2)(1 5 3)").isFull());
        assertFalse(new Configuration("(2 7 5)(4 8 6)(0 3 1)").isFull());
        assertTrue(new Configuration("(0 4 1 6 3)(2 7 5)").isFull());
        assertFalse(new Configuration("(0 4 1)(2 5 3)").isFull());
        assertTrue(new Configuration("(0 13 11)(1 14 3)(2 6 4)(5 9 7)(8 12 10)").isFull());
        assertFalse(new Configuration("(0 15 13 2 1)(3 16 5)(4 8 6)(7 11 9)(10 14 12)").isFull());
        assertTrue(new Configuration("(0 1 10)(2 9 5)(3 11 7)(4 8 6)").isFull());
        assertFalse(new Configuration("(0 7 3)(1 6 2 8 5 4)").isFull());
    }


}
