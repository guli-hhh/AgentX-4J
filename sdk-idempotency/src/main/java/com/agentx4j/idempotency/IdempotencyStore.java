package com.agentx4j.idempotency;

import java.time.Duration;

/**
 * 幂等存储接口。
 *
 * <p>用于缓存 paymentId → response 映射，防止重复处理支付请求。</p>
 *
 * <p>支持多种后端实现：内存（Caffeine）、Redis、数据库等。</p>
 */
public interface IdempotencyStore {

    /**
     * 保存幂等记录。
     *
     * @param paymentId       支付 ID
     * @param requestFingerprint 请求指纹
     * @param result          结果数据
     * @param ttl             过期时间
     */
    void save(String paymentId, String requestFingerprint, Object result, Duration ttl);

    /**
     * 获取缓存的结果。
     *
     * @param paymentId 支付 ID
     * @return 缓存的条目，如果不存在则返回 null
     */
    IdempotencyEntry get(String paymentId);

    /**
     * 删除幂等记录。
     *
     * @param paymentId 支付 ID
     */
    void delete(String paymentId);

    /**
     * 清空所有幂等记录。
     */
    void clear();
}
