package com.agentx4j.core.exception;

import lombok.Getter;

/**
 * 幂等冲突异常。
 *
 * <p>当同一 paymentId 被用于不同请求时抛出。</p>
 *
 * <p>这是幂等性保障的核心异常，防止 paymentId 被滥用。</p>
 */
@Getter
public class IdempotencyConflictException extends AgentX4JException {

    /** 冲突的 paymentId */
    private final String paymentId;

    public IdempotencyConflictException(String paymentId) {
        super("IDEMPOTENCY_CONFLICT",
                "Payment ID already used with a different request: " + paymentId);
        this.paymentId = paymentId;
    }
}
