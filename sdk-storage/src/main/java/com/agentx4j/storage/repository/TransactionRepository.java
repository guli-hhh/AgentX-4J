package com.agentx4j.storage.repository;

import com.agentx4j.storage.persistence.entity.TransactionEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 交易记录数据访问接口。
 */
public interface TransactionRepository {

    /** 保存交易记录 */
    TransactionEntity save(TransactionEntity entity);

    /** 根据 transactionId 查询 */
    Optional<TransactionEntity> findByTransactionId(String transactionId);

    /** 根据 idempotencyKey 查询 */
    Optional<TransactionEntity> findByIdempotencyKey(String idempotencyKey);

    /** 查询 Agent 的交易历史 */
    List<TransactionEntity> findByAgentId(String agentId, int page, int size);

    /** 查询 Agent 作为付款方的交易 */
    List<TransactionEntity> findByFromAgentId(String fromAgentId, int page, int size);

    /** 查询 Agent 作为收款方的交易 */
    List<TransactionEntity> findByToAgentId(String toAgentId, int page, int size);

    /** 查询时间范围内的交易 */
    List<TransactionEntity> findByTimeRange(Instant from, Instant to);

    /** 查询待结算交易 */
    List<TransactionEntity> findPendingTransactions();

    /** 更新交易状态 */
    void updateStatus(String transactionId, String status);

    /** 统计 Agent 交易总数 */
    long countByAgentId(String agentId);

    /** 统计 Agent 总收入 */
    java.math.BigDecimal sumIncomeByAgentId(String agentId);

    /** 统计 Agent 总支出 */
    java.math.BigDecimal sumExpenseByAgentId(String agentId);
}
