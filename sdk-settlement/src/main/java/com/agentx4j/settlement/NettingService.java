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
 * 净额结算服务 — 计算 Agent 间的净头寸，减少链上交易次数。
 *
 * <p>净额结算原理：</p>
 * <pre>
 * 示例:
 *   A→B: $10, B→A: $3, A→C: $5
 *   净额: A→B: $7, A→C: $5 (只需 2 笔而非 3 笔链上交易)
 * </pre>
 *
 * <p>类比：NettingService ≈ "银行间轧差清算"
 *       多边债务相互抵消，只结算净差额。</p>
 */
public interface NettingService {

    /**
     * 计算 Agent 间的净头寸。
     *
     * @param agentIds 参与净额结算的 Agent ID 列表
     * @param date     结算日期
     * @return 净额结算结果
     */
    NettingResult calculateNetting(List<String> agentIds, LocalDate date);

    /**
     * 执行净额结算。
     *
     * @param netting 净额结算结果
     * @return 结算结果列表
     */
    List<SettlementEngine.SettlementResult> executeNetting(NettingResult netting);

    /**
     * 查找可净额结算的交易。
     *
     * @param date 结算日期
     * @return 待结算交易列表
     */
    List<TransactionRecord> findNettableTransactions(LocalDate date);

    // ==================== 内部数据结构 ====================

    /**
     * 净额结算结果。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class NettingResult {
        private LocalDate date;
        private Map<String, BigDecimal> netPositions;
        private int txCount;
        private int nettingCount;
        private BigDecimal totalVolume;
        private BigDecimal netVolume;
        private BigDecimal savedGasEstimate;
    }

    /**
     * 净额条目 — 两个 Agent 之间的净结算。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class NettingEntry {
        private String fromAgentId;
        private String toAgentId;
        private BigDecimal amount;
        private int sourceTxCount;
    }
}
