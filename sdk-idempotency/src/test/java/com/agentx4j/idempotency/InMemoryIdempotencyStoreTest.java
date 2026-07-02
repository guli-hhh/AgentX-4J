package com.agentx4j.idempotency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * InMemoryIdempotencyStore 测试。
 */
class InMemoryIdempotencyStoreTest {

    private InMemoryIdempotencyStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryIdempotencyStore();
    }

    @Test
    void testSaveAndGet() {
        store.save("pay_123", "fingerprint_abc", "result_data", Duration.ofHours(1));

        IdempotencyEntry entry = store.get("pay_123");
        assertNotNull(entry);
        assertEquals("pay_123", entry.getPaymentId());
        assertEquals("fingerprint_abc", entry.getFingerprint());
        assertEquals("result_data", entry.getResult());
        assertFalse(entry.isExpired());
    }

    @Test
    void testGetNonExistent() {
        assertNull(store.get("pay_nonexistent"));
    }

    @Test
    void testDelete() {
        store.save("pay_123", "fp", "result", Duration.ofHours(1));
        assertNotNull(store.get("pay_123"));

        store.delete("pay_123");
        assertNull(store.get("pay_123"));
    }

    @Test
    void testClear() {
        store.save("pay_1", "fp1", "r1", Duration.ofHours(1));
        store.save("pay_2", "fp2", "r2", Duration.ofHours(1));

        store.clear();
        assertNull(store.get("pay_1"));
        assertNull(store.get("pay_2"));
    }

    @Test
    void testExpiredEntry() {
        // 使用极短的 TTL
        store.save("pay_123", "fp", "result", Duration.ofMillis(1));
        // 等待过期
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // 过期后应返回 null
        assertNull(store.get("pay_123"));
    }
}
