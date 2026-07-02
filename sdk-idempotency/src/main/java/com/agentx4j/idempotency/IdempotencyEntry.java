package com.agentx4j.idempotency;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 幂等记录条目。
 *
 * <p>存储一次支付请求的结果，用于后续重试时返回缓存结果。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyEntry {

    /** 支付 ID */
    private String paymentId;

    /** 请求指纹（防止 paymentId 被用于不同请求） */
    private String fingerprint;

    /** 缓存的结果数据 */
    private Object result;

    /** 创建时间 */
    private Instant createdAt;

    /** 过期时间 */
    private Instant expiresAt;

    /**
     * 检查是否已过期。
     *
     * @return true 如果已过期
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
