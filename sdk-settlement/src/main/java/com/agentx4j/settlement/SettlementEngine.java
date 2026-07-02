package com.agentx4j.settlement;

import com.agentx4j.core.model.TransactionRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 结算引擎 — 处理交易结算、对账、净额结算。
 *
 * <p>核心能力：</p>
 * <ol>
 *   <li>单笔结算 — 一笔交易完成后立即结算</li>
 *   <li>批量结算 — 日终批量处理多笔交易</li>
 *   <li>净额结算 — 多笔交易相互抵消后只结算净差额</li>
 *   <li>退款 — 支持全额/部分退款</li>
 * </ol>
 *
 * <p>结算模式：</p>
 * <ul>
 *   <li>REALTIME: 每笔交易立即结算（适合低频高价值）</li>
 *   <li>DAILY_NETTING: 日终净额结算（适合高频微支付）</li>
 * </ul>
 */
public interface SettlementEngine {

    /** 执行单笔结算 */
    SettlementResult settle(TransactionRecord transaction);

    /** 批量结算（日终） */
    BatchSettlementResult settleBatch(List<TransactionRecord> transactions);

    /** 净额结算 — 计算 Agent 间的净头寸 */
    NettingResult calculateNetting(List<String> agentIds, LocalDate date);

    /** 执行净额结算 */
    SettlementResult executeNetting(NettingResult netting);

    /** 退款 */
    RefundResult refund(String transactionId, BigDecimal amount, String reason);

    // ==================== 内部数据结构 ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class SettlementResult {
        private String transactionId;
        private boolean success;
        private String txHash;
        private Long blockNumber;
        private String error;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class BatchSettlementResult {
        private int totalCount;
        private int successCount;
        private int failedCount;
        private List<SettlementResult> results;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class NettingResult {
        private LocalDate date;
        private Map<String, BigDecimal> netPositions; // Agent ID → 净头寸（正=应收，负=应付）
        private int txCount;
        private int nettingCount;
        private BigDecimal totalVolume;
        private BigDecimal netVolume;
        private BigDecimal savedGasEstimate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class RefundResult {
        private String originalTransactionId;
        private boolean success;
        private BigDecimal refundAmount;
        private String error;
    }
}
