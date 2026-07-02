package com.agentx4j.x402.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SettlementOverride 测试。
 */
class SettlementOverrideTest {

    @BeforeEach
    void setUp() {
        SettlementOverride.clear();
    }

    @AfterEach
    void tearDown() {
        SettlementOverride.clear();
    }

    @Test
    void testSetAndGet() {
        SettlementOverride.set("500");
        assertTrue(SettlementOverride.isSet());
        assertEquals("500", SettlementOverride.get());
    }

    @Test
    void testSetPercent() {
        SettlementOverride.setPercent(50);
        assertEquals("50%", SettlementOverride.get());
    }

    @Test
    void testSetDollar() {
        SettlementOverride.setDollar("0.003");
        assertEquals("3000", SettlementOverride.get()); // $0.003 = 3000 原子单位
    }

    @Test
    void testSetDollarWithSign() {
        SettlementOverride.setDollar("$0.005");
        assertEquals("5000", SettlementOverride.get());
    }

    @Test
    void testSetDollarBigDecimal() {
        SettlementOverride.setDollar(new BigDecimal("0.01"));
        assertEquals("10000", SettlementOverride.get());
    }

    @Test
    void testClear() {
        SettlementOverride.set("500");
        assertTrue(SettlementOverride.isSet());
        SettlementOverride.clear();
        assertFalse(SettlementOverride.isSet());
        assertNull(SettlementOverride.get());
    }

    @Test
    void testResolveWithOverride() {
        SettlementOverride.set("300");
        String result = SettlementOverride.resolve("1000");
        assertEquals("300", result);
    }

    @Test
    void testResolveWithPercent() {
        SettlementOverride.setPercent(50);
        String result = SettlementOverride.resolve("1000");
        assertEquals("500", result);
    }

    @Test
    void testResolveWithoutOverride() {
        String result = SettlementOverride.resolve("1000");
        assertEquals("1000", result); // 返回原始授权上限
    }

    @Test
    void testThreadLocalIsolation() {
        SettlementOverride.set("100");
        assertEquals("100", SettlementOverride.get());

        // 新线程应该看不到这个值
        Thread thread = new Thread(() -> {
            assertNull(SettlementOverride.get());
            assertFalse(SettlementOverride.isSet());
        });
        try {
            thread.start();
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 原线程的值不受影响
        assertEquals("100", SettlementOverride.get());
    }
}
