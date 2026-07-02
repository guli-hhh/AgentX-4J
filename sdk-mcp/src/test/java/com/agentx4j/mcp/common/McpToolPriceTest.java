package com.agentx4j.mcp.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * McpToolPrice 测试。
 */
class McpToolPriceTest {

    @Test
    void testGetAtomicAmount() {
        McpToolPrice price = McpToolPrice.builder()
                .price("$0.001")
                .build();

        assertEquals("1000", price.getAtomicAmount(6)); // USDC 6 位小数
    }

    @Test
    void testGetAtomicAmountLarge() {
        McpToolPrice price = McpToolPrice.builder()
                .price("$1.50")
                .build();

        assertEquals("1500000", price.getAtomicAmount(6));
    }

    @Test
    void testGetAtomicAmountWithoutDollarSign() {
        McpToolPrice price = McpToolPrice.builder()
                .price("0.005")
                .build();

        assertEquals("5000", price.getAtomicAmount(6));
    }
}
