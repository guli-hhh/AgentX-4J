package com.agentx4j.core.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NetworkType 枚举测试。
 */
class NetworkTypeTest {

    @Test
    void testCaip2Prefixes() {
        assertEquals("eip155", NetworkType.EVM.getCaip2Prefix());
        assertEquals("solana", NetworkType.SVM.getCaip2Prefix());
        assertEquals("tvm", NetworkType.TVM.getCaip2Prefix());
    }

    @Test
    void testFromCaip2Id() {
        assertEquals(NetworkType.EVM, NetworkType.fromCaip2Id("eip155:84532"));
        assertEquals(NetworkType.EVM, NetworkType.fromCaip2Id("eip155:1"));
        assertEquals(NetworkType.SVM, NetworkType.fromCaip2Id("solana:5eykt4UsFv8P8NJdTREpY1vzqKqZKvdp"));
    }

    @Test
    void testFromCaip2IdInvalid() {
        assertThrows(IllegalArgumentException.class, () -> NetworkType.fromCaip2Id("invalid"));
        assertThrows(IllegalArgumentException.class, () -> NetworkType.fromCaip2Id(null));
    }
}
