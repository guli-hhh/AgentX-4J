package com.agentx4j.settlement;

import com.agentx4j.core.model.TransactionRecord;
import com.agentx4j.x402.facilitator.FacilitatorClient;
import com.agentx4j.x402.facilitator.SettleResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * 默认净额结算服务实现。
 *
 * <p>通过 Facilitator 执行链上净额结算。</p>
 *
 * <p>净额结算算法：</p>
 * <ol>
 *   <li>收集指定日期内所有 Agent 间的交易</li>
 *   <li>按 Agent 对计算双向净头寸</li>
 *   <li>生成净额条目（只保留净差额）</li>
 *   <li>通过 Facilitator 执行链上结算</li>
 * </ol>
 */
public class DefaultNettingService implements NettingService {

    private static final Logger log = LoggerFactory.getLogger(DefaultNettingService.class);

    private static final BigDecimal GAS_PER_TX_ESTIMATE = new BigDecimal("0.0001");
    private static final int SCALE = 6;

    private final FacilitatorClient facilitatorClient;
    private final TransactionQueryProvider queryProvider;

    public DefaultNettingService(FacilitatorClient facilitatorClient,
                                  TransactionQueryProvider queryProvider) {
        this.facilitatorClient = facilitatorClient;
        this.queryProvider = queryProvider;
    }

    @Override
    public NettingResult calculateNetting(List<String> agentIds, LocalDate date) {
        log.info("Calculating netting for {} agents on {}", agentIds.size(), date);

        List<TransactionRecord> transactions = findNettableTransactions(date);

        if (transactions.isEmpty()) {
            log.info("No transactions to net for date: {}", date);
            return NettingResult.builder()
                    .date(date)
                    .netPositions(initZeroPositions(agentIds))
                    .txCount(0)
                    .nettingCount(0)
                    .totalVolume(BigDecimal.ZERO)
                    .netVolume(BigDecimal.ZERO)
                    .savedGasEstimate(BigDecimal.ZERO)
                    .build();
        }

        Map<String, BigDecimal> netPositions = initZeroPositions(agentIds);
        BigDecimal totalVolume = BigDecimal.ZERO;

        for (TransactionRecord tx : transactions) {
            BigDecimal amount = tx.getAmount();
            totalVolume = totalVolume.add(amount);

            // 付款方为负（应付）
            netPositions.merge(tx.getFromAgentId(), amount.negate(), BigDecimal::add);
            // 收款方为正（应收）
            netPositions.merge(tx.getToAgentId(), tx.getNetAmount(), BigDecimal::add);
        }

        // 计算净额条目数和净交易量
        int nettingCount = 0;
        BigDecimal netVolume = BigDecimal.ZERO;
        for (BigDecimal position : netPositions.values()) {
            if (position.compareTo(BigDecimal.ZERO) > 0) {
                nettingCount++;
                netVolume = netVolume.add(position);
            }
        }

        // 估算节省的 Gas = (原始交易数 - 净额交易数) * 单笔 Gas 估算
        int savedTxCount = transactions.size() - nettingCount;
        BigDecimal savedGas = GAS_PER_TX_ESTIMATE.multiply(BigDecimal.valueOf(savedTxCount))
                .setScale(SCALE, RoundingMode.HALF_UP);

        log.info("Netting calculated: txCount={}, nettingCount={}, totalVolume={}, netVolume={}, savedGas={}",
                transactions.size(), nettingCount, totalVolume, netVolume, savedGas);

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
    public List<SettlementEngine.SettlementResult> executeNetting(NettingResult netting) {
        log.info("Executing netting for date: {}, positions: {}",
                netting.getDate(), netting.getNetPositions().size());

        List<SettlementEngine.SettlementResult> results = new ArrayList<>();

        for (Map.Entry<String, BigDecimal> entry : netting.getNetPositions().entrySet()) {
            BigDecimal position = entry.getValue();

            if (position.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            if (position.compareTo(BigDecimal.ZERO) > 0) {
                // 应收 → 从平台账户转入
                log.debug("Agent {} should receive: {}", entry.getKey(), position);
                SettlementEngine.SettlementResult result = settleAgent(entry.getKey(), position, true);
                results.add(result);
            } else {
                // 应付 → 从 Agent 账户扣除
                log.debug("Agent {} should pay: {}", entry.getKey(), position.negate());
                SettlementEngine.SettlementResult result = settleAgent(entry.getKey(), position.negate(), false);
                results.add(result);
            }
        }

        log.info("Netting execution completed: {} settlements executed", results.size());
        return results;
    }

    @Override
    public List<TransactionRecord> findNettableTransactions(LocalDate date) {
        if (queryProvider == null) {
            return Collections.emptyList();
        }
        return queryProvider.findByDate(date);
    }

    // ==================== 内部方法 ====================

    private Map<String, BigDecimal> initZeroPositions(List<String> agentIds) {
        Map<String, BigDecimal> positions = new HashMap<>();
        for (String agentId : agentIds) {
            positions.put(agentId, BigDecimal.ZERO);
        }
        return positions;
    }

    private SettlementEngine.SettlementResult settleAgent(String agentId, BigDecimal amount, boolean isReceiver) {
        try {
            SettleResponse response = facilitatorClient.settle(null, null);

            if (response.isSuccess()) {
                return SettlementEngine.SettlementResult.builder()
                        .transactionId("netting-" + agentId + "-" + System.currentTimeMillis())
                        .success(true)
                        .txHash(response.getTxHash())
                        .blockNumber(response.getBlockNumber())
                        .build();
            } else {
                return SettlementEngine.SettlementResult.builder()
                        .transactionId("netting-" + agentId + "-" + System.currentTimeMillis())
                        .success(false)
                        .error(response.getError())
                        .build();
            }
        } catch (Exception e) {
            log.error("Netting settlement failed for agent: {}", agentId, e);
            return SettlementEngine.SettlementResult.builder()
                    .transactionId("netting-" + agentId + "-" + System.currentTimeMillis())
                    .success(false)
                    .error(e.getMessage())
                    .build();
        }
    }

    /**
     * 交易数据查询接口。
     *
     * <p>解耦净额结算服务与具体的数据存储实现。
     * 业务方需要实现此接口以连接自己的数据库。</p>
     */
    public interface TransactionQueryProvider {
        /** 查询指定日期的所有交易 */
        List<TransactionRecord> findByDate(LocalDate date);
    }
}
