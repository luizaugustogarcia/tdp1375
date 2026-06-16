package br.unb.cic.tdp.base;

import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationTest {

    // ========================================
    // Configuration 1: (0 9 7)(1 5 2 6 3)(4 10 8)
    // ========================================

    @Test
    void testConfig1_3Norm() {
        val config = new Configuration("(0 9 7)(1 5 2 6 3)(4 10 8)");
        assertEquals(4, config.get3Norm());
    }

    @Test
    void testConfig1_OpenGates() {
        val config = new Configuration("(0 9 7)(1 5 2 6 3)(4 10 8)");
        assertEquals(Set.of(), config.getOpenGates());
        assertTrue(config.isFull());
    }

    @Test
    void testConfig1_Signature() {
        val config = new Configuration("(0 9 7)(1 5 2 6 3)(4 10 8)");
        val signature = config.getSignature();

        float[] expectedSignature = {1.0f, 2.01f, 2.03f, 2.05f, 3.0f, 2.02f, 2.04f, 1.0f, 3.0f, 1.0f, 3.0f};
        assertArrayEquals(expectedSignature, signature.getContent(), 0.0001f);
        assertFalse(signature.isMirror());
    }

    @Test
    void testConfig1_CanonicalSignature() {
        val config = new Configuration("(0 9 7)(1 5 2 6 3)(4 10 8)");
        val canonical = config.getCanonical();

        float[] expectedCanonicalSignature = {1.0f, 2.01f, 2.03f, 2.05f, 3.0f, 2.02f, 2.04f, 1.0f, 3.0f, 1.0f, 3.0f};
        assertArrayEquals(expectedCanonicalSignature, canonical.getSignature().getContent(), 0.0001f);
    }

    @Test
    void testConfig1_HashCode() {
        val config = new Configuration("(0 9 7)(1 5 2 6 3)(4 10 8)");
        assertEquals(-2127296534, config.hashCode());
    }

    @Test
    void testConfig1_EquivalentSignatures() {
        val config = new Configuration("(0 9 7)(1 5 2 6 3)(4 10 8)");
        val equivalentSignatures = config.getEquivalentSignatures()
                .map(sig -> Arrays.toString(sig.getContent()))
                .collect(Collectors.toList());

        val expectedSignatures = Arrays.asList(
            "[1.0, 2.01, 2.03, 2.05, 3.0, 2.02, 2.04, 1.0, 3.0, 1.0, 3.0]",
            "[1.0, 2.0, 1.0, 2.0, 3.01, 3.03, 1.0, 3.05, 3.02, 3.04, 2.0]",
            "[1.01, 1.03, 1.05, 2.0, 1.02, 1.04, 3.0, 2.0, 3.0, 2.0, 3.0]",
            "[1.0, 2.0, 1.0, 2.0, 1.0, 3.01, 3.03, 2.0, 3.05, 3.02, 3.04]",
            "[1.01, 1.03, 2.0, 1.05, 1.02, 3.0, 2.0, 3.0, 2.0, 3.0, 1.04]",
            "[1.01, 2.0, 3.0, 2.0, 3.0, 2.0, 1.03, 1.05, 3.0, 1.02, 1.04]",
            "[1.01, 2.0, 1.03, 1.05, 3.0, 2.0, 3.0, 2.0, 3.0, 1.02, 1.04]",
            "[1.01, 1.03, 2.0, 3.0, 2.0, 3.0, 2.0, 1.05, 1.02, 3.0, 1.04]",
            "[1.0, 2.01, 2.03, 3.0, 1.0, 3.0, 1.0, 3.0, 2.05, 2.02, 2.04]",
            "[1.01, 1.03, 1.05, 2.0, 3.0, 2.0, 3.0, 2.0, 1.02, 1.04, 3.0]",
            "[1.01, 1.03, 2.0, 3.0, 2.0, 3.0, 2.0, 1.05, 1.02, 1.04, 3.0]",
            "[1.0, 2.01, 2.03, 2.05, 3.0, 1.0, 3.0, 1.0, 3.0, 2.02, 2.04]",
            "[1.01, 2.0, 3.0, 2.0, 3.0, 2.0, 1.03, 1.05, 1.02, 3.0, 1.04]",
            "[1.01, 2.0, 1.03, 1.05, 1.02, 3.0, 2.0, 3.0, 2.0, 3.0, 1.04]",
            "[1.0, 2.0, 1.0, 2.0, 1.0, 3.01, 3.03, 3.05, 2.0, 3.02, 3.04]",
            "[1.01, 1.03, 2.0, 1.05, 1.02, 1.04, 3.0, 2.0, 3.0, 2.0, 3.0]",
            "[1.0, 2.0, 1.0, 2.0, 3.01, 3.03, 3.05, 1.0, 3.02, 3.04, 2.0]",
            "[1.0, 2.01, 2.03, 3.0, 2.05, 2.02, 2.04, 1.0, 3.0, 1.0, 3.0]",
            "[1.0, 2.0, 1.0, 3.01, 3.03, 3.05, 2.0, 3.02, 3.04, 1.0, 2.0]",
            "[1.0, 2.0, 3.01, 3.03, 1.0, 3.05, 3.02, 3.04, 2.0, 1.0, 2.0]",
            "[1.0, 2.0, 3.01, 3.03, 3.05, 1.0, 3.02, 3.04, 2.0, 1.0, 2.0]",
            "[1.0, 2.0, 1.0, 3.01, 3.03, 2.0, 3.05, 3.02, 3.04, 1.0, 2.0]"
        );

        assertEquals(expectedSignatures.size(), equivalentSignatures.size());
        for (String expected : expectedSignatures) {
            assertTrue(equivalentSignatures.contains(expected),
                    "Missing expected signature: " + expected);
        }
    }

    // ========================================
    // Configuration 2: (0 8 2)(1 6 4)(3 7 5)
    // ========================================

    @Test
    void testConfig2_3Norm() {
        val config = new Configuration("(0 8 2)(1 6 4)(3 7 5)");
        assertEquals(3, config.get3Norm());
    }

    @Test
    void testConfig2_OpenGates() {
        val config = new Configuration("(0 8 2)(1 6 4)(3 7 5)");
        assertEquals(Set.of(0), config.getOpenGates());
        assertFalse(config.isFull());
    }

    @Test
    void testConfig2_Signature() {
        val config = new Configuration("(0 8 2)(1 6 4)(3 7 5)");
        val signature = config.getSignature();

        float[] expectedSignature = {1.0f, 2.0f, 1.0f, 3.0f, 2.0f, 3.0f, 2.0f, 3.0f, 1.0f};
        assertArrayEquals(expectedSignature, signature.getContent(), 0.0001f);
        assertFalse(signature.isMirror());
    }

    @Test
    void testConfig2_CanonicalSignature() {
        val config = new Configuration("(0 8 2)(1 6 4)(3 7 5)");
        val canonical = config.getCanonical();

        float[] expectedCanonicalSignature = {1.0f, 2.0f, 1.0f, 3.0f, 2.0f, 3.0f, 2.0f, 3.0f, 1.0f};
        assertArrayEquals(expectedCanonicalSignature, canonical.getSignature().getContent(), 0.0001f);
    }

    @Test
    void testConfig2_HashCode() {
        val config = new Configuration("(0 8 2)(1 6 4)(3 7 5)");
        assertEquals(-2052909137, config.hashCode());
    }

    @Test
    void testConfig2_EquivalentSignatures() {
        val config = new Configuration("(0 8 2)(1 6 4)(3 7 5)");
        val equivalentSignatures = config.getEquivalentSignatures()
                .map(sig -> Arrays.toString(sig.getContent()))
                .collect(Collectors.toList());

        val expectedSignatures = Arrays.asList(
            "[1.0, 2.0, 1.0, 3.0, 2.0, 3.0, 2.0, 3.0, 1.0]",
            "[1.0, 2.0, 3.0, 2.0, 3.0, 2.0, 1.0, 3.0, 1.0]",
            "[1.0, 2.0, 3.0, 1.0, 3.0, 1.0, 3.0, 2.0, 2.0]",
            "[1.0, 1.0, 2.0, 3.0, 2.0, 3.0, 2.0, 1.0, 3.0]",
            "[1.0, 2.0, 3.0, 2.0, 3.0, 2.0, 1.0, 1.0, 3.0]",
            "[1.0, 2.0, 2.0, 3.0, 1.0, 3.0, 1.0, 3.0, 2.0]",
            "[1.0, 2.0, 1.0, 2.0, 1.0, 3.0, 3.0, 2.0, 3.0]",
            "[1.0, 2.0, 1.0, 1.0, 3.0, 2.0, 3.0, 2.0, 3.0]",
            "[1.0, 2.0, 1.0, 2.0, 3.0, 3.0, 1.0, 3.0, 2.0]",
            "[1.0, 2.0, 3.0, 2.0, 2.0, 1.0, 3.0, 1.0, 3.0]",
            "[1.0, 2.0, 1.0, 3.0, 3.0, 2.0, 3.0, 1.0, 2.0]",
            "[1.0, 2.0, 3.0, 1.0, 3.0, 3.0, 2.0, 1.0, 2.0]",
            "[1.0, 2.0, 3.0, 3.0, 1.0, 3.0, 2.0, 1.0, 2.0]",
            "[1.0, 2.0, 1.0, 3.0, 2.0, 3.0, 3.0, 1.0, 2.0]",
            "[1.0, 2.0, 2.0, 3.0, 2.0, 1.0, 3.0, 1.0, 3.0]",
            "[1.0, 2.0, 1.0, 2.0, 3.0, 1.0, 3.0, 3.0, 2.0]",
            "[1.0, 1.0, 2.0, 1.0, 3.0, 2.0, 3.0, 2.0, 3.0]",
            "[1.0, 2.0, 1.0, 2.0, 1.0, 3.0, 2.0, 3.0, 3.0]"
        );

        assertEquals(expectedSignatures.size(), equivalentSignatures.size());
        for (String expected : expectedSignatures) {
            assertTrue(equivalentSignatures.contains(expected),
                    "Missing expected signature: " + expected);
        }
    }

    // ========================================
    // Configuration 3: (0 5 1 11 3)(2 12 9 8 4)(6 10 7)
    // ========================================

    @Test
    void testConfig3_3Norm() {
        val config = new Configuration("(0 5 1 11 3)(2 12 9 8 4)(6 10 7)");
        assertEquals(5, config.get3Norm());
    }

    @Test
    void testConfig3_OpenGates() {
        val config = new Configuration("(0 5 1 11 3)(2 12 9 8 4)(6 10 7)");
        assertEquals(Set.of(7, 9), config.getOpenGates());
        assertFalse(config.isFull());
    }

    @Test
    void testConfig3_Signature() {
        val config = new Configuration("(0 5 1 11 3)(2 12 9 8 4)(6 10 7)");
        val signature = config.getSignature();

        float[] expectedSignature = {1.01f, 1.03f, 2.0f, 1.05f, 2.0f, 1.02f, 3.0f, 3.0f, 2.0f, 2.0f, 3.0f, 1.04f, 2.0f};
        assertArrayEquals(expectedSignature, signature.getContent(), 0.0001f);
        assertFalse(signature.isMirror());
    }

    @Test
    void testConfig3_CanonicalSignature() {
        val config = new Configuration("(0 5 1 11 3)(2 12 9 8 4)(6 10 7)");
        val canonical = config.getCanonical();

        float[] expectedCanonicalSignature = {1.01f, 1.03f, 2.0f, 1.05f, 2.0f, 1.02f, 3.0f, 3.0f, 2.0f, 2.0f, 3.0f, 1.04f, 2.0f};
        assertArrayEquals(expectedCanonicalSignature, canonical.getSignature().getContent(), 0.0001f);
    }

    @Test
    void testConfig3_HashCode() {
        val config = new Configuration("(0 5 1 11 3)(2 12 9 8 4)(6 10 7)");
        assertEquals(-2018071758, config.hashCode());
    }

    @Test
    void testConfig3_EquivalentSignatures() {
        val config = new Configuration("(0 5 1 11 3)(2 12 9 8 4)(6 10 7)");
        val equivalentSignatures = config.getEquivalentSignatures()
                .map(sig -> Arrays.toString(sig.getContent()))
                .collect(Collectors.toList());

        val expectedSignatures = Arrays.asList(
            "[1.01, 1.03, 2.0, 1.05, 2.0, 1.02, 3.0, 3.0, 2.0, 2.0, 3.0, 1.04, 2.0]",
            "[1.0, 2.01, 3.0, 1.0, 1.0, 3.0, 3.0, 2.03, 1.0, 2.05, 1.0, 2.02, 2.04]",
            "[1.01, 2.0, 1.03, 2.0, 1.05, 3.0, 3.0, 2.0, 2.0, 3.0, 1.02, 2.0, 1.04]",
            "[1.01, 2.0, 1.03, 3.0, 2.0, 2.0, 3.0, 3.0, 1.05, 2.0, 1.02, 2.0, 1.04]",
            "[1.0, 2.01, 1.0, 2.03, 3.0, 3.0, 1.0, 1.0, 3.0, 2.05, 1.0, 2.02, 2.04]",
            "[1.01, 1.03, 2.0, 1.05, 3.0, 2.0, 2.0, 3.0, 3.0, 1.02, 2.0, 1.04, 2.0]",
            "[1.01, 2.0, 1.03, 3.0, 3.0, 2.0, 2.0, 3.0, 1.05, 2.0, 1.02, 1.04, 2.0]",
            "[1.0, 2.01, 2.03, 1.0, 2.05, 3.0, 1.0, 1.0, 3.0, 3.0, 2.02, 1.0, 2.04]",
            "[1.0, 2.01, 3.0, 3.0, 1.0, 1.0, 3.0, 2.03, 1.0, 2.05, 2.02, 1.0, 2.04]",
            "[1.01, 2.0, 1.03, 1.05, 2.0, 1.02, 3.0, 2.0, 2.0, 3.0, 3.0, 1.04, 2.0]",
            "[1.01, 2.0, 2.0, 3.0, 3.0, 2.0, 1.03, 3.0, 1.05, 1.02, 3.0, 1.04, 3.0]",
            "[1.0, 2.01, 1.0, 2.03, 2.05, 1.0, 2.02, 3.0, 1.0, 1.0, 3.0, 3.0, 2.04]",
            "[1.0, 1.0, 2.0, 2.0, 1.0, 3.01, 2.0, 3.03, 3.05, 2.0, 3.02, 2.0, 3.04]",
            "[1.01, 2.0, 1.03, 2.0, 1.05, 1.02, 2.0, 1.04, 3.0, 2.0, 2.0, 3.0, 3.0]",
            "[1.0, 2.0, 2.0, 1.0, 3.01, 2.0, 3.03, 3.05, 2.0, 3.02, 2.0, 3.04, 1.0]",
            "[1.0, 2.01, 3.0, 2.03, 3.0, 2.05, 2.02, 3.0, 2.04, 1.0, 3.0, 3.0, 1.0]",
            "[1.0, 1.0, 2.0, 3.01, 1.0, 3.03, 3.05, 1.0, 3.02, 1.0, 3.04, 2.0, 2.0]",
            "[1.0, 1.0, 2.01, 3.0, 2.03, 3.0, 2.05, 2.02, 3.0, 2.04, 1.0, 3.0, 3.0]",
            "[1.0, 2.0, 3.01, 1.0, 3.03, 3.05, 1.0, 3.02, 1.0, 3.04, 2.0, 2.0, 1.0]",
            "[1.0, 2.0, 2.0, 3.01, 1.0, 3.03, 1.0, 3.05, 3.02, 1.0, 3.04, 2.0, 1.0]",
            "[1.0, 2.01, 3.0, 2.03, 2.05, 3.0, 2.02, 3.0, 2.04, 1.0, 1.0, 3.0, 3.0]",
            "[1.0, 1.0, 2.0, 2.0, 3.01, 1.0, 3.03, 1.0, 3.05, 3.02, 1.0, 3.04, 2.0]",
            "[1.01, 2.0, 1.03, 1.05, 2.0, 1.02, 2.0, 1.04, 3.0, 3.0, 2.0, 2.0, 3.0]",
            "[1.0, 2.0, 2.0, 1.0, 1.0, 3.01, 2.0, 3.03, 2.0, 3.05, 3.02, 2.0, 3.04]",
            "[1.0, 2.01, 2.03, 1.0, 2.05, 1.0, 2.02, 3.0, 3.0, 1.0, 1.0, 3.0, 2.04]",
            "[1.01, 2.0, 3.0, 3.0, 2.0, 2.0, 1.03, 3.0, 1.05, 3.0, 1.02, 1.04, 3.0]"
        );

        assertEquals(expectedSignatures.size(), equivalentSignatures.size());
        for (String expected : expectedSignatures) {
            assertTrue(equivalentSignatures.contains(expected),
                    "Missing expected signature: " + expected);
        }
    }

    // ========================================
    // Cross-configuration tests
    // ========================================

    @Test
    void testEqualityBetweenSameConfigurations() {
        val config1a = new Configuration("(0 9 7)(1 5 2 6 3)(4 10 8)");
        val config1b = new Configuration("(0 9 7)(1 5 2 6 3)(4 10 8)");

        assertEquals(config1a, config1b);
        assertEquals(config1a.hashCode(), config1b.hashCode());
    }

    @Test
    void testInequalityBetweenDifferentConfigurations() {
        val config1 = new Configuration("(0 9 7)(1 5 2 6 3)(4 10 8)");
        val config2 = new Configuration("(0 8 2)(1 6 4)(3 7 5)");
        val config3 = new Configuration("(0 5 1 11 3)(2 12 9 8 4)(6 10 7)");

        assertNotEquals(config1, config2);
        assertNotEquals(config1, config3);
        assertNotEquals(config2, config3);
    }

    @Test
    void testSignatureRoundTrip_AllConfigurations() {
        String[] inputs = {
            "(0 9 7)(1 5 2 6 3)(4 10 8)",
            "(0 8 2)(1 6 4)(3 7 5)",
            "(0 5 1 11 3)(2 12 9 8 4)(6 10 7)"
        };

        for (String input : inputs) {
            val original = new Configuration(input);
            val signature = original.getSignature().getContent();
            val reconstructed = Configuration.ofSignature(signature);

            assertArrayEquals(signature, reconstructed.getSignature().getContent(), 0.0001f,
                    "Round-trip failed for: " + input);
        }
    }

    @Test
    void testCanonicalIsIdempotent() {
        String[] inputs = {
            "(0 9 7)(1 5 2 6 3)(4 10 8)",
            "(0 8 2)(1 6 4)(3 7 5)",
            "(0 5 1 11 3)(2 12 9 8 4)(6 10 7)"
        };

        for (String input : inputs) {
            val config = new Configuration(input);
            val canonical1 = config.getCanonical();
            val canonical2 = config.getCanonical();

            assertSame(canonical1, canonical2, "getCanonical should return the same instance (cached)");
        }
    }
}
