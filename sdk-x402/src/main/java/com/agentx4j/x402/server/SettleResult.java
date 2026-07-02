package com.agentx4j.x402.server;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 结算结果。
 *
 * <p>Resource Server 执行结算后的返回结果。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettleResult {

    /** 结算是否成功 */
    private boolean success;

    /** 链上交易哈希 */
    private String txHash;

    /** 区块号 */
    private Long blockNumber;

    /** 失败原因 */
    private String error;

    public static SettleResult success(String txHash, Long blockNumber) {
        return SettleResult.builder()
                .success(true)
                .txHash(txHash)
                .blockNumber(blockNumber)
                .build();
    }

    public static SettleResult failed(String error) {
        return SettleResult.builder()
                .success(false)
                .error(error)
                .build();
    }
}
