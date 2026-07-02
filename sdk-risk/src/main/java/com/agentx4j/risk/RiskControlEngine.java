package com.agentx4j.risk;

import com.agentx4j.core.enums.RiskAction;
import com.agentx4j.core.model.TransactionRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 风控引擎 — 实时风险控制。
 *
 * <p>在支付流程的关键节点进行风险检查：</p>
 * <ol>
 *   <li>支付前检查 (prePaymentCheck) — 决定是否允许支付</li>
 *   <li>支付后监控 (postPaymentMonitor) — 发现异常行为</li>
 * </ol>
 *
 * <p>风险等级：</p>
 * <ul>
 *   <li>LOW (0-30): 正常，放行</li>
 *   <li>MEDIUM (31-60): 关注，记录日志</li>
 *   <li>HIGH (61-80): 审核，可能需要人工介入</li>
 *   <li>CRITICAL (81-100): 阻断，拒绝交易</li>
 * </ul>
 *
 * <p>决策结果：</p>
 * <ul>
 *   <li>ALLOW: 允许支付</li>
 *   <li>REVIEW: 需要人工审核</li>
 *   <li>BLOCK: 拒绝支付</li>
 * </ul>
 */
public class RiskControlEngine {

    private static final Logger log = LoggerFactory.getLogger(RiskControlEngine.class);

    private final List<RiskRule> rules;
    private final FraudDetectionService fraudDetection;
    private final RateLimitService rateLimit;

    public RiskControlEngine(FraudDetectionService fraudDetection, RateLimitService rateLimit) {
        this.fraudDetection = fraudDetection;
        this.rateLimit = rateLimit;
        this.rules = new ArrayList<>();
        initDefaultRules();
    }

    /**
     * 支付前检查。
     * 在支付发起前执行，决定是否允许支付。
     *
     * @param context 风控上下文
     * @return 检查结果
     */
    public RiskCheckResult prePaymentCheck(RiskContext context) {
        // 1. 限流检查
        if (!rateLimit.tryAcquire(context.getFromAgentId())) {
            return RiskCheckResult.builder()
                    .action(RiskAction.BLOCK)
                    .riskScore(100)
                    .riskLevel(com.agentx4j.core.enums.RiskLevel.CRITICAL)
                    .triggeredRules(List.of("rate-limit-exceeded"))
                    .reason("Rate limit exceeded for agent: " + context.getFromAgentId())
                    .build();
        }

        // 2. 规则引擎评估
        RiskCheckResult ruleResult = evaluateRules(context);
        if (ruleResult.isBlocked()) {
            return ruleResult;
        }

        // 3. 欺诈检测评分
        RiskCheckResult fraudResult = fraudDetection.calculateScore(context);

        // 4. 综合决策（取更严格的那个）
        return mergeResults(ruleResult, fraudResult);
    }

    /**
     * 支付后监控。
     * 在支付完成后执行，发现异常行为模式。
     */
    public void postPaymentMonitor(TransactionRecord transaction) {
        // 更新 Agent 行为画像（可扩展为写入数据库）
        log.debug("Post payment monitoring: tx={}, agent={}, amount={}",
                transaction.getTransactionId(), transaction.getFromAgentId(), transaction.getAmount());
    }

    /**
     * 添加风控规则。
     */
    public void addRule(RiskRule rule) {
        rules.add(rule);
        rules.sort((a, b) -> Integer.compare(a.getPriority(), b.getPriority()));
    }

    /**
     * 移除风控规则。
     */
    public void removeRule(String ruleId) {
        rules.removeIf(r -> r.getRuleId().equals(ruleId));
    }

    // ==================== 内部方法 ====================

    private void initDefaultRules() {
        // 默认规则：单笔金额 > $100 → BLOCK
        rules.add(RiskRule.builder()
                .ruleId("max-amount")
                .name("单笔金额限制")
                .description("单笔交易金额不能超过 $100")
                .condition("amount > 100")
                .action(RiskAction.BLOCK)
                .priority(1)
                .enabled(true)
                .build());

        // 默认规则：循环交易 → BLOCK
        rules.add(RiskRule.builder()
                .ruleId("circular-trading")
                .name("循环交易检测")
                .description("检测到 A→B→A 循环交易模式")
                .condition("circularTrading == true")
                .action(RiskAction.BLOCK)
                .priority(2)
                .enabled(true)
                .build());
    }

