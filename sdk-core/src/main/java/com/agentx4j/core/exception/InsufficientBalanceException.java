package com.agentx4j.core.exception;

import lombok.Getter;

import java.math.BigDecimal;

/**
 * 余额不足异常。
 *
 * <p>当 Agent 钱包余额不足以支付时抛出。</p>
 */
@Getter
public class InsufficientBalanceException extends AgentX4JException {

    /** 当前余额 */
    private final BigDecimal currentBalance;

    /** 所需金额 */
    private final BigDecimal requiredAmount;

    /** 代币合约地址 */
    private final String tokenAddress;

    public InsufficientBalanceException(BigDecimal currentBalance, BigDecimal requiredAmount, String tokenAddress) {
        super("INSUFFICIENT_BALANCE",
                String.format("Insufficient balance: current=%s, required=%s", currentBalance, requiredAmount));
        this.currentBalance = currentBalance;
        this.requiredAmount = requiredAmount;
        this.tokenAddress = tokenAddress;
    }
}
