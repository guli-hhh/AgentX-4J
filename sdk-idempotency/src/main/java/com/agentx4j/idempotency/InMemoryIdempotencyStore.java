package com.agentx4j.idempotency;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;

/**
 * 基于内存（Caffeine）的幂等存储实现。
 *
 * <p>适用于单实例部署。多实例部署请使用 Redis 实现。</p>
 *
 * <p>Caffeine 是一个高性能的 Java 缓存库，支持 TTL 过期、LRU 淘汰等策略。</p>
 */
public class InMemoryIdempotencyStore implements IdempotencyStore {

    private final Cache<String, IdempotencyEntry> cache;

    public InMemoryIdempotencyStore() {
        this.cache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .build();
    }

    public InMemoryIdempotencyStore(long maxSize) {
        this.cache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .build();
    }

    @Override
    public void save(String paymentId, String requestFingerprint, Object result, Duration ttl) {
        IdempotencyEntry entry = IdempotencyEntry.builder()
                .paymentId(paymentId)
                .fingerprint(requestFingerprint)
                .result(result)
                .createdAt(java.time.Instant.now())
                .expiresAt(java.time.Instant.now().plus(ttl))
                .build();
        cache.put(paymentId, entry);
    }

    @Override
    public IdempotencyEntry get(String paymentId) {
        IdempotencyEntry entry = cache.getIfPresent(paymentId);
        if (entry != null && entry.isExpired()) {
            cache.invalidate(paymentId);
            return null;
        }
        return entry;
    }

    @Override
    public void delete(String paymentId) {
        cache.invalidate(paymentId);
    }

    @Override
    public void clear() {
        cache.invalidateAll();
    }
}
