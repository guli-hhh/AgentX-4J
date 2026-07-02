package com.agentx4j.core.constant;

/**
 * SDK 常量定义。
 */
public final class SdkConstants {

    private SdkConstants() {}

    // ==================== HTTP Headers (x402 协议) ====================

    /** x402 支付要求响应头（Base64 编码的 PaymentRequirements） */
    public static final String HEADER_PAYMENT_REQUIRED = "PAYMENT-REQUIRED";

    /** x402 支付签名请求头（Base64 编码的 PaymentPayload） */
    public static final String HEADER_PAYMENT_SIGNATURE = "PAYMENT-SIGNATURE";

    /** x402 支付响应头（Base64 编码的 PaymentResponse） */
    public static final String HEADER_PAYMENT_RESPONSE = "PAYMENT-RESPONSE";

    /** x402 扩展响应头 */
    public static final String HEADER_EXTENSION_RESPONSES = "EXTENSION-RESPONSES";

    // ==================== 默认值 ====================

    /** 默认网络（Base Sepolia 测试网） */
    public static final String DEFAULT_NETWORK = "eip155:84532";

    /** 默认代币（Base Sepolia USDC） */
    public static final String DEFAULT_ASSET = "0x036CbD53842c5426634e7929541eC2318f3dCF7e";

    /** 默认超时时间（秒） */
    public static final long DEFAULT_TIMEOUT_SECONDS = 60;

    /** 默认代币小数位数（USDC = 6） */
    public static final int DEFAULT_TOKEN_DECIMALS = 6;

    /** 默认 Facilitator URL（测试网） */
    public static final String DEFAULT_FACILITATOR_URL = "https://x402.org/facilitator";

    // ==================== 幂等性 ====================

    /** 幂等 Key 前缀 */
    public static final String IDEMPOTENCY_KEY_PREFIX = "pay_";

    /** 幂等 Key 最小长度 */
    public static final int IDEMPOTENCY_KEY_MIN_LENGTH = 16;

    /** 幂等 Key 最大长度 */
    public static final int IDEMPOTENCY_KEY_MAX_LENGTH = 128;

    /** 幂等缓存默认 TTL（毫秒） */
    public static final long IDEMPOTENCY_DEFAULT_TTL_MS = 24 * 60 * 60 * 1000; // 24 小时

    // ==================== 版本 ====================

    /** SDK 版本 */
    public static final String SDK_VERSION = "1.0.0-SNAPSHOT";

    /** 支持的 x402 协议版本 */
    public static final int X402_PROTOCOL_VERSION = 2;
}
