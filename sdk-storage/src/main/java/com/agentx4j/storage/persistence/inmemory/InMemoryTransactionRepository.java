package com.agentx4j.storage.persistence.inmemory;

import com.agentx4j.storage.persistence.entity.TransactionEntity;
import com.agentx4j.storage.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 交易记录内存存储实现。
 *
 * <p>适用于测试和单实例部署。生产环境请使用数据库实现。</p>
 */
public class InMemoryTransactionRepository implements TransactionRepository {

    private final Map<String, TransactionEntity> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public TransactionEntity save(TransactionEntity entity) {
        if (entity.getId() == null) {
            entity.setId(idGenerator.getAndIncrement());
        }
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        store.put(entity.getTransactionId(), entity);
        return entity;
    }

    @Override
    public Optional<TransactionEntity> findByTransactionId(String transactionId) {
        return Optional.ofNullable(store.get(transactionId));
    }

    @Override
    public Optional<TransactionEntity> findByIdempotencyKey(String idempotencyKey) {
        return store.values().stream()
                .filter(e -> idempotencyKey.equals(e.getIdempotencyKey()))
                .findFirst();
    }

    @Override
    public List<TransactionEntity> findByAgentId(String agentId, int page, int size) {
        return store.values().stream()
                .filter(e -> agentId.equals(e.getFromAgentId()) || agentId.equals(e.getToAgentId()))
                .sorted(Comparator.comparing(TransactionEntity::getCreatedAt).reversed())
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());
    }

    @Override
    public List<TransactionEntity> findByFromAgentId(String fromAgentId, int page, int size) {
        return store.values().stream()
                .filter(e -> fromAgentId.equals(e.getFromAgentId()))
                .sorted(Comparator.comparing(TransactionEntity::getCreatedAt).reversed())
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());
    }

    @Override
    public List<TransactionEntity> findByToAgentId(String toAgentId, int page, int size) {
        return store.values().stream()
                .filter(e -> toAgentId.equals(e.getToAgentId()))
                .sorted(Comparator.comparing(TransactionEntity::getCreatedAt).reversed())
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());
    }

    @Override
    public List<TransactionEntity> findByTimeRange(Instant from, Instant to) {
        return store.values().stream()
                .filter(e -> !e.getCreatedAt().isBefore(from) && !e.getCreatedAt().isAfter(to))
                .sorted(Comparator.comparing(TransactionEntity::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<TransactionEntity> findPendingTransactions() {
        return store.values().stream()
                .filter(e -> "PENDING".equals(e.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    public void updateStatus(String transactionId, String status) {
        TransactionEntity entity = store.get(transactionId);
        if (entity != null) {
            entity.setStatus(status);
            if ("SETTLED".equals(status) && entity.getSettledAt() == null) {
                entity.setSettledAt(Instant.now());
            }
            if ("COMPLETED".equals(status) && entity.getCompletedAt() == null) {
                entity.setCompletedAt(Instant.now());
            }
        }
    }

    @Override
    public long countByAgentId(String agentId) {
        return store.values().stream()
                .filter(e -> agentId.equals(e.getFromAgentId()) || agentId.equals(e.getToAgentId()))
                .count();
    }

    @Override
    public BigDecimal sumIncomeByAgentId(String agentId) {
        return store.values().stream()
                .filter(e -> agentId.equals(e.getToAgentId()))
                .filter(e -> "SETTLED".equals(e.getStatus()) || "COMPLETED".equals(e.getStatus()))
                .map(TransactionEntity::getNetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal sumExpenseByAgentId(String agentId) {
        return store.values().stream()
                .filter(e -> agentId.equals(e.getFromAgentId()))
                .filter(e -> "SETTLED".equals(e.getStatus()) || "COMPLETED".equals(e.getStatus()))
                .map(TransactionEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
