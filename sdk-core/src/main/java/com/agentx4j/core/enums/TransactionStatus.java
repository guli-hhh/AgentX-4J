package com.agentx4j.core.enums;

/**
 * 交易状态枚举。
 *
 * <p>交易生命周期：</p>
 * <pre>
 * PENDING → SETTLED → COMPLETED
 *    ↓         ↓
 * FAILED   REFUNDED
 *    ↓
 * DISPUTED
 * </pre>
 */
public enum TransactionStatus {

    /**
     * 待处理 — 交易已创建，等待链上结算。
     */
    PENDING,

    /**
     * 已结算 — 链上交易已确认，资金已转移。
     */
    SETTLED,

    /**
     * 已完成 — 交易全流程结束（结算 + 通知 + 记账）。
     */
    COMPLETED,

    /**
     * 失败 — 交易执行过程中出错（余额不足、签名无效等）。
     */
    FAILED,

    /**
     * 已退款 — 交易完成后被退款（全额或部分）。
     */
    REFUNDED,

    /**
     * 争议中 — 交易存在争议，等待人工处理。
     */
    DISPUTED;
}
