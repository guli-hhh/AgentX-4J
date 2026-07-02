package com.agentx4j.core.enums;

/**
 * 计费方案枚举
 *
 * <p>定义三种计费模式，对应 x402 协议的三种 scheme。</p>
 *
 * <ul>
 *   <li>{@link #EXACT} — 固定价格，调用前明确知道费用</li>
 *   <li>{@link #UPTO} — 按用量上限授权，按实际用量结算</li>
 *   <li>{@link #BATCH_SETTLEMENT} — 一次存款，链下凭证，批量链上结算</li>
 * </ul>
 */
public enum BillingScheme {

    /**
     * 固定价格 — 调用前明确知道费用。
     * <p>适用于标准化工具（翻译/天气/搜索）。</p>
     * <p>类比：一碗面 $5，吃之前就知道多少钱。</p>
     */
    EXACT("exact"),

    /**
     * 按用量 — 授权上限，按实际用量结算。
     * <p>适用于 LLM 推理、计算服务等非确定用量场景。</p>
     * <p>类比：自助餐，进门说最多花 $50，吃多少按实际算。</p>
     */
    UPTO("upto"),

    /**
     * 批量结算 — 一次存款 + 链下凭证 + 批量链上结算。
     * <p>适用于高频微支付场景。</p>
     * <p>类比：充 $100 会员卡，每次刷卡不付现金，月底统一结算。</p>
     */
    BATCH_SETTLEMENT("batch-settlement");

    private final String schemeName;

    BillingScheme(String schemeName) {
        this.schemeName = schemeName;
    }

    public String getSchemeName() {
        return schemeName;
    }

    public boolean isExact() {
        return this == EXACT;
    }

    public boolean isUpto() {
        return this == UPTO;
    }

    public boolean isBatchSettlement() {
        return this == BATCH_SETTLEMENT;
    }

    /**
     * 根据 x402 协议中的 scheme 字符串获取枚举。
     *
     * @param schemeName x402 协议中的 scheme 名称
     * @return 对应的枚举值
     * @throws IllegalArgumentException 如果 scheme 名称未知
     */
    public static BillingScheme fromSchemeName(String schemeName) {
        for (BillingScheme scheme : values()) {
            if (scheme.schemeName.equals(schemeName)) {
                return scheme;
            }
        }
        throw new IllegalArgumentException("Unknown billing scheme: " + schemeName);
    }
}
