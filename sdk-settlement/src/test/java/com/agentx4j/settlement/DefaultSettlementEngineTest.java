package com.agentx4j.settlement;

import com.agentx4j.core.model.TransactionRecord;
import com.agentx4j.x402.facilitator.FacilitatorClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * DefaultSettlementEngine 测试。
 */
class DefaultSettlementEngineTest {

    private FacilitatorClient facilitatorClient;
    private DefaultSettlementEngine engine;

    @BeforeEach
    void setUp() {
        facilitatorClient = mock(FacilitatorClient.class);
        engine = new DefaultSettlementEngine(facilitatorClient);
    }

    @Test
    void testCalculateNetting() {
        List<String> agentIds = Arrays.asList("agent-a", "agent-b", "agent-c");
        SettlementEngine.NettingResult result = engine.calculateNetting(agentIds, LocalDate.now());

        assertNotNull(result);
        assertEquals(LocalDate.now(), result.getDate());
        assertEquals(3, result.getNetPositions().size());
        assertTrue(result.getNetPositions().containsKey("agent-a"));
    }

    @Test
    void testExecuteNetting() {
        SettlementEngine.NettingResult netting = SettlementEngine.NettingResult.builder()
                .date(LocalDate.now())
                .netPositions(java.util.Map.of(
                        "agent-a", new BigDecimal("100"),
                        "agent-b", new BigDecimal("-50")
                ))
                .txCount(5)
                .build();

        SettlementEngine.SettlementResult result = engine.executeNetting(netting);
        assertNotNull(result);
    }

    @Test
    void testRefund() {
        SettlementEngine.RefundResult result = engine.refund("tx-001", new BigDecimal("0.001"), "Test refund");
        assertNotNull(result);
        assertEquals("tx-001", result.getOriginalTransactionId());
        assertEquals(new BigDecimal("0.001"), result.getRefundAmount());
    }
}
