package com.agentx4j.settlement;

import com.agentx4j.core.model.TransactionRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 对账服务 — 确保账目一致性。
 *
 * <p>对账流程：</p>
 * <ol>
 *   <li>汇总当日所有交易</li>
 *   <li>按 Agent 分组计算应收/应付</li>
 *   <li>与链上实际余额比对</li>
 *   <li>发现差异 → 告警 + 人工处理</li>
 *   <li>生成对账单</li>
 * </ol>
 */
public interface ReconciliationService {

    /** 生成日终对账单 */
    ReconciliationReport generateDailyReport(LocalDate date);

    /** 差异检测 */
    List<ReconciliationDiscrepancy> detectDiscrepancies(LocalDate date);

    /** 确认对账 */
    void confirmReconciliation(String reconciliationId, String operatorId);

    /** 获取 Agent 对账单 */
    AgentStatement getAgentStatement(String agentId, LocalDate from, LocalDate to);

    // ==================== 内部数据结构 ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class ReconciliationReport {
        private LocalDate date;
        private int totalTransactions;
        private BigDecimal totalVolume;
        private BigDecimal totalFees;
        private int discrepancyCount;
        private List<AgentStatement> agentStatements;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class ReconciliationDiscrepancy {
        private String agentId;
        private BigDecimal expectedBalance;
        private BigDecimal actualBalance;
        private BigDecimal difference;
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class AgentStatement {
        private String agentId;
        private LocalDate date;
        private BigDecimal totalIncome;
        private BigDecimal totalExpense;
        private BigDecimal commission;
        private BigDecimal netAmount;
        private int txCount;
    }
}
