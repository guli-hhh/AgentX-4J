package com.agentx4j.storage.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 交易记录数据库实体。
 *
 * <p>对应 transaction 表。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEntity {

    /** 主键 ID */
    private Long id;

    /** 交易 ID (UUID) */
    private String transactionId;

    /** 幂等 Key */
    private String idempotencyKey;

    /** 付款方 Agent ID */
    private String fromAgentId;

    /** 收款方 Agent ID */
    private String toAgentId;

    /** 关联服务 ID */
    private String serviceId;

    /** 关联 MCP 工具名 */
    private String toolName;

    /** 交易金额 */
    private BigDecimal amount;

    /** 平台佣金 */
    private BigDecimal platformFee;

    /** 实际到账 */
    private BigDecimal netAmount;

    /** 货币 */
    private String currency;

    /** 网络类型 */
    private String network;

    /** 计费方式: EXACT / UPTO / BATCH_SETTLEMENT */
    private String scheme;

    /** 交易状态: PENDING / SETTLED / COMPLETED / FAILED / REFUNDED / DISPUTED */
    private String status;

    /** 链上交易哈希 */
    private String txHash;

    /** Facilitator 交易 ID */
    private String facilitatorTxId;

    /** 失败原因 */
    private String errorMessage;

    /** 扩展元数据 (JSON) */
    private String metadata;

    /** 创建时间 */
    private Instant createdAt;

    /** 结算时间 */
    private Instant settledAt;

    /** 完成时间 */
    private Instant completedAt;
}
