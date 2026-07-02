package com.agentx4j.storage.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Agent 数据库实体。
 *
 * <p>对应 agent 表。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentEntity {

    /** 主键 ID */
    private Long id;

    /** Agent 唯一标识 (UUID) */
    private String agentId;

    /** Agent 名称 */
    private String name;

    /** Agent 描述 */
    private String description;

    /** 去中心化身份 (DID) */
    private String did;

    /** 主钱包地址 */
    private String walletAddress;

    /** 主网络类型 */
    private String network;

    /** 角色: BUYER / SELLER / BOTH */
    private String role;

    /** 状态: ACTIVE / SUSPENDED / BANNED */
    private String status;

    /** 信誉评分 (0-100) */
    private Double reputationScore;

    /** 信用额度 */
    private String creditLimit;

    /** 总交易数 */
    private Integer totalTransactions;

    /** 创建时间 */
    private Instant createdAt;

    /** 更新时间 */
    private Instant updatedAt;
}
