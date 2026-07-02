package com.agentx4j.storage.persistence.inmemory;

import com.agentx4j.storage.persistence.entity.WalletEntity;
import com.agentx4j.storage.repository.WalletRepository;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 钱包内存存储实现。
 *
 * <p>适用于测试和单实例部署。生产环境请使用数据库实现。</p>
 */
public class InMemoryWalletRepository implements WalletRepository {

    private final Map<String, WalletEntity> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public WalletEntity save(WalletEntity entity) {
        if (entity.getId() == null) {
            entity.setId(idGenerator.getAndIncrement());
        }
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(java.time.Instant.now());
        }
        entity.setUpdatedAt(java.time.Instant.now());
        store.put(entity.getWalletId(), entity);
        return entity;
    }

    @Override
    public Optional<WalletEntity> findByWalletId(String walletId) {
        return Optional.ofNullable(store.get(walletId));
    }

    @Override
    public List<WalletEntity> findByAgentId(String agentId) {
        return store.values().stream()
                .filter(e -> agentId.equals(e.getAgentId()))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<WalletEntity> findByAddress(String address) {
        return store.values().stream()
                .filter(e -> address.equalsIgnoreCase(e.getAddress()))
                .findFirst();
    }

    @Override
    public Optional<WalletEntity> findByAgentIdAndNetwork(String agentId, String network) {
        return store.values().stream()
                .filter(e -> agentId.equals(e.getAgentId()) && network.equals(e.getNetwork()))
                .findFirst();
    }

    @Override
    public void updateBalance(String walletId, BigDecimal balance) {
        WalletEntity entity = store.get(walletId);
        if (entity != null) {
            entity.setBalance(balance);
            entity.setUpdatedAt(java.time.Instant.now());
        }
    }

    @Override
    public void lockBalance(String walletId, BigDecimal amount) {
        WalletEntity entity = store.get(walletId);
        if (entity != null) {
            entity.setLockedBalance(entity.getLockedBalance().add(amount));
            entity.setUpdatedAt(java.time.Instant.now());
        }
    }

    @Override
    public void unlockBalance(String walletId, BigDecimal amount) {
        WalletEntity entity = store.get(walletId);
        if (entity != null) {
            entity.setLockedBalance(entity.getLockedBalance().subtract(amount));
            entity.setUpdatedAt(java.time.Instant.now());
        }
    }

    @Override
    public void deleteByWalletId(String walletId) {
        store.remove(walletId);
    }
}
