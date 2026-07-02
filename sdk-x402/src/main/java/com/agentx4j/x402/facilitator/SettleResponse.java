package com.agentx4j.x402.facilitator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 结算响应。
 *
 * <p>Facilitator 执行链上结算后的返回结果。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettleResponse {

    /** 结算是否成功 */
    private boolean success;

    /** 链上交易哈希 */
    private String txHash;

    /** 区块号 */
    private Long blockNumber;

    /** 结算失败原因（success=false 时） */
    private String error;

    /** 结算时间戳 */
    private long settledAt;
}
