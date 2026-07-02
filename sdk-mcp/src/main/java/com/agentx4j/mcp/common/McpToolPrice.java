package com.agentx4j.mcp.common;

import com.agentx4j.core.enums.BillingScheme;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP 工具定价信息。
 *
 * <p>存储从 @McpTool 注解解析出的定价配置。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpToolPrice {

    /** 工具名称 */
    private String toolName;

    /** 工具描述 */
    private String description;

    /** 价格（美元字符串，如 "$0.001"） */
    private String price;

    /** 网络标识 */
    private String network;

    /** 代币合约地址 */
    private String asset;

    /** 收款地址 */
    private String payTo;

    /** 计费方案 */
    private BillingScheme scheme;

    /** 超时时间（秒） */
    private long maxTimeoutSeconds;

    /** MCP transport 类型 */
    private String transport;

    /**
     * 将美元价格转换为原子单位。
     *
     * @param decimals 代币小数位数（USDC 为 6）
     * @return 原子单位金额字符串
     */
    public String getAtomicAmount(int decimals) {
        String priceStr = price.startsWith("$") ? price.substring(1) : price;
        java.math.BigDecimal dollars = new java.math.BigDecimal(priceStr);
        return dollars.multiply(java.math.BigDecimal.TEN.pow(decimals)).toBigInteger().toString();
    }
}
