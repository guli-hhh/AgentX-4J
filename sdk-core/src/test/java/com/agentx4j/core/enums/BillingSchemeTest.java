package com.agentx4j.core.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BillingScheme 枚举测试。
 */
class BillingSchemeTest {

    @Test
    void testSchemeNames() {
        assertEquals("exact", BillingScheme.EXACT.getSchemeName());
        assertEquals("upto", BillingScheme.UPTO.getSchemeName());
        assertEquals("batch-settlement", BillingScheme.BATCH_SETTLEMENT.getSchemeName());
    }

    @Test
    void testFromSchemeName() {
        assertEquals(BillingScheme.EXACT, BillingScheme.fromSchemeName("exact"));
        assertEquals(BillingScheme.UPTO, BillingScheme.fromSchemeName("upto"));
        assertEquals(BillingScheme.BATCH_SETTLEMENT, BillingScheme.fromSchemeName("batch-settlement"));
    }

    @Test
    void testFromSchemeNameInvalid() {
        assertThrows(IllegalArgumentException.class, () -> BillingScheme.fromSchemeName("unknown"));
    }

    @Test
    void testConvenienceMethods() {
        assertTrue(BillingScheme.EXACT.isExact());
        assertFalse(BillingScheme.EXACT.isUpto());
        assertFalse(BillingScheme.EXACT.isBatchSettlement());

        assertTrue(BillingScheme.UPTO.isUpto());
        assertTrue(BillingScheme.BATCH_SETTLEMENT.isBatchSettlement());
    }
}
