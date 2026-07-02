package com.agentx4j.core.model;

import com.agentx4j.core.enums.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 结算结果。
 *
 * <p>结算引擎执行后的返回结果，包含链上交易信息。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementResult {

    /** 交易 ID */
    private String transactionId;

    /** 结算后的交易状态 */
    private TransactionStatus status;

    /** 链上交易哈希 */
    private String txHash;

    /** 区块号 */
    private Long blockNumber;

    /** 实际 Gas 消耗 */
    private String gasUsed;

    /** 失败原因（如果失败） */
    private String error;

    /** 是否成功 */
    public boolean isSuccess() {
        return status == TransactionStatus.SETTLED || status == TransactionStatus.COMPLETED;
    }
}
