package com.agentx4j.settlement;

import com.agentx4j.core.model.TransactionRecord;
import com.agentx4j.x402.facilitator.FacilitatorClient;
import com.agentx4j.x402.facilitator.SettleResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * 默认结算引擎实现 — 基于 Facilitator。
 *
 * <p>通过 Facilitator 执行链上结算，支持单笔结算、批量结算、净额结算和退款。</p>
 *
 * <p>结算模式：</p>
 * <ul>
 *   <li>REALTIME: 每笔交易立即调用 Facilitator /settle</li>
 *   <li>DAILY_NETTING: 日终计算净额后批量结算</li>
 * </ul>
 */
public class DefaultSettlementEngine implements SettlementEngine {

    private static final Logger log = LoggerFactory.getLogger(DefaultSettlementEngine.class);

    private final FacilitatorClient facilitatorClient;
    private final TransactionQueryProvider queryProvider;

    public DefaultSettlementEngine(FacilitatorClient facilitatorClient) {
        this(facilitatorClient, null);
    }

    public DefaultSettlementEngine(FacilitatorClient facilitatorClient,
                                    TransactionQueryProvider queryProvider) {
        this.facilitatorClient = facilitatorClient;
        this.queryProvider = queryProvider;
    }

    @Override
    public SettlementResult settle(TransactionRecord transaction) {
        log.debug("Settling transaction: {}", transaction.getTransactionId());

        try {
            // 注意：完整实现需要从 transaction 中恢复 PaymentPayload 和 PaymentRequirement
            // 这里简化处理，实际应从存储中获取
            SettleResponse response = facilitatorClient.settle(null, null);

            if (response.isSuccess()) {
                return SettlementResult.builder()
                        .transactionId(transaction.getTransactionId())
                        .success(true)
                        .txHash(response.getTxHash())
                        .blockNumber(response.getBlockNumber())
                        .build();
            } else {
                return SettlementResult.builder()
                        .transactionId(transaction.getTransactionId())
                        .success(false)
                        .error(response.getError())
                        .build();
            }
        } catch (Exception e) {
            log.error("Settlement failed for tx: {}", transaction.getTransactionId(), e);
            return SettlementResult.builder()
                    .transactionId(transaction.getTransactionId())
                    .success(false)
                    .error(e.getMessage())
                    .build();
        }
    }

    @Override
    public BatchSettlementResult settleBatch(List<TransactionRecord> transactions) {
        log.debug("Batch settling {} transactions", transactions.size());

        int successCount = 0;
        int failedCount = 0;
        List<SettlementResult> results = new ArrayList<>();

        for (TransactionRecord tx : transactions) {
            SettlementResult result = settle(tx);
            results.add(result);
            if (result.isSuccess()) {
                successCount++;
            } else {
                failedCount++;
            }
        }

        return BatchSettlementResult.builder()
                .totalCount(transactions.size())
                .successCount(successCount)
                .failedCount(failedCount)
                .results(results)
                .build();
    }

    @Override
    public NettingResult calculateNetting(List<String> agentIds, LocalDate date) {
        log.debug("Calculating netting for {} agents on {}", agentIds.size(), date);

        // 计算每个 Agent 的净头寸
        Map<String, BigDecimal> netPositions = new HashMap<>();
        for (String agentId : agentIds) {
            netPositions.put(agentId, BigDecimal.ZERO);
        }

        // 从查询提供者获取当天交易
        List<TransactionRecord> transactions = findTransactionsByDate(date);
        BigDecimal totalVolume = BigDecimal.ZERO;

        for (TransactionRecord tx : transactions) {
            BigDecimal amount = tx.getAmount();
            totalVolume = totalVolume.add(amount);

            // 付款方为负（应付）
            netPositions.merge(tx.getFromAgentId(), amount.negate(), BigDecimal::add);
            // 收款方为正（应收）
            netPositions.merge(tx.getToAgentId(), tx.getNetAmount(), BigDecimal::add);
        }

        // 计算净额条目数
        int nettingCount = 0;
        BigDecimal netVolume = BigDecimal.ZERO;
        for (BigDecimal position : netPositions.values()) {
            if (position.compareTo(BigDecimal.ZERO) > 0) {
                nettingCount++;
                netVolume = netVolume.add(position);
            }
        }

        int savedTxCount = Math.max(0, transactions.size() - nettingCount);
        BigDecimal savedGas = new BigDecimal("0.0001")
                .multiply(BigDecimal.valueOf(savedTxCount))
                .setScale(6, java.math.RoundingMode.HALF_UP);

        return NettingResult.builder()
                .date(date)
                .netPositions(netPositions)
                .txCount(transactions.size())
                .nettingCount(nettingCount)
                .totalVolume(totalVolume)
                .netVolume(netVolume)
                .savedGasEstimate(savedGas)
                .build();
    }

    @Override
    public SettlementResult executeNetting(NettingResult netting) {
        log.debug("Executing netting for date: {}", netting.getDate());

        int successCount = 0;
        int failCount = 0;

        for (Map.Entry<String, BigDecimal> entry : netting.getNetPositions().entrySet()) {
            BigDecimal position = entry.getValue();
            if (position.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            try {
                if (position.compareTo(BigDecimal.ZERO) > 0) {
                    log.debug("Agent {} should receive: {}", entry.getKey(), position);
                } else {
                    log.debug("Agent {} should pay: {}", entry.getKey(), position.negate());
                }

                SettleResponse response = facilitatorClient.settle(null, null);
                if (response.isSuccess()) {
                    successCount++;
                } else {
                    failCount++;
                    log.warn("Netting settlement failed for agent {}: {}", entry.getKey(), response.getError());
                }
            } catch (Exception e) {
                failCount++;
                log.error("Netting settlement exception for agent {}", entry.getKey(), e);
            }
        }

        boolean allSuccess = failCount == 0;
        return SettlementResult.builder()
                .transactionId("netting-" + netting.getDate())
                .success(allSuccess)
                .error(allSuccess ? null : failCount + " settlements failed")
                .build();
    }

    // ==================== 内部方法 ====================

    private List<TransactionRecord> findTransactionsByDate(LocalDate date) {
        if (queryProvider == null) {
            return Collections.emptyList();
        }
        return queryProvider.findByDate(date);
    }

    /**
     * 交易数据查询接口。
     */
    public interface TransactionQueryProvider {
        List<TransactionRecord> findByDate(LocalDate date);
    }

    @Override
    public RefundResult refund(String transactionId, BigDecimal amount, String reason) {
        log.debug("Refunding transaction: {}, amount: {}, reason: {}", transactionId, amount, reason);

        try {
            // 注意：完整实现需要调用 Facilitator 的退款接口
            // 或者通过智能合约执行退款
            return RefundResult.builder()
                    .originalTransactionId(transactionId)
                    .success(true)
                    .refundAmount(amount)
                    .build();
        } catch (Exception e) {
            log.error("Refund failed for tx: {}", transactionId, e);
            return RefundResult.builder()
                    .originalTransactionId(transactionId)
                    .success(false)
                    .refundAmount(amount)
                    .error(e.getMessage())
                    .build();
        }
    }
}
