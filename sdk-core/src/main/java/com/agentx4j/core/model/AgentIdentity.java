package com.agentx4j.core.model;

import com.agentx4j.core.enums.AgentRole;
import com.agentx4j.core.enums.NetworkType;
import com.agentx4j.core.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Agent 身份 — Agent 经济体中的唯一实体。
 *
 * <p>每个参与 Agent 经济的应用都需要注册一个 AgentIdentity。
 * 它包含了身份信息、钱包地址、角色定位和信誉评级。</p>
 *
 * <p>类比：AgentIdentity ≈ 一个人的"身份证 + 银行卡 + 营业执照"</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentIdentity {

    /** Agent 唯一标识 (UUID v4) */
    private String agentId;

    /** Agent 名称（如 "天气助手"） */
    private String name;

    /** Agent 描述（如 "提供全球天气查询服务"） */
    private String description;

    /** 去中心化身份 (DID, 如 "did:web:weather-agent.com") */
    private String did;

    /** 主钱包地址（如 "0x1234..."） */
    private String walletAddress;

    /** 主网络类型 */
    private NetworkType network;

    /** 角色：BUYER(只买) / SELLER(只卖) / BOTH(既买又卖) */
    private AgentRole role;

    /** 当前余额（缓存值，非实时链上余额） */
    private BigDecimal balance;

    /** 风险等级 */
    private RiskLevel riskLevel;

    /** 信用额度（后付费模式下可用） */
    private BigDecimal creditLimit;

    /** 扩展元数据（自定义 KV） */
    private Map<String, String> metadata;

    /** 创建时间 */
    private Instant createdAt;

    /** 更新时间 */
    private Instant updatedAt;
}
