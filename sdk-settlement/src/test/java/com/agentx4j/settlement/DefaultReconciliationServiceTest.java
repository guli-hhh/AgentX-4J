package com.agentx4j.settlement;

import com.agentx4j.core.model.TransactionRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DefaultReconciliationService 测试。
 */
class DefaultReconciliationServiceTest {

    private DefaultReconciliationService service;

    @BeforeEach
    void setUp() {
        service = new DefaultReconciliationService();
    }

    @Test
    void testGenerateDailyReportWithNoData() {
        DefaultReconciliationService.ReconciliationReport report =
                service.generateDailyReport(LocalDate.now());

        assertNotNull(report);
        assertEquals(LocalDate.now(), report.getDate());
        assertEquals(0, report.getTotalTransactions());
        assertEquals(BigDecimal.ZERO, report.getTotalVolume());
    }

    @Test
    void testGenerateDailyReportWithData() {
        List<TransactionRecord> transactions = Arrays.asList(
                createTx("tx-001", "agent-a", "agent-b", "1000", "100", "900"),
                createTx("tx-002", "agent-b", "agent-c", "2000", "200", "1800"),
                createTx("tx-003", "agent-c", "agent-a", "500", "50", "450")
        );

        DefaultReconciliationService withData = new DefaultReconciliationService(
                new DefaultReconciliationService.TransactionQueryProvider() {
                    @Override
                    public List<TransactionRecord> findByDate(LocalDate date) {
                        return transactions;
                    }

                    @Override
                    public List<TransactionRecord> findByAgentAndDateRange(String agentId, LocalDate from, LocalDate to) {
                        return Collections.emptyList();
                    }

                    @Override
                    public BigDecimal getOnChainBalance(String agentId) {
                        return BigDecimal.ZERO;
                    }
                });

        DefaultReconciliationService.ReconciliationReport report =
                withData.generateDailyReport(LocalDate.now());

        assertEquals(3, report.getTotalTransactions());
        assertEquals(new BigDecimal("3500"), report.getTotalVolume());
        assertEquals(new BigDecimal("350"), report.getTotalFees());
        assertFalse(report.getAgentStatements().isEmpty());
    }

    @Test
    void testDetectDiscrepancies() {
        List<ReconciliationService.ReconciliationDiscrepancy> discrepancies =
                service.detectDiscrepancies(LocalDate.now());
        assertNotNull(discrepancies);
        assertTrue(discrepancies.isEmpty());
    }

    @Test
    void testGetAgentStatement() {
        DefaultReconciliationService.AgentStatement statement =
                service.getAgentStatement("agent-a", LocalDate.now().minusDays(7), LocalDate.now());

        assertNotNull(statement);
        assertEquals("agent-a", statement.getAgentId());
        assertEquals(BigDecimal.ZERO, statement.getTotalIncome());
        assertEquals(BigDecimal.ZERO, statement.getTotalExpense());
    }

    @Test
    void testConfirmReconciliation() {
        // 不抛异常即可
        service.confirmReconciliation("recon-001", "operator-001");
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
                .scheme(com.agentx4j.core.enums.BillingScheme.EXACT)
                .status(com.agentx4j.core.enums.TransactionStatus.SETTLED)
                .build();
    }
}