    private RiskCheckResult evaluateRules(RiskContext context) {
        for (RiskRule rule : rules) {
            if (!rule.isEnabled()) continue;

            if (evaluateCondition(rule.getCondition(), context)) {
                log.debug("Risk rule triggered: {} for agent: {}", rule.getName(), context.getFromAgentId());
                return RiskCheckResult.builder()
                        .action(rule.getAction())
                        .riskScore(rule.getAction() == RiskAction.BLOCK ? 100 : 60)
                        .riskLevel(rule.getAction() == RiskAction.BLOCK
                                ? com.agentx4j.core.enums.RiskLevel.CRITICAL
                                : com.agentx4j.core.enums.RiskLevel.HIGH)
                        .triggeredRules(List.of(rule.getRuleId()))
                        .reason("Rule triggered: " + rule.getName())
                        .build();
            }
        }

        return RiskCheckResult.builder()
                .action(RiskAction.ALLOW)
                .riskScore(0)
                .riskLevel(com.agentx4j.core.enums.RiskLevel.LOW)
                .triggeredRules(List.of())
                .build();
    }

    /**
     * 简化版条件评估（不使用 SpEL，直接解析简单表达式）。
     */
    private boolean evaluateCondition(String condition, RiskContext context) {
        if (condition == null || condition.isEmpty()) return false;

        try {
            // 支持格式: "amount > 100", "circularTrading == true" 等
            String[] parts = condition.trim().split("\\s+");
            if (parts.length != 3) return false;

            String field = parts[0];
            String operator = parts[1];
            String value = parts[2];

            switch (field) {
                case "amount":
                    BigDecimal amount = context.getAmount();
                    BigDecimal threshold = new BigDecimal(value);
                    return compare(amount, operator, threshold);
                case "recentTxCount":
                    int count = context.getRecentTxCount();
                    int countThreshold = Integer.parseInt(value);
                    return compare(count, operator, countThreshold);
                case "circularTrading":
                    boolean circular = context.isCircularTrading();
                    return operator.equals("==") && circular == Boolean.parseBoolean(value);
                default:
                    return false;
            }
        } catch (Exception e) {
            log.warn("Failed to evaluate condition: {}", condition, e);
            return false;
        }
    }

    private boolean compare(BigDecimal left, String op, BigDecimal right) {
        int cmp = left.compareTo(right);
        return switch (op) {
            case ">" -> cmp > 0;
            case ">=" -> cmp >= 0;
            case "<" -> cmp < 0;
            case "<=" -> cmp <= 0;
            case "==" -> cmp == 0;
            case "!=" -> cmp != 0;
            default -> false;
        };
    }

    private boolean compare(int left, String op, int right) {
        return switch (op) {
            case ">" -> left > right;
            case ">=" -> left >= right;
            case "<" -> left < right;
            case "<=" -> left <= right;
            case "==" -> left == right;
            case "!=" -> left != right;
            default -> false;
        };
    }

    private RiskCheckResult mergeResults(RiskCheckResult ruleResult, RiskCheckResult fraudResult) {
        // 如果任一结果为 BLOCK，最终为 BLOCK
        if (ruleResult.isBlocked() || fraudResult.isBlocked()) {
            List<String> allRules = new ArrayList<>();
            allRules.addAll(ruleResult.getTriggeredRules());
            allRules.addAll(fraudResult.getTriggeredRules());
            return RiskCheckResult.builder()
                    .action(RiskAction.BLOCK)
                    .riskScore(Math.max(ruleResult.getRiskScore(), fraudResult.getRiskScore()))
                    .riskLevel(com.agentx4j.core.enums.RiskLevel.CRITICAL)
                    .triggeredRules(allRules)
                    .reason("Blocked: " + ruleResult.getReason() + "; " + fraudResult.getReason())
                    .build();
        }

        // 如果任一结果为 REVIEW，最终为 REVIEW
        if (ruleResult.needsReview() || fraudResult.needsReview()) {
            List<String> allRules = new ArrayList<>();
            allRules.addAll(ruleResult.getTriggeredRules());
            allRules.addAll(fraudResult.getTriggeredRules());
            return RiskCheckResult.builder()
                    .action(RiskAction.REVIEW)
                    .riskScore(Math.max(ruleResult.getRiskScore(), fraudResult.getRiskScore()))
                    .riskLevel(com.agentx4j.core.enums.RiskLevel.HIGH)
                    .triggeredRules(allRules)
                    .reason("Review required: " + ruleResult.getReason() + "; " + fraudResult.getReason())
                    .build();
        }

        return fraudResult;
    }
}
