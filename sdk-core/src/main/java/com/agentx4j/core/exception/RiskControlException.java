package com.agentx4j.core.exception;

import com.agentx4j.core.enums.RiskAction;
import com.agentx4j.core.enums.RiskLevel;
import lombok.Getter;

/**
 * 风控拦截异常。
 *
 * <p>当风控引擎判定交易风险过高时抛出。</p>
 */
@Getter
public class RiskControlException extends AgentX4JException {

    /** 风险等级 */
    private final RiskLevel riskLevel;

    /** 风控动作 */
    private final RiskAction action;

    /** 触发的原因 */
    private final String reason;

    public RiskControlException(RiskLevel riskLevel, RiskAction action, String reason) {
        super("RISK_CONTROL_BLOCKED",
                String.format("Transaction blocked by risk control: level=%s, reason=%s", riskLevel, reason));
        this.riskLevel = riskLevel;
        this.action = action;
        this.reason = reason;
    }
}
