package com.agentx4j.core.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PaymentRequirement 模型测试。
 */
class PaymentRequirementTest {

    @Test
    void testIsExactScheme() {
        PaymentRequirement req = PaymentRequirement.builder()
                .scheme("exact")
                .network("eip155:84532")
                .amount("1000")
                .build();

        assertTrue(req.isExact());
        assertFalse(req.isUpto());
        assertFalse(req.isBatchSettlement());
    }

    @Test
    void testIsUptoScheme() {
        PaymentRequirement req = PaymentRequirement.builder()
                .scheme("upto")
                .network("eip155:84532")
                .amount("5000")
                .build();

        assertFalse(req.isExact());
        assertTrue(req.isUpto());
    }

    @Test
    void testGetReadableAmount() {
        PaymentRequirement req = PaymentRequirement.builder()
                .amount("1000")
                .build();

        assertEquals("0.001", req.getReadableAmount(6));
        assertEquals("0.00001", req.getReadableAmount(8));
    }

    @Test
    void testGetTokenName() {
        PaymentRequirement req = PaymentRequirement.builder()
                .extra(java.util.Map.of("name", "USD Coin", "version", "2"))
                .build();

        assertEquals("USD Coin", req.getTokenName());
        assertEquals("2", req.getTokenVersion());
    }

    @Test
    void testGetNetworkType() {
        PaymentRequirement req = PaymentRequirement.builder()
                .network("eip155:84532")
                .build();

        assertEquals(com.agentx4j.core.enums.NetworkType.EVM, req.getNetworkType());
    }
}
