package com.agentx4j.risk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RateLimitService 测试。
 */
class RateLimitServiceTest {

    private RateLimitService rateLimit;

    @BeforeEach
    void setUp() {
        // 桶容量 10，每秒补充 5 个令牌
        rateLimit = new RateLimitService(10, 5);
    }

    @Test
    void testTryAcquireWithinLimit() {
        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimit.tryAcquire("agent-1"), "Should acquire token " + i);
        }
    }

    @Test
    void testTryAcquireExceedsLimit() {
        // 消耗所有令牌
        for (int i = 0; i < 10; i++) {
            rateLimit.tryAcquire("agent-1");
        }
        // 第 11 次应该失败
        assertFalse(rateLimit.tryAcquire("agent-1"));
    }

    @Test
    void testIndependentKeys() {
        // 消耗 agent-1 的所有令牌
        for (int i = 0; i < 10; i++) {
            rateLimit.tryAcquire("agent-1");
        }
        assertFalse(rateLimit.tryAcquire("agent-1"));

        // agent-2 不受影响
        assertTrue(rateLimit.tryAcquire("agent-2"));
    }

    @Test
    void testGetRemainingTokens() {
        assertEquals(10, rateLimit.getRemainingTokens("agent-1"));
        rateLimit.tryAcquire("agent-1");
        rateLimit.tryAcquire("agent-1");
        assertEquals(8, rateLimit.getRemainingTokens("agent-1"));
    }

    @Test
    void testClear() {
        for (int i = 0; i < 10; i++) {
            rateLimit.tryAcquire("agent-1");
        }
        assertFalse(rateLimit.tryAcquire("agent-1"));

        rateLimit.clear("agent-1");
        assertTrue(rateLimit.tryAcquire("agent-1"));
    }
}
