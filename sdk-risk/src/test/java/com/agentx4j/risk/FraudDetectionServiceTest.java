package com.agentx4j.risk;

import com.agentx4j.core.enums.RiskAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FraudDetectionService 测试。
 */
class FraudDetectionServiceTest {

    private FraudDetectionService fraudDetection;

    @BeforeEach
    void setUp() {
        fraudDetection = new FraudDetectionService();
    }

    @Test
    void testNormalTransaction() {
        RiskContext context = RiskContext.builder()
                .fromAgentId("agent-1")
                .amount(new BigDecimal("0.001"))
                .recentTxCount(1)
                .circularTrading(false)
                .fromAgentRegisteredAt(System.currentTimeMillis() / 1000 - 86400) // 注册 1 天前
                .fromAgentAvgAmount(new BigDecimal("0.001"))
                .build();

        RiskCheckResult result = fraudDetection.calculateScore(context);
        assertEquals(RiskAction.ALLOW, result.getAction());
        assertTrue(result.getRiskScore() < 50);
    }

    @Test
    void testHighVelocity() {
        RiskContext context = RiskContext.builder()
                .fromAgentId("agent-1")
                .amount(new BigDecimal("0.001"))
                .recentTxCount(100) // 高频交易
                .circularTrading(false)
                .build();

        RiskCheckResult result = fraudDetection.calculateScore(context);
        assertTrue(result.getRiskScore() > 0);
    }

    @Test
    void testCircularTrading() {
        RiskContext context = RiskContext.builder()
                .fromAgentId("agent-1")
                .amount(new BigDecimal("1.00"))
                .recentTxCount(1)
                .circularTrading(true) // 循环交易
                .build();

        RiskCheckResult result = fraudDetection.calculateScore(context);
        assertTrue(result.getTriggeredRules().contains("circular-trading"));
    }

    @Test
    void testNewAgentLargeTransaction() {
        RiskContext context = RiskContext.builder()
                .fromAgentId("agent-new")
                .amount(new BigDecimal("5.00")) // 新 Agent 大额交易
                .recentTxCount(1)
                .circularTrading(false)
                .fromAgentRegisteredAt(System.currentTimeMillis() / 1000 - 3600) // 注册 1 小时前
                .build();

        RiskCheckResult result = fraudDetection.calculateScore(context);
        assertTrue(result.getTriggeredRules().contains("new-agent-large-tx"));
    }

    @Test
    void testCheckAmountAnomaly() {
        assertTrue(fraudDetection.checkAmountAnomaly(
                new BigDecimal("100"), new BigDecimal("1"), 5.0));
        assertFalse(fraudDetection.checkAmountAnomaly(
                new BigDecimal("3"), new BigDecimal("1"), 5.0));
    }

    @Test
    void testIsNewAgent() {
        // 注册 1 小时前 → 是新 Agent
        assertTrue(fraudDetection.isNewAgent(System.currentTimeMillis() / 1000 - 3600));
        // 注册 3 天前 → 不是新 Agent
        assertFalse(fraudDetection.isNewAgent(System.currentTimeMillis() / 1000 - 259200));
        // null → 不是新 Agent
        assertFalse(fraudDetection.isNewAgent(null));
    }
}
