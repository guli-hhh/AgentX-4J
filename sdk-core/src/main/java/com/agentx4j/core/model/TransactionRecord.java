package com.agentx4j.core.model;

import com.agentx4j.core.enums.BillingScheme;
import com.agentx4j.core.enums.NetworkType;
import com.agentx4j.core.enums.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * 交易记录 — 不可变的交易流水。
 *
 * <p>记录每一笔 Agent 间交易的完整信息。
 * 一旦创建，核心字段不可修改（只追加状态变更）。</p>
 *
 * <p>类比：TransactionRecord ≈ 银行流水中的"一条转账记录"</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRecord {

    /** 交易 ID (UUID) */
    private String transactionId;

    /** 幂等 Key (防止重复处理) */
    private String idempotencyKey;

    /** 付款方 Agent ID */
    private String fromAgentId;

    /** 收款方 Agent ID */
    private String toAgentId;

    /** 关联服务 ID（可选） */
    private String serviceId;

    /** 关联 MCP 工具名（如 "get_weather"） */
    private String toolName;

    /** 交易金额 */
    private BigDecimal amount;

    /** 平台佣金 */
    private BigDecimal platformFee;

    /** 实际到账 (amount - platformFee) */
    private BigDecimal netAmount;

    /** 货币（USDC） */
    private String currency;

    /** 网络类型 */
    private NetworkType network;

    /** 计费方式 */
    private BillingScheme scheme;

    /** 交易状态 */
    private TransactionStatus status;

    /** 链上交易哈希 */
    private String txHash;

    /** Facilitator 交易 ID */
    private String facilitatorTxId;

    /** 失败原因 */
    private String error;

    /** 扩展数据 */
    private Map<String, String> metadata;

    /** 创建时间 */
    private Instant createdAt;

    /** 结算时间 */
    private Instant settledAt;

    /** 完成时间 */
    private Instant completedAt;
}
