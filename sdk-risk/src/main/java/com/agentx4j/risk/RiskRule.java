package com.agentx4j.risk;

import com.agentx4j.core.enums.RiskAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 风控规则。
 *
 * <p>使用 SpEL 表达式定义条件，满足条件时执行对应动作。</p>
 *
 * <p>示例规则：</p>
 * <ul>
 *   <li>单笔金额 > $10 → BLOCK</li>
 *   <li>每分钟交易 > 60 次 → REVIEW</li>
 *   <li>新 Agent 首笔交易 > $1 → BLOCK</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskRule {

    /** 规则 ID */
    private String ruleId;

    /** 规则名称 */
    private String name;

    /** 规则描述 */
    private String description;

    /** 条件表达式（简化版，支持 key operator value 格式） */
    private String condition;

    /** 命中规则时的动作 */
    private RiskAction action;

    /** 优先级（数字越小优先级越高） */
    private int priority;

    /** 是否启用 */
    private boolean enabled;
}
