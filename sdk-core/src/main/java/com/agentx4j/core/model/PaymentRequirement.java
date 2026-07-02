package com.agentx4j.core.model;

import com.agentx4j.core.enums.BillingScheme;
import com.agentx4j.core.enums.NetworkType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Map;

/**
 * x402 PaymentRequirement — 支付要求。
 *
 * <p>对应 x402 协议中 402 响应的 accepts[] 项。
 * 这是 Resource Server 告诉 Client "你需要怎么付钱"的标准格式。</p>
 *
 * <p>类比：PaymentRequirement ≈ 餐厅菜单上的"菜品 + 价格 + 支付方式"</p>
 *
 * <p>JSON 示例：</p>
 * <pre>{@code
 * {
 *   "scheme": "exact",
 *   "network": "eip155:84532",
 *   "amount": "1000",
 *   "asset": "0x036CbD53842c5426634e7929541eC2318f3dCF7e",
 *   "payTo": "0xYourAddress",
 *   "maxTimeoutSeconds": 60,
 *   "extra": { "name": "USD Coin", "version": "2" }
 * }
 * }</pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequirement {

    /** 计费方案: "exact" / "upto" / "batch-settlement" */
    private String scheme;

    /** CAIP-2 网络标识 (如 "eip155:8453" 表示 Base 主网) */
    private String network;

    /** 金额 (原子单位字符串, 如 "1000" = $0.001 USDC) */
    private String amount;

    /** 代币合约地址 (USDC 的合约地址) */
    private String asset;

    /** 收款地址 (卖家钱包地址) */
    private String payTo;

    /** 超时时间(秒), 超过后支付要求失效 */
    private Long maxTimeoutSeconds;

    /** 额外参数 (如代币名称 "USD Coin", 版本 "2") */
    private Map<String, String> extra;

    /** 扩展信息 (bazaar 发现信息, payment-identifier 等) */
    private Map<String, Object> extensions;

    // ==================== 便捷方法 ====================

    /** 是否为 exact 方案 */
    public boolean isExact() {
        return BillingScheme.EXACT.getSchemeName().equals(scheme);
    }

    /** 是否为 upto 方案 */
    public boolean isUpto() {
        return BillingScheme.UPTO.getSchemeName().equals(scheme);
    }

    /** 是否为 batch-settlement 方案 */
    public boolean isBatchSettlement() {
        return BillingScheme.BATCH_SETTLEMENT.getSchemeName().equals(scheme);
    }

    /** 获取计费方案枚举 */
    public BillingScheme getBillingScheme() {
        return BillingScheme.fromSchemeName(scheme);
    }

    /** 获取网络类型 */
    public NetworkType getNetworkType() {
        return NetworkType.fromCaip2Id(network);
    }

    /**
     * 将原子单位金额转为人类可读格式。
     *
     * @param decimals 代币小数位数（USDC 为 6）
     * @return 人类可读金额字符串（如 "0.001"）
     */
    public String getReadableAmount(int decimals) {
        return new BigDecimal(amount)
                .movePointLeft(decimals)
                .stripTrailingZeros()
                .toPlainString();
    }

    /**
     * 获取代币名称（从 extra 中读取）。
     *
     * @return 代币名称，如 "USD Coin"
     */
    public String getTokenName() {
        return extra != null ? extra.get("name") : null;
    }

    /**
     * 获取代币版本（从 extra 中读取）。
     *
     * @return 代币版本，如 "2"
     */
    public String getTokenVersion() {
        return extra != null ? extra.get("version") : null;
    }
}
