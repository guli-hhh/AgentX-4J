package com.agentx4j.core.exception;

import com.agentx4j.core.model.PaymentRequirement;
import lombok.Getter;

import java.util.List;

/**
 * 支付要求异常。
 *
 * <p>当请求需要支付但未提供有效支付时抛出。
 * 包含支付要求列表，告诉调用方需要如何支付。</p>
 *
 * <p>对应 HTTP 402 Payment Required。</p>
 */
@Getter
public class PaymentRequiredException extends AgentX4JException {

    /** 支付要求列表 */
    private final List<PaymentRequirement> requirements;

    /** 受保护的资源路径 */
    private final String resource;

    public PaymentRequiredException(String message, List<PaymentRequirement> requirements, String resource) {
        super("PAYMENT_REQUIRED", message);
        this.requirements = requirements;
        this.resource = resource;
    }

    public PaymentRequiredException(List<PaymentRequirement> requirements, String resource) {
        this("Payment is required to access this resource", requirements, resource);
    }
}
