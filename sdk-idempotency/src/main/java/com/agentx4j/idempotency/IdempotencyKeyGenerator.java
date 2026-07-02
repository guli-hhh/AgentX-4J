package com.agentx4j.idempotency;

import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * 幂等 Key 生成器。
 *
 * <p>生成加密安全的唯一支付 ID，格式：{@code pay_<32字符hex>}。</p>
 *
 * <p>使用 SecureRandom 确保不可预测性。</p>
 */
public class IdempotencyKeyGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String DEFAULT_PREFIX = "pay_";
    private static final int RANDOM_BYTES = 16; // 128 bit

    /**
     * 生成默认前缀的幂等 Key。
     *
     * @return 如 "pay_7d5d747be160e280504c099d984bcfe0"
     */
    public static String generate() {
        return generate(DEFAULT_PREFIX);
    }

    /**
     * 生成自定义前缀的幂等 Key。
     *
     * @param prefix 前缀（如 "order_"）
     * @return 如 "order_7d5d747be160e280504c099d984bcfe0"
     */
    public static String generate(String prefix) {
        byte[] bytes = new byte[RANDOM_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return prefix + HexFormat.of().formatHex(bytes);
    }

    /**
     * 验证幂等 Key 格式是否合法。
     *
     * @param key 待验证的 Key
     * @return true 如果格式合法
     */
    public static boolean isValid(String key) {
        if (key == null || key.length() < 16 || key.length() > 128) {
            return false;
        }
        // 只允许字母、数字、下划线、连字符
        return key.matches("^[a-zA-Z0-9_-]+$");
    }
}
