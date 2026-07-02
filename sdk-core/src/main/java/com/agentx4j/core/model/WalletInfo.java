package com.agentx4j.core.model;

import com.agentx4j.core.enums.NetworkType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 钱包信息。
 *
 * <p>Agent 在某个区块链网络上的钱包信息。
 * 一个 Agent 可以拥有多个不同网络的钱包。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletInfo {

    /** 钱包 ID */
    private String walletId;

    /** 所属 Agent ID */
    private String agentId;

    /** 网络类型 */
    private NetworkType network;

    /** 钱包地址 */
    private String address;

    /** 当前余额（缓存值） */
    private BigDecimal balance;

    /** 代币合约地址（可选，原生代币为 null） */
    private String tokenAddress;

    /** 钱包标签（如 "主钱包"、"收款钱包"） */
    private String label;
}
