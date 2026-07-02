package com.agentx4j.storage.repository;

import com.agentx4j.storage.persistence.entity.AgentEntity;

import java.util.List;
import java.util.Optional;

/**
 * Agent 数据访问接口。
 */
public interface AgentRepository {

    /** 保存 Agent */
    AgentEntity save(AgentEntity entity);

    /** 根据 agentId 查询 */
    Optional<AgentEntity> findByAgentId(String agentId);

    /** 根据钱包地址查询 */
    Optional<AgentEntity> findByWalletAddress(String walletAddress);

    /** 查询所有活跃 Agent */
    List<AgentEntity> findAllActive();

    /** 更新信誉评分 */
    void updateReputationScore(String agentId, Double score);

    /** 删除 Agent */
    void deleteByAgentId(String agentId);

    /** 统计 Agent 数量 */
    long count();
}
