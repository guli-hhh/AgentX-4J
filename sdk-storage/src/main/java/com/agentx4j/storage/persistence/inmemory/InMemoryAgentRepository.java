package com.agentx4j.storage.persistence.inmemory;

import com.agentx4j.storage.persistence.entity.AgentEntity;
import com.agentx4j.storage.repository.AgentRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Agent 内存存储实现。
 *
 * <p>适用于测试和单实例部署。生产环境请使用数据库实现。</p>
 */
public class InMemoryAgentRepository implements AgentRepository {

    private final Map<String, AgentEntity> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public AgentEntity save(AgentEntity entity) {
        if (entity.getId() == null) {
            entity.setId(idGenerator.getAndIncrement());
        }
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(java.time.Instant.now());
        }
        entity.setUpdatedAt(java.time.Instant.now());
        store.put(entity.getAgentId(), entity);
        return entity;
    }

    @Override
    public Optional<AgentEntity> findByAgentId(String agentId) {
        return Optional.ofNullable(store.get(agentId));
    }

    @Override
    public Optional<AgentEntity> findByWalletAddress(String walletAddress) {
        return store.values().stream()
                .filter(e -> walletAddress.equals(e.getWalletAddress()))
                .findFirst();
    }

    @Override
    public List<AgentEntity> findAllActive() {
        return store.values().stream()
                .filter(e -> "ACTIVE".equals(e.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    public void updateReputationScore(String agentId, Double score) {
        AgentEntity entity = store.get(agentId);
        if (entity != null) {
            entity.setReputationScore(score);
            entity.setUpdatedAt(java.time.Instant.now());
        }
    }

    @Override
    public void deleteByAgentId(String agentId) {
        store.remove(agentId);
    }

    @Override
    public long count() {
        return store.size();
    }
}
