package com.agentx4j.storage.repository;

import com.agentx4j.storage.persistence.entity.WalletEntity;

import java.util.List;
import java.util.Optional;

/**
 * 钱包数据访问接口。
 */
public interface WalletRepository {

    /** 保存钱包 */
    WalletEntity save(WalletEntity entity);

    /** 根据 walletId 查询 */
    Optional<WalletEntity> findByWalletId(String walletId);

    /** 根据 agentId 查询所有钱包 */
    List<WalletEntity> findByAgentId(String agentId);

    /** 根据地址查询 */
    Optional<WalletEntity> findByAddress(String address);

    /** 查询 Agent 在指定网络的钱包 */
    Optional<WalletEntity> findByAgentIdAndNetwork(String agentId, String network);

    /** 更新余额 */
    void updateBalance(String walletId, java.math.BigDecimal balance);

    /** 冻结余额 */
    void lockBalance(String walletId, java.math.BigDecimal amount);

    /** 解冻余额 */
    void unlockBalance(String walletId, java.math.BigDecimal amount);

    /** 删除钱包 */
    void deleteByWalletId(String walletId);
}
