package com.agentx4j.idempotency;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * IdempotencyKeyGenerator 测试。
 */
class IdempotencyKeyGeneratorTest {

    @Test
    void testGenerateDefaultPrefix() {
        String key = IdempotencyKeyGenerator.generate();
        assertNotNull(key);
        assertTrue(key.startsWith("pay_"));
        assertTrue(key.length() > 16);
    }

    @Test
    void testGenerateCustomPrefix() {
        String key = IdempotencyKeyGenerator.generate("order_");
        assertTrue(key.startsWith("order_"));
    }

    @Test
    void testUniqueness() {
        String key1 = IdempotencyKeyGenerator.generate();
        String key2 = IdempotencyKeyGenerator.generate();
        assertNotEquals(key1, key2);
    }

    @Test
    void testIsValid() {
        assertTrue(IdempotencyKeyGenerator.isValid("pay_abc123def456"));
        assertTrue(IdempotencyKeyGenerator.isValid("order_1234567890abcdef"));
        assertFalse(IdempotencyKeyGenerator.isValid("ab")); // too short
        assertFalse(IdempotencyKeyGenerator.isValid("")); // empty
        assertFalse(IdempotencyKeyGenerator.isValid(null)); // null
        assertFalse(IdempotencyKeyGenerator.isValid("pay_abc def")); // space
    }
}
