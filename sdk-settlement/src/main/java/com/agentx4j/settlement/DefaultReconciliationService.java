package com.agentx4j.settlement;

import com.agentx4j.core.model.TransactionRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * 默认对账服务实现。
 *
 * <p>对账流程：</p>
 * <ol>
 *   <li>汇总当日所有交易</li>
 *   <li>按 Agent 分组计算应收/应付</li>
 *   <li>与链上实际余额比对</li>
 *   <li>发现差异 → 告警 + 人工处理</li>
 *   <li>生成对账单</li>
 * </ol>
 *
 * <p>类比：ReconciliationService ≈ "会计对账"
 *       确保"账上的钱"和"实际的钱"一致。</p>
 */
public class DefaultReconciliationService implements ReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(DefaultReconciliationService.class);

    private final TransactionQueryProvider queryProvider;

    public DefaultReconciliationService() {
        this.queryProvider = null;
    }

    public DefaultReconciliationService(TransactionQueryProvider queryProvider) {
        this.queryProvider = queryProvider;
    }

    @Override
    public ReconciliationReport generateDailyReport(LocalDate date) {
        log.info("Generating reconciliation report for: {}", date);

        List<TransactionRecord> transactions = queryProvider != null
                ? queryProvider.findByDate(date)
                : Collections.emptyList();

        // 按 Agent 分组统计
        Map<String, AgentStats> statsMap = new HashMap<>();
        BigDecimal totalVolume = BigDecimal.ZERO;
        BigDecimal totalFees = BigDecimal.ZERO;

        for (TransactionRecord tx : transactions) {
            totalVolume = totalVolume.add(tx.getAmount());
            totalFees = totalFees.add(tx.getPlatformFee());

            // 付款方支出
            statsMap.computeIfAbsent(tx.getFromAgentId(), k -> new AgentStats())
                    .addExpense(tx.getAmount());

            // 收款方收入
            statsMap.computeIfAbsent(tx.getToAgentId(), k -> new AgentStats())
                    .addIncome(tx.getNetAmount());
        }

        // 生成 Agent 对账单
        List<AgentStatement> statements = new ArrayList<>();
        for (Map.Entry<String, AgentStats> entry : statsMap.entrySet()) {
            AgentStats stats = entry.getValue();
            statements.add(AgentStatement.builder()
                    .agentId(entry.getKey())
                    .date(date)
                    .totalIncome(stats.income)
                    .totalExpense(stats.expense)
                    .commission(stats.expense.multiply(new BigDecimal("0.1"))) // 简化计算
                    .netAmount(stats.income.subtract(stats.expense))
                    .txCount(stats.txCount)
                    .build());
        }

        return ReconciliationReport.builder()
                .date(date)
                .totalTransactions(transactions.size())
                .totalVolume(totalVolume)
                .totalFees(totalFees)
                .discrepancyCount(0)
                .agentStatements(statements)
                .build();
    }

    @Override
    public List<ReconciliationDiscrepancy> detectDiscrepancies(LocalDate date) {
        log.info("Detecting discrepancies for: {}", date);

        List<ReconciliationDiscrepancy> discrepancies = new ArrayList<>();

        if (queryProvider == null) {
            return discrepancies;
        }

        // 获取当日交易
        List<TransactionRecord> transactions = queryProvider.findByDate(date);

        // 按 Agent 分组计算应收/应付
        Map<String, BigDecimal> expectedBalances = new HashMap<>();
        for (TransactionRecord tx : transactions) {
            expectedBalances.merge(tx.getFromAgentId(), tx.getAmount().negate(), BigDecimal::add);
            expectedBalances.merge(tx.getToAgentId(), tx.getNetAmount(), BigDecimal::add);
        }

        // 与链上余额比对
        for (Map.Entry<String, BigDecimal> entry : expectedBalances.entrySet()) {
            BigDecimal expected = entry.getValue();
            BigDecimal actual = queryProvider.getOnChainBalance(entry.getKey());
            BigDecimal difference = actual.subtract(expected);

            if (difference.abs().compareTo(new BigDecimal("0.000001")) > 0) {
                discrepancies.add(ReconciliationDiscrepancy.builder()
                        .agentId(entry.getKey())
                        .expectedBalance(expected)
                        .actualBalance(actual)
                        .difference(difference)
                        .reason("Balance mismatch detected")
                        .build());

                log.warn("Discrepancy detected for agent {}: expected={}, actual={}, diff={}",
                        entry.getKey(), expected, actual, difference);
            }
        }

        return discrepancies;
    }

    @Override
    public void confirmReconciliation(String reconciliationId, String operatorId) {
        log.info("Reconciliation {} confirmed by {}", reconciliationId, operatorId);
        // 实际应更新数据库中对账记录的状态
    }

    @Override
    public AgentStatement getAgentStatement(String agentId, LocalDate from, LocalDate to) {
        log.info("Getting statement for agent {} from {} to {}", agentId, from, to);

        if (queryProvider == null) {
            return AgentStatement.builder()
                    .agentId(agentId)
                    .date(LocalDate.now())
                    .totalIncome(BigDecimal.ZERO)
                    .totalExpense(BigDecimal.ZERO)
                    .commission(BigDecimal.ZERO)
                    .netAmount(BigDecimal.ZERO)
                    .txCount(0)
                    .build();
        }

        List<TransactionRecord> transactions = queryProvider.findByAgentAndDateRange(agentId, from, to);

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        int txCount = 0;

        for (TransactionRecord tx : transactions) {
            if (tx.getToAgentId().equals(agentId)) {
                totalIncome = totalIncome.add(tx.getNetAmount());
                txCount++;
            }
            if (tx.getFromAgentId().equals(agentId)) {
                totalExpense = totalExpense.add(tx.getAmount());
                txCount++;
            }
        }

        return AgentStatement.builder()
                .agentId(agentId)
                .date(to)
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .commission(totalExpense.multiply(new BigDecimal("0.1")))
                .netAmount(totalIncome.subtract(totalExpense))
                .txCount(txCount)
                .build();
    }

    // ==================== 查询接口 ====================

    /**
     * 交易数据查询接口。
     *
     * <p>解耦结算引擎与具体的数据存储实现。
     * 业务方需要实现此接口以连接自己的数据库。</p>
     */
    public interface TransactionQueryProvider {
        /** 查询指定日期的所有交易 */
        List<TransactionRecord> findByDate(LocalDate date);

        /** 查询 Agent 在指定日期范围内的交易 */
        List<TransactionRecord> findByAgentAndDateRange(String agentId, LocalDate from, LocalDate to);

        /** 查询 Agent 的链上余额 */
        BigDecimal getOnChainBalance(String agentId);
    }

    // ==================== 内部数据结构 ====================

    private static class AgentStats {
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        int txCount = 0;

        void addIncome(BigDecimal amount) {
            this.income = this.income.add(amount);
            this.txCount++;
        }

        void addExpense(BigDecimal amount) {
            this.expense = this.expense.add(amount);
            this.txCount++;
        }
    }
}
