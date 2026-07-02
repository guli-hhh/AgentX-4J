package com.agentx4j.risk;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 限流服务 — 基于 Token Bucket 算法。
 *
 * <p>Token Bucket 原理：</p>
 * <ul>
 *   <li>桶里放令牌，每秒钟补充固定数量</li>
 *   <li>每次请求消耗一个令牌</li>
 *   <li>桶空了 → 拒绝请求（等待补充）</li>
 * </ul>
 *
 * <p>支持维度：</p>
 * <ul>
 *   <li>Agent 级：每个 Agent 独立限流</li>
 *   <li>全局级：整个系统总体限流</li>
 *   <li>Agent 对级：两个 Agent 之间限流</li>
 * </ul>
 */
public class RateLimitService {

    private final int maxTokens;
    private final int refillRate;
    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public RateLimitService(int maxTokens, int refillRate) {
        this.maxTokens = maxTokens;
        this.refillRate = refillRate;
    }

    /**
     * 检查是否允许通过。
     *
     * @param key   限流 key（如 agent ID）
     * @param tokens 消耗的令牌数
     * @return true 如果允许通过
     */
    public boolean tryAcquire(String key, int tokens) {
        TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(maxTokens, refillRate));
        return bucket.tryAcquire(tokens);
    }

    /**
     * 检查是否允许通过（消耗 1 个令牌）。
     */
    public boolean tryAcquire(String key) {
        return tryAcquire(key, 1);
    }

    /**
     * 获取当前剩余令牌数。
     */
    public long getRemainingTokens(String key) {
        TokenBucket bucket = buckets.get(key);
        return bucket != null ? bucket.getTokens() : maxTokens;
    }

    /**
     * 清除指定 key 的限流桶。
     */
    public void clear(String key) {
        buckets.remove(key);
    }

    /**
     * 清除所有限流桶。
     */
    public void clearAll() {
        buckets.clear();
    }

    // ==================== 内部类 ====================

    /**
     * Token Bucket 实现。
     */
    private static class TokenBucket {
        private final int maxTokens;
        private final int refillRate;
        private AtomicLong tokens;
        private volatile long lastRefillTime;

        TokenBucket(int maxTokens, int refillRate) {
            this.maxTokens = maxTokens;
            this.refillRate = refillRate;
            this.tokens = new AtomicLong(maxTokens);
            this.lastRefillTime = System.currentTimeMillis();
        }

        synchronized boolean tryAcquire(int requestedTokens) {
            refill();
            long currentTokens = tokens.get();
            if (currentTokens >= requestedTokens) {
                tokens.addAndGet(-requestedTokens);
                return true;
            }
            return false;
        }

        long getTokens() {
            refill();
            return tokens.get();
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long elapsedMs = now - lastRefillTime;
            if (elapsedMs <= 0) return;

            long tokensToAdd = (elapsedMs * refillRate) / 1000;
            if (tokensToAdd <= 0) return;

            long newTokens = Math.min(maxTokens, tokens.get() + tokensToAdd);
            tokens.set(newTokens);
            lastRefillTime = now;
        }
    }
}
