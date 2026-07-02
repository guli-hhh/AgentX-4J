package com.agentx4j.core.enums;

/**
 * 风控决策动作枚举。
 *
 * <p>风控引擎评估后的处置方式。</p>
 */
public enum RiskAction {

    /**
     * 允许 — 风险可控，放行交易。
     */
    ALLOW,

    /**
     * 审核 — 需要人工介入确认。
     */
    REVIEW,

    /**
     * 阻断 — 风险过高，拒绝交易。
     */
    BLOCK
}
