package com.agentx4j.core.exception;

import lombok.Getter;

/**
 * 支付验证异常。
 *
 * <p>当支付签名验证失败时抛出。</p>
 */
@Getter
public class PaymentVerificationException extends AgentX4JException {

    /** 验证失败原因 */
    private final String reason;

    public PaymentVerificationException(String reason) {
        super("PAYMENT_VERIFICATION_FAILED", "Payment verification failed: " + reason);
        this.reason = reason;
    }

    public PaymentVerificationException(String reason, Throwable cause) {
        super("PAYMENT_VERIFICATION_FAILED", "Payment verification failed: " + reason, cause);
        this.reason = reason;
    }
}
