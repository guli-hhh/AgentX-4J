package com.agentx4j.risk;

import com.agentx4j.core.enums.RiskAction;
import com.agentx4j.core.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 风控检查结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskCheckResult {

    /** 最终决策 */
    private RiskAction action;

    /** 风险等级 */
    private RiskLevel riskLevel;

    /** 风险评分 (0-100) */
    private int riskScore;

    /** 命中的规则列表 */
    private List<String> triggeredRules;

    /** 拒绝原因（action = BLOCK 时） */
    private String reason;

    /** 是否允许支付 */
    public boolean isAllowed() {
        return action == RiskAction.ALLOW;
    }

    /** 是否需要人工审核 */
    public boolean needsReview() {
        return action == RiskAction.REVIEW;
    }

    /** 是否被阻断 */
    public boolean isBlocked() {
        return action == RiskAction.BLOCK;
    }
}
