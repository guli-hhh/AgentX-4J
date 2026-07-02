package com.agentx4j.starter.annotation;

import com.agentx4j.core.enums.BillingScheme;

import java.lang.annotation.*;

/**
 * 声明接口需要支付才能访问。
 *
 * <p>标注在 Controller 方法上，该方法将受到 x402 支付保护。</p>
 *
 * <p>使用方式：</p>
 * <pre>{@code
 * @X402Protected(price = "$0.001", network = "eip155:84532")
 * @GetMapping("/api/data")
 * public Map<String, Object> getData() {
 *     // 如果执行到这里，说明支付已经验证通过
 *     return Map.of("result", "your paid data");
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface X402Protected {

    /**
     * 价格（美元字符串，如 "$0.001"）
     *
     * @return 价格字符串
     */
    String price();

    /**
     * 网络（CAIP-2 标识，如 "eip155:84532"）
     *
     * @return 网络标识
     */
    String network() default "eip155:84532";

    /**
     * 代币合约地址（可选，默认使用全局配置）
     *
     * @return 代币合约地址
     */
    String asset() default "";

    /**
     * 收款地址（可选，默认使用全局配置）
     *
     * @return 收款地址
     */
    String payTo() default "";

    /**
     * 计费方案
     *
     * @return 计费方案
     */
    BillingScheme scheme() default BillingScheme.EXACT;

    /**
     * 超时时间（秒）
     *
     * @return 超时秒数
     */
    long maxTimeoutSeconds() default 60;
}
