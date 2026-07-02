package com.agentx4j.x402.network;

import com.agentx4j.core.model.PaymentPayload;
import com.agentx4j.core.model.PaymentRequirement;

import java.math.BigInteger;

/**
 * 网络适配器 — 抽象不同区块链网络的交互。
 *
 * <p>设计为可插拔架构：</p>
 * <ul>
 *   <li>新增一条链 → 实现 NetworkAdapter 接口即可</li>
 *   <li>不影响其他链的逻辑</li>
 * </ul>
 *
 * <p>类比：NetworkAdapter ≈ "翻译官"
 *       不同的区块链说不同的语言，翻译官负责统一翻译。</p>
 */
public interface NetworkAdapter {

    /** CAIP-2 网络前缀（如 "eip155", "solana"） */
    String getNetworkPrefix();

    /**
     * 验证支付签名。
     *
     * @return true=签名有效, false=签名无效
     */
    boolean verifySignature(PaymentPayload payload, PaymentRequirement requirement);

    /**
     * 提交链上交易。
     *
     * @return 交易哈希 (txHash)
     */
    String submitTransaction(PaymentPayload payload, PaymentRequirement requirement);

    /**
     * 查询交易确认状态。
     */
    TransactionConfirmation getConfirmation(String txHash);

    /**
     * 查询代币余额。
     *
     * @param walletAddress 钱包地址
     * @param tokenAddress  代币合约地址
     * @return 余额 (原子单位)
     */
    BigInteger getTokenBalance(String walletAddress, String tokenAddress);

    /** 获取当前区块号 */
    long getBlockNumber();

    // ==================== 内部数据结构 ====================

    /**
     * 交易确认信息。
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    class TransactionConfirmation {
        private String txHash;
        private long blockNumber;
        private int confirmations;
        private boolean confirmed;
        private String status; // SUCCESS / FAILED
    }
}
