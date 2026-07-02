package com.agentx4j.settlement;

import com.agentx4j.core.enums.BillingScheme;
import com.agentx4j.core.enums.TransactionStatus;
import com.agentx4j.core.model.TransactionRecord;
import com.agentx4j.x402.facilitator.FacilitatorClient;
import com.agentx4j.x402.facilitator.SettleResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * DefaultNettingService 测试。
 */
class DefaultNettingServiceTest {

    private FacilitatorClient facilitatorClient;
    private DefaultNettingService.TransactionQueryProvider queryProvider;
    private DefaultNettingService nettingService;

    @BeforeEach
    void setUp() {
        facilitatorClient = mock(FacilitatorClient.class);
        queryProvider = mock(DefaultNettingService.TransactionQueryProvider.class);
        nettingService = new DefaultNettingService(facilitatorClient, queryProvider);
    }

    @Test
    void testCalculateNettingWithNoTransactions() {
        when(queryProvider.findByDate(any())).thenReturn(Collections.emptyList());

        List<String> agentIds = Arrays.asList("agent-a", "agent-b");
        DefaultNettingService.NettingResult result = nettingService.calculateNetting(agentIds, LocalDate.now());

        assertNotNull(result);
        assertEquals(LocalDate.now(), result.getDate());
        assertEquals(0, result.getTxCount());
        assertEquals(0, result.getNettingCount());
        assertEquals(BigDecimal.ZERO, result.getTotalVolume());
        assertEquals(BigDecimal.ZERO, result.getNetVolume());
        assertTrue(result.getNetPositions().containsKey("agent-a"));
        assertTrue(result.getNetPositions().containsKey("agent-b"));
        assertEquals(BigDecimal.ZERO, result.getNetPositions().get("agent-a"));
    }

