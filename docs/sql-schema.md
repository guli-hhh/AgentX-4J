# 数据库表结构

> AgentX-4J SDK 所需的数据库表结构（MySQL 8.x）。

---

## 建表脚本

```sql
-- ============================================
-- AgentX-4J 数据库表结构
-- 数据库: MySQL 8.x
-- 字符集: utf8mb4
-- ============================================

CREATE DATABASE IF NOT EXISTS agentx4j DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE agentx4j;

-- ============================================
-- 1. Agent 表
-- ============================================
CREATE TABLE IF NOT EXISTS `agent` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `agent_id` VARCHAR(36) NOT NULL COMMENT 'Agent 唯一标识 (UUID)',
    `name` VARCHAR(128) NOT NULL COMMENT 'Agent 名称',
    `description` TEXT COMMENT 'Agent 描述',
    `did` VARCHAR(255) DEFAULT NULL COMMENT '去中心化身份 (DID)',
    `wallet_address` VARCHAR(128) DEFAULT NULL COMMENT '主钱包地址',
    `network` VARCHAR(32) DEFAULT NULL COMMENT '主网络类型',
    `role` ENUM('BUYER', 'SELLER', 'BOTH') NOT NULL DEFAULT 'BOTH' COMMENT '角色',
    `status` ENUM('ACTIVE', 'SUSPENDED', 'BANNED') NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
    `reputation_score` DECIMAL(5,2) DEFAULT 50.00 COMMENT '信誉评分 (0-100)',
    `credit_limit` DECIMAL(18,8) DEFAULT 0 COMMENT '信用额度',
    `total_transactions` INT DEFAULT 0 COMMENT '总交易数',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_agent_id` (`agent_id`),
    UNIQUE KEY `uk_wallet_address` (`wallet_address`),
    KEY `idx_status` (`status`),
    KEY `idx_reputation` (`reputation_score` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent 表';

-- ============================================
-- 2. 钱包表
-- ============================================
CREATE TABLE IF NOT EXISTS `wallet` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `wallet_id` VARCHAR(36) NOT NULL COMMENT '钱包 ID (UUID)',
    `agent_id` VARCHAR(36) NOT NULL COMMENT '所属 Agent ID',
    `network` VARCHAR(32) NOT NULL COMMENT '网络类型',
    `address` VARCHAR(128) NOT NULL COMMENT '钱包地址',
    `balance` DECIMAL(18,8) NOT NULL DEFAULT 0 COMMENT '当前余额',
    `locked_balance` DECIMAL(18,8) NOT NULL DEFAULT 0 COMMENT '冻结余额',
    `token_address` VARCHAR(128) DEFAULT NULL COMMENT '代币合约地址',
    `label` VARCHAR(64) DEFAULT NULL COMMENT '钱包标签',
    `status` ENUM('ACTIVE', 'FROZEN') NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_wallet_id` (`wallet_id`),
    UNIQUE KEY `uk_agent_network` (`agent_id`, `network`),
    KEY `idx_address` (`address`),
    KEY `idx_agent_id` (`agent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='钱包表';

-- ============================================
-- 3. 交易记录表
-- ============================================
CREATE TABLE IF NOT EXISTS `transaction` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `transaction_id` VARCHAR(36) NOT NULL COMMENT '交易 ID (UUID)',
    `idempotency_key` VARCHAR(128) NOT NULL COMMENT '幂等 Key',
    `from_agent_id` VARCHAR(36) DEFAULT NULL COMMENT '付款方 Agent ID',
    `to_agent_id` VARCHAR(36) NOT NULL COMMENT '收款方 Agent ID',
    `service_id` VARCHAR(36) DEFAULT NULL COMMENT '关联服务 ID',
    `tool_name` VARCHAR(128) DEFAULT NULL COMMENT '关联 MCP 工具名',
    `amount` DECIMAL(18,8) NOT NULL COMMENT '交易金额',
    `platform_fee` DECIMAL(18,8) NOT NULL DEFAULT 0 COMMENT '平台佣金',
    `net_amount` DECIMAL(18,8) NOT NULL COMMENT '实际到账 (amount - platform_fee)',
    `currency` VARCHAR(16) NOT NULL DEFAULT 'USDC' COMMENT '货币',
    `network` VARCHAR(64) NOT NULL COMMENT '网络类型',
    `scheme` ENUM('EXACT', 'UPTO', 'BATCH_SETTLEMENT') NOT NULL COMMENT '计费方式',
    `status` ENUM('PENDING', 'SETTLED', 'COMPLETED', 'FAILED', 'REFUNDED', 'DISPUTED') NOT NULL DEFAULT 'PENDING' COMMENT '交易状态',
    `tx_hash` VARCHAR(128) DEFAULT NULL COMMENT '链上交易哈希',
    `facilitator_tx_id` VARCHAR(128) DEFAULT NULL COMMENT 'Facilitator 交易 ID',
    `error_message` VARCHAR(512) DEFAULT NULL COMMENT '失败原因',
    `metadata` JSON COMMENT '扩展元数据',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `settled_at` TIMESTAMP NULL DEFAULT NULL COMMENT '结算时间',
    `completed_at` TIMESTAMP NULL DEFAULT NULL COMMENT '完成时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_transaction_id` (`transaction_id`),
    UNIQUE KEY `uk_idempotency_key` (`idempotency_key`),
    KEY `idx_from_agent` (`from_agent_id`, `created_at` DESC),
    KEY `idx_to_agent` (`to_agent_id`, `created_at` DESC),
    KEY `idx_status` (`status`),
    KEY `idx_created` (`created_at` DESC),
    KEY `idx_tx_hash` (`tx_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交易记录表';

-- ============================================
-- 4. 对账单表
-- ============================================
CREATE TABLE IF NOT EXISTS `statement` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `agent_id` VARCHAR(36) NOT NULL COMMENT 'Agent ID',
    `statement_date` DATE NOT NULL COMMENT '对账日期',
    `total_income` DECIMAL(18,8) NOT NULL DEFAULT 0 COMMENT '总收入',
    `total_expense` DECIMAL(18,8) NOT NULL DEFAULT 0 COMMENT '总支出',
    `commission` DECIMAL(18,8) NOT NULL DEFAULT 0 COMMENT '平台佣金',
    `net_amount` DECIMAL(18,8) NOT NULL DEFAULT 0 COMMENT '净额',
    `tx_count` INT NOT NULL DEFAULT 0 COMMENT '交易笔数',
    `status` ENUM('PENDING', 'CONFIRMED', 'DISPUTED') NOT NULL DEFAULT 'PENDING' COMMENT '状态',
    `confirmed_at` TIMESTAMP NULL DEFAULT NULL COMMENT '确认时间',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_agent_date` (`agent_id`, `statement_date`),
    KEY `idx_statement_date` (`statement_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对账单表';
```

## ER 关系图

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   agent      │     │   wallet     │     │ transaction  │
├──────────────┤     ├──────────────┤     ├──────────────┤
│ id (PK)      │◀────│ agent_id(FK) │     │ id (PK)      │
│ agent_id     │     │ wallet_id    │     │ from_agent───┼──▶ agent.agent_id
│ name         │     │ network      │     │ to_agent─────┼──▶ agent.agent_id
│ wallet_addr  │     │ address      │     │ amount       │
│ role         │     │ balance      │     │ status       │
│ status       │     │ status       │     │ tx_hash      │
│ reputation   │     └──────────────┘     │ created_at   │
└──────────────┘                          └──────────────┘
                                   ┌──────────────┐
                                   │ statement    │
                                   ├──────────────┤
                                   │ agent_id(FK) │
                                   │ date         │
                                   │ total_income │
                                   │ total_expense│
                                   │ net_amount   │
                                   └──────────────┘
```
