package com.agentx4j.starter.annotation;

import java.lang.annotation.*;

/**
 * 自动结算注解。
 *
 * <p>标注在方法上，该方法执行完成后自动触发支付结算。</p>
 *
 * <p>通常与 {@link X402Protected} 配合使用，但也可以单独使用。</p>
 *
 * <p>使用方式：</p>
 * <pre>{@code
 * @AutoSettle
 * public void handleSettlement(String transactionId) {
 *     // 结算逻辑
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AutoSettle {

    /** 结算模式 */
    SettlementMode mode() default SettlementMode.AUTO;

    public enum SettlementMode {
        /** 自动结算（支付验证通过后立即结算） */
        AUTO,
        /** 手动结算（需要显式调用） */
        MANUAL,
        /** 批量结算（累积多笔后统一结算） */
        BATCH
    }
}
