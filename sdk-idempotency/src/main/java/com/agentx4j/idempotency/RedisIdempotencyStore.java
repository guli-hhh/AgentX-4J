package com.agentx4j.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;

/**
 * 基于 Redis 的幂等存储实现。
 *
 * <p>适用于多实例部署，确保跨节点的幂等性。</p>
 *
 * <p>使用方式：</p>
 * <pre>{@code
 * // 需要 Spring Data Redis 的 RedisTemplate
 * RedisIdempotencyStore store = new RedisIdempotencyStore(redisTemplate, Duration.ofHours(24));
 * }</pre>
 *
 * <p>注意：此类仅在 classpath 中存在 Spring Data Redis 时可用。</p>
 */
public class RedisIdempotencyStore implements IdempotencyStore {

    private static final String KEY_PREFIX = "agentx4j:idempotency:";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final org.springframework.data.redis.core.RedisTemplate<String, String> redisTemplate;
    private final Duration defaultTtl;

    public RedisIdempotencyStore(org.springframework.data.redis.core.RedisTemplate<String, String> redisTemplate,
                                  Duration defaultTtl) {
        this.redisTemplate = redisTemplate;
        this.defaultTtl = defaultTtl;
    }

    @Override
    public void save(String paymentId, String requestFingerprint, Object result, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(result);
            IdempotencyEntry entry = IdempotencyEntry.builder()
                    .paymentId(paymentId)
                    .fingerprint(requestFingerprint)
                    .result(json)
                    .createdAt(java.time.Instant.now())
                    .expiresAt(java.time.Instant.now().plus(ttl != null ? ttl : defaultTtl))
                    .build();
            String entryJson = objectMapper.writeValueAsString(entry);
            redisTemplate.opsForValue().set(
                    KEY_PREFIX + paymentId,
                    entryJson,
                    ttl != null ? ttl : defaultTtl
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize idempotency entry", e);
        }
    }

    @Override
    public IdempotencyEntry get(String paymentId) {
        try {
            String json = redisTemplate.opsForValue().get(KEY_PREFIX + paymentId);
            if (json == null) {
                return null;
            }
            IdempotencyEntry entry = objectMapper.readValue(json, IdempotencyEntry.class);
            if (entry.isExpired()) {
                delete(paymentId);
                return null;
            }
            return entry;
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @Override
    public void delete(String paymentId) {
        redisTemplate.delete(KEY_PREFIX + paymentId);
    }

    @Override
    public void clear() {
        // 注意：生产环境慎用，可能影响其他 key
        var keys = redisTemplate.keys(KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
