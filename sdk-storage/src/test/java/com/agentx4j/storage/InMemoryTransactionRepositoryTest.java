package com.agentx4j.storage;

import com.agentx4j.storage.persistence.entity.TransactionEntity;
import com.agentx4j.storage.persistence.inmemory.InMemoryTransactionRepository;
import com.agentx4j.storage.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * InMemoryTransactionRepository 测试。
 */
class InMemoryTransactionRepositoryTest {

    private TransactionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryTransactionRepository();
    }

    @Test
    void testSaveAndFind() {
        TransactionEntity entity = createTestTransaction("tx-001", "agent-a", "agent-b", "1000");
        repository.save(entity);

        var found = repository.findByTransactionId("tx-001");
        assertTrue(found.isPresent());
        assertEquals("agent-a", found.get().getFromAgentId());
        assertEquals("agent-b", found.get().getToAgentId());
    }

    @Test
    void testFindByIdempotencyKey() {
        TransactionEntity entity = createTestTransaction("tx-001", "agent-a", "agent-b", "1000");
        entity.setIdempotencyKey("pay_abc123");
        repository.save(entity);

        var found = repository.findByIdempotencyKey("pay_abc123");
        assertTrue(found.isPresent());
        assertEquals("tx-001", found.get().getTransactionId());
    }

    @Test
    void testFindByAgentId() {
        repository.save(createTestTransaction("tx-001", "agent-a", "agent-b", "1000"));
        repository.save(createTestTransaction("tx-002", "agent-b", "agent-c", "2000"));
        repository.save(createTestTransaction("tx-003", "agent-c", "agent-a", "500"));

        List<TransactionEntity> agentATx = repository.findByAgentId("agent-a", 0, 10);
        assertEquals(2, agentATx.size()); // tx-001 (from) + tx-003 (to)

        List<TransactionEntity> agentBTx = repository.findByAgentId("agent-b", 0, 10);
        assertEquals(2, agentBTx.size()); // tx-001 (to) + tx-002 (from)
    }

    @Test
    void testFindPendingTransactions() {
        TransactionEntity tx1 = createTestTransaction("tx-001", "agent-a", "agent-b", "1000");
        tx1.setStatus("PENDING");
        repository.save(tx1);

        TransactionEntity tx2 = createTestTransaction("tx-002", "agent-a", "agent-b", "2000");
        tx2.setStatus("SETTLED");
        repository.save(tx2);

        List<TransactionEntity> pending = repository.findPendingTransactions();
        assertEquals(1, pending.size());
        assertEquals("tx-001", pending.get(0).getTransactionId());
    }

    @Test
    void testUpdateStatus() {
        TransactionEntity entity = createTestTransaction("tx-001", "agent-a", "agent-b", "1000");
        entity.setStatus("PENDING");
        repository.save(entity);

        repository.updateStatus("tx-001", "SETTLED");

        var found = repository.findByTransactionId("tx-001");
        assertTrue(found.isPresent());
        assertEquals("SETTLED", found.get().getStatus());
        assertNotNull(found.get().getSettledAt());
    }

    @Test
    void testSumIncomeByAgentId() {
        TransactionEntity tx1 = createTestTransaction("tx-001", "agent-a", "agent-b", "1000");
        tx1.setStatus("SETTLED");
        tx1.setNetAmount(new BigDecimal("900"));
        repository.save(tx1);

        TransactionEntity tx2 = createTestTransaction("tx-002", "agent-c", "agent-b", "2000");
        tx2.setStatus("COMPLETED");
        tx2.setNetAmount(new BigDecimal("1800"));
        repository.save(tx2);

        BigDecimal income = repository.sumIncomeByAgentId("agent-b");
        assertEquals(new BigDecimal("2700"), income);
    }

    @Test
    void testCountByAgentId() {
        repository.save(createTestTransaction("tx-001", "agent-a", "agent-b", "1000"));
        repository.save(createTestTransaction("tx-002", "agent-a", "agent-c", "2000"));
        repository.save(createTestTransaction("tx-003", "agent-c", "agent-a", "500"));

        assertEquals(3, repository.countByAgentId("agent-a"));
        assertEquals(1, repository.countByAgentId("agent-b"));
        assertEquals(0, repository.countByAgentId("agent-d"));
    }

    private TransactionEntity createTestTransaction(String txId, String from, String to, String amount) {
        return TransactionEntity.builder()
                .transactionId(txId)
                .fromAgentId(from)
                .toAgentId(to)
                .amount(new BigDecimal(amount))
                .platformFee(new BigDecimal("100"))
                .netAmount(new BigDecimal(amount).subtract(new BigDecimal("100")))
                .currency("USDC")
                .network("eip155:84532")
                .scheme("EXACT")
                .status("PENDING")
                .build();
    }
}
