package com.agentx4j.risk;

import com.agentx4j.core.enums.RiskLevel;
import com.agentx4j.core.enums.RiskAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * 欺诈检测服务。
 *
 * <p>基于行为分析和统计模型检测欺诈行为。</p>
 *
 * <p>检测维度：</p>
 * <ol>
 *   <li>速度异常 — 交易频率突然暴增</li>
 *   <li>金额异常 — 交易金额偏离历史均值</li>
 *   <li>循环交易 — A→B→A 的洗钱模式</li>
 *   <li>新 Agent — 新注册 Agent 的异常行为</li>
 * </ol>
 */
public class FraudDetectionService {

    private static final Logger log = LoggerFactory.getLogger(FraudDetectionService.class);

    /** 默认金额异常阈值（偏离均值的倍数） */
    private static final double DEFAULT_AMOUNT_ANOMALY_THRESHOLD = 5.0;

    /** 新 Agent 阈值（注册时间在此时间内视为新 Agent） */
    private static final Duration NEW_AGENT_THRESHOLD = Duration.ofHours(24);

    /** 新 Agent 首笔交易限额 */
    private static final BigDecimal NEW_AGENT_MAX_AMOUNT = new BigDecimal("1.00");

    /**
     * 计算综合风险评分 (0-100)。
     *
     * @param context 风控上下文
     * @return 风险评分
     */
    public RiskCheckResult calculateScore(RiskContext context) {
        int score = 0;
        List<String> triggeredRules = new ArrayList<>();

        // 1. 速度异常检测
        if (context.getRecentTxCount() > 60) {
            score += 30;
            triggeredRules.add("velocity-high");
        } else if (context.getRecentTxCount() > 30) {
            score += 15;
            triggeredRules.add("velocity-medium");
        }

        // 2. 金额异常检测
        if (context.getFromAgentAvgAmount() != null
                && context.getFromAgentAvgAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal ratio = context.getAmount()
                    .divide(context.getFromAgentAvgAmount(), 2, RoundingMode.HALF_UP);
            if (ratio.compareTo(BigDecimal.valueOf(DEFAULT_AMOUNT_ANOMALY_THRESHOLD)) > 0) {
                score += 25;
                triggeredRules.add("amount-anomaly");
            }
        }

        // 3. 循环交易检测
        if (context.isCircularTrading()) {
            score += 35;
            triggeredRules.add("circular-trading");
        }

        // 4. 新 Agent 检测
        if (isNewAgent(context.getFromAgentRegisteredAt())) {
            score += 10;
            triggeredRules.add("new-agent");
            if (context.getAmount().compareTo(NEW_AGENT_MAX_AMOUNT) > 0) {
                score += 20;
                triggeredRules.add("new-agent-large-tx");
            }
        }

        // 5. 大额交易检测
        if (context.getAmount().compareTo(new BigDecimal("10")) > 0) {
            score += 20;
            triggeredRules.add("large-tx");
        }

        // 限制最高 100
        score = Math.min(100, score);

        RiskLevel level = RiskLevel.fromScore(score);
        RiskAction action = determineAction(score);

        return RiskCheckResult.builder()
                .action(action)
                .riskLevel(level)
                .riskScore(score)
                .triggeredRules(triggeredRules)
                .reason(action != com.agentx4j.core.enums.RiskAction.ALLOW
                        ? "Risk score: " + score + ", rules: " + triggeredRules
                        : null)
                .build();
    }

    /**
     * 检查是否为速度异常。
     */
    public boolean checkVelocity(int recentTxCount, int threshold) {
        return recentTxCount > threshold;
    }

    /**
     * 检查是否为金额异常。
     */
    public boolean checkAmountAnomaly(BigDecimal amount, BigDecimal avgAmount, double threshold) {
        if (avgAmount == null || avgAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        BigDecimal ratio = amount.divide(avgAmount, 2, RoundingMode.HALF_UP);
        return ratio.compareTo(BigDecimal.valueOf(threshold)) > 0;
    }

    /**
     * 检查是否为新 Agent。
     */
    public boolean isNewAgent(Long registeredAt) {
        if (registeredAt == null) return false;
        Duration age = Duration.between(Instant.ofEpochSecond(registeredAt), Instant.now());
        return age.compareTo(NEW_AGENT_THRESHOLD) < 0;
    }

    // ==================== 内部方法 ====================

    private RiskAction determineAction(int score) {
        if (score >= 80) return com.agentx4j.core.enums.RiskAction.BLOCK;
        if (score >= 50) return com.agentx4j.core.enums.RiskAction.REVIEW;
        return com.agentx4j.core.enums.RiskAction.ALLOW;
    }
}
