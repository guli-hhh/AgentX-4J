package com.agentx4j.storage.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 钱包数据库实体。
 *
 * <p>对应 wallet 表。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletEntity {

    /** 主键 ID */
    private Long id;

    /** 钱包 ID (UUID) */
    private String walletId;

    /** 所属 Agent ID */
    private String agentId;

    /** 网络类型 */
    private String network;

    /** 钱包地址 */
    private String address;

    /** 当前余额 */
    private BigDecimal balance;

    /** 冻结余额 */
    private BigDecimal lockedBalance;

    /** 代币合约地址 */
    private String tokenAddress;

    /** 钱包标签 */
    private String label;

    /** 状态: ACTIVE / FROZEN */
    private String status;

    /** 创建时间 */
    private Instant createdAt;

    /** 更新时间 */
    private Instant updatedAt;
}