    @Test
    void testCalculateNettingWithSimpleTransactions() {
        List<TransactionRecord> transactions = Arrays.asList(
                createTx("tx-001", "agent-a", "agent-b", "1000", "100", "900"),
                createTx("tx-002", "agent-b", "agent-a", "300", "30", "270")
        );
        when(queryProvider.findByDate(any())).thenReturn(transactions);

        List<String> agentIds = Arrays.asList("agent-a", "agent-b");
        DefaultNettingService.NettingResult result = nettingService.calculateNetting(agentIds, LocalDate.now());

        assertNotNull(result);
        assertEquals(2, result.getTxCount());
        assertEquals(new BigDecimal("1300"), result.getTotalVolume());

        // agent-a: -1000 + 270 = -730 (应付)
        // agent-b: -300 + 900 = +600 (应收)
        BigDecimal positionA = result.getNetPositions().get("agent-a");
        BigDecimal positionB = result.getNetPositions().get("agent-b");

        assertNotNull(positionA);
        assertNotNull(positionB);
        assertTrue(positionA.compareTo(BigDecimal.ZERO) < 0);
        assertTrue(positionB.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void testCalculateNettingWithThreeAgents() {
        List<TransactionRecord> transactions = Arrays.asList(
                createTx("tx-001", "agent-a", "agent-b", "1000", "100", "900"),
                createTx("tx-002", "agent-b", "agent-c", "2000", "200", "1800"),
                createTx("tx-003", "agent-c", "agent-a", "500", "50", "450")
        );
        when(queryProvider.findByDate(any())).thenReturn(transactions);

        List<String> agentIds = Arrays.asList("agent-a", "agent-b", "agent-c");
        DefaultNettingService.NettingResult result = nettingService.calculateNetting(agentIds, LocalDate.now());

        assertNotNull(result);
        assertEquals(3, result.getTxCount());
        assertEquals(new BigDecimal("3500"), result.getTotalVolume());

        // 验证净头寸总和等于负的平台总费用（平台佣金从体系中流出）
        BigDecimal sum = result.getNetPositions().values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, sum.compareTo(new BigDecimal("-350")),
                "Net positions should sum to negative total fees, but got: " + sum);
    }

    @Test
    void testCalculateNettingGasSavings() {
        // 3 笔交易，净额后只需 2 笔 → 节省 1 笔 Gas
        List<TransactionRecord> transactions = Arrays.asList(
                createTx("tx-001", "agent-a", "agent-b", "1000", "100", "900"),
                createTx("tx-002", "agent-b", "agent-c", "2000", "200", "1800"),
                createTx("tx-003", "agent-c", "agent-a", "500", "50", "450")
        );
        when(queryProvider.findByDate(any())).thenReturn(transactions);

        List<String> agentIds = Arrays.asList("agent-a", "agent-b", "agent-c");
        DefaultNettingService.NettingResult result = nettingService.calculateNetting(agentIds, LocalDate.now());

        assertNotNull(result.getSavedGasEstimate());
        assertTrue(result.getSavedGasEstimate().compareTo(BigDecimal.ZERO) >= 0);
    }

    @Test
    void testExecuteNetting() {
        when(facilitatorClient.settle(any(), any()))
                .thenReturn(SettleResponse.builder().success(true).txHash("0xabc").build());

        DefaultNettingService.NettingResult netting = DefaultNettingService.NettingResult.builder()
                .date(LocalDate.now())
                .netPositions(java.util.Map.of(
                        "agent-a", new BigDecimal("730"),
                        "agent-b", new BigDecimal("-600"),
                        "agent-c", new BigDecimal("-130")
                ))
                .txCount(3)
                .nettingCount(1)
                .totalVolume(new BigDecimal("1300"))
                .netVolume(new BigDecimal("730"))
                .savedGasEstimate(new BigDecimal("0.0001"))
                .build();

        List<SettlementEngine.SettlementResult> results = nettingService.executeNetting(netting);

        assertNotNull(results);
        assertEquals(3, results.size());
    }

    @Test
    void testExecuteNettingWithZeroPositions() {
        when(facilitatorClient.settle(any(), any()))
                .thenReturn(SettleResponse.builder().success(true).txHash("0xabc").build());

        DefaultNettingService.NettingResult netting = DefaultNettingService.NettingResult.builder()
                .date(LocalDate.now())
                .netPositions(java.util.Map.of(
                        "agent-a", BigDecimal.ZERO,
                        "agent-b", BigDecimal.ZERO
                ))
                .txCount(1)
                .nettingCount(0)
                .totalVolume(BigDecimal.ZERO)
                .netVolume(BigDecimal.ZERO)
                .savedGasEstimate(BigDecimal.ZERO)
                .build();

        List<SettlementEngine.SettlementResult> results = nettingService.executeNetting(netting);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testFindNettableTransactions() {
        List<TransactionRecord> transactions = Arrays.asList(
                createTx("tx-001", "agent-a", "agent-b", "1000", "100", "900")
        );
        when(queryProvider.findByDate(any())).thenReturn(transactions);

        List<TransactionRecord> result = nettingService.findNettableTransactions(LocalDate.now());

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("tx-001", result.get(0).getTransactionId());
    }

    @Test
    void testFindNettableTransactionsWithNullProvider() {
        DefaultNettingService serviceWithNullProvider = new DefaultNettingService(facilitatorClient, null);

        List<TransactionRecord> result = serviceWithNullProvider.findNettableTransactions(LocalDate.now());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    private TransactionRecord createTx(String txId, String from, String to,
                                        String amount, String fee, String net) {
        return TransactionRecord.builder()
                .transactionId(txId)
                .fromAgentId(from)
                .toAgentId(to)
                .amount(new BigDecimal(amount))
                .platformFee(new BigDecimal(fee))
                .netAmount(new BigDecimal(net))
                .currency("USDC")
                .scheme(BillingScheme.EXACT)
                .status(TransactionStatus.SETTLED)
                .build();
    }
}
