package com.agentx4j.risk;

import com.agentx4j.core.enums.AgentRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 风控上下文 — 风控检查时需要的所有信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskContext {

    /** 付款方 Agent ID */
    private String fromAgentId;

    /** 收款方 Agent ID */
    private String toAgentId;

    /** 交易金额（美元） */
    private BigDecimal amount;

    /** 付款方角色 */
    private AgentRole fromAgentRole;

    /** 付款方注册时间（Unix 时间戳，null 表示未知） */
    private Long fromAgentRegisteredAt;

    /** 付款方历史交易数 */
    private int fromAgentTxCount;

    /** 付款方历史平均交易金额 */
    private BigDecimal fromAgentAvgAmount;

    /** 付款方过去 N 分钟内的交易数 */
    private int recentTxCount;

    /** 是否为循环交易（A→B→A） */
    private boolean circularTrading;

    /** 网络类型 */
    private String network;
}
