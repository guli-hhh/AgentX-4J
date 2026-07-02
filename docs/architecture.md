# AgentX-4J 架构设计

> 本文档描述 AgentX-4J SDK 的整体架构设计。
> 对应设计文档：`agent-sdk-design.md` 第一至第九章。

---

## 一、系统定位

**AgentX-4J 是一个 Java SDK，为 AI Agent 提供基于 x402 协议的标准化交易结算能力。**

```
AgentX-4J 不是"跨语言 SDK"
AgentX-4J 是"Java 生态的 x402 SDK"

类比：
  Stripe Java SDK  ≠  Stripe 的跨语言方案
  Stripe Python SDK ≠  Stripe 的跨语言方案
  
  它们是同一份 API 规范的不同语言实现
```

### 核心价值

| 价值 | 说明 |
|------|------|
| **Java 生态最佳体验** | Spring Boot 集成、声明式注解、自动装配 |
| **x402 协议兼容** | 基于开放标准，与其他 x402 实现互操作 |
| **可插拔架构** | Scheme、Adapter、Store 均可替换 |
| **参考实现** | 其他语言的 Java 开发者可以参考 AgentX-4J 的设计 |

### 跨语言策略

```
所有语言通过 x402 标准 HTTP 协议互操作，不需要中间层。

Java 开发者 → AgentX-4J SDK（Maven 依赖）
Python 开发者 → x402 官方 Python SDK（pip install x402）
Go 开发者 → x402 官方 Go SDK
Node.js 开发者 → x402 官方 TypeScript SDK
```

---

## 二、协议栈总览

```
┌─────────────────────────────────────────────────────────────────┐
│                      Agent 应用层                                │
│                                                                 │
│   ┌──────────────┐    ┌──────────────┐    ┌──────────────┐     │
│   │   Agent A    │    │   Agent B    │    │   Agent C    │     │
│   │ (Buyer/买家) │    │(Seller/卖家) │    │ (Both/两者)  │     │
│   └──────┬───────┘    └──────┬───────┘    └──────┬───────┘     │
│          │                  │                  │               │
├──────────┼──────────────────┼──────────────────┼───────────────┤
│          │   AgentX-4J SDK  │                  │               │
│          │                  │                  │               │
│  ┌───────▼──────────────────▼──────────────────▼────────────┐  │
│  │              sdk-mcp (MCP + x402 集成层)                  │  │
│  │  ┌─────────────────┐       ┌─────────────────────────┐  │  │
│  │  │ MCP Client 集成  │       │ MCP Server 集成          │  │  │
│  │  │ - 自动发现服务   │       │ - 声明工具价格           │  │  │
│  │  │ - 自动处理 402   │       │ - 自动返回 402 响应      │  │  │
│  │  │ - 自动签名支付   │       │ - 自动验证支付           │  │  │
│  │  │ - 自动重试请求   │       │ - 自动结算               │  │  │
│  │  └────────┬────────┘       └───────────┬─────────────┘  │  │
│  └───────────┼─────────────────────────────┼────────────────┘  │
│              │                             │                   │
│  ┌───────────▼─────────────────────────────▼────────────────┐  │
│  │               sdk-x402 (x402 协议 Java 实现)              │  │
│  │                                                           │  │
│  │  ┌───────────┐  ┌───────────┐  ┌───────────────────────┐│  │
│  │  │  Exact    │  │   Upto    │  │  Batch Settlement     ││  │
│  │  │  Scheme   │  │  Scheme   │  │  Scheme               ││  │
│  │  │ 固定价格   │  │ 按用量    │  │ 批量结算               ││  │
│  │  └───────────┘  └───────────┘  └───────────────────────┘│  │
│  │                                                           │  │
│  │  ┌─────────────────────────────────────────────────────┐│  │
│  │  │          Network Adapter (网络适配器，可插拔)          ││  │
│  │  │   EVM │ SVM │ TON │ Stellar │ Algorand │ ...       ││  │
│  │  └─────────────────────────────────────────────────────┘│  │
│  │                                                           │  │
│  │  ┌─────────────────────────────────────────────────────┐│  │
│  │  │          Facilitator Client (支付协调方客户端)        ││  │
│  │  │   /verify — 验证支付   /settle — 执行结算            ││  │
│  │  └─────────────────────────────────────────────────────┘│  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  支撑模块层                                                │  │
│  │  ┌─────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐ │  │
│  │  │sdk-     │ │sdk-risk  │ │sdk-      │ │sdk-bazaar    │ │  │
│  │  │wallet   │ │风控引擎   │ │settlement│ │服务发现      │ │  │
│  │  │钱包体系  │ │          │ │结算引擎   │ │              │ │  │
│  │  └─────────┘ └──────────┘ └──────────┘ └──────────────┘ │  │
│  │  ┌─────────────────┐  ┌─────────────────────────────────┐│  │
│  │  │sdk-idempotency  │  │sdk-storage (持久化层)           ││  │
│  │  │幂等性保障        │  │MySQL + Redis                    ││  │
│  │  └─────────────────┘  └─────────────────────────────────┘│  │
│  └───────────────────────────────────────────────────────────┘  │
├─────────────────────────────────────────────────────────────────┤
│                      区块链网络层                                 │
│   Base/Ethereum │ Solana │ TON │ Stellar │ Algorand │ ...      │
└─────────────────────────────────────────────────────────────────┘
```

---

## 三、模块依赖关系

```
sdk-spring-boot-starter
    ├── sdk-x402
    │   ├── sdk-core
    │   ├── sdk-wallet
    │   │   └── sdk-core
    │   └── sdk-idempotency
    │       └── sdk-core
    └── sdk-core
```

### 模块说明

| 模块 | 依赖 | 说明 |
|------|------|------|
| `sdk-core` | 无 | 纯 POJO + 枚举，无外部依赖 |
| `sdk-wallet` | sdk-core, web3j | EVM 钱包管理 |
| `sdk-idempotency` | sdk-core, Caffeine | 幂等性保障 |
| `sdk-x402` | sdk-core, sdk-wallet, sdk-idempotency, web3j, OkHttp | x402 协议核心 |
| `sdk-spring-boot-starter` | sdk-x402, sdk-core, Spring Boot | 自动装配 |

---

## 四、核心流程

### 4.1 x402 协议完整流程（13 步）

```
Client (买家)              Resource Server (卖家)         Facilitator (支付网关)      Blockchain (区块链)
  │                            │                              │                          │
  │  ① HTTP Request           │                              │                          │
  │  GET /api/weather         │                              │                          │
  │  ?city=Beijing            │                              │                          │
  │───────────────────────────▶│                              │                          │
  │                            │                              │                          │
  │  ② 402 Payment Required   │                              │                          │
  │  PAYMENT-REQUIRED:        │                              │                          │
  │  (Base64编码的价目表)       │                              │                          │
  │◀───────────────────────────│                              │                          │
  │                            │                              │                          │
  │  ③ 客户端自动处理:         │                              │                          │
  │  - 解码 Base64             │                              │                          │
  │  - 解析 PaymentRequirements│                              │                          │
  │  - 选择 scheme + network   │                              │                          │
  │  - 检查钱包余额            │                              │                          │
  │  - 构建支付数据            │                              │                          │
  │  - 用私钥签名              │                              │                          │
  │  - 生成 PaymentPayload     │                              │                          │
  │                            │                              │                          │
  │  ④ Request + SIGNATURE    │                              │                          │
  │  PAYMENT-SIGNATURE:        │                              │                          │
  │  (Base64编码的签名支付)     │                              │                          │
  │───────────────────────────▶│                              │                          │
  │                            │                              │                          │
  │                            │  ⑤ POST /verify              │                          │
  │                            │  (验证支付签名是否有效)        │                          │
  │                            │─────────────────────────────▶│                          │
  │                            │                              │                          │
  │                            │                              │  ⑥ 验证:                 │
  │                            │                              │  - 签名是否有效？         │
  │                            │                              │  - 金额是否匹配？         │
  │                            │                              │  - nonce 是否未使用？     │
  │                            │                              │  - 是否在有效期内？       │
  │                            │                              │  - 链上余额是否充足？     │
  │                            │                              │                          │
  │                            │  ⑦ Verification Response     │                          │
  │                            │  { valid: true/false }       │                          │
  │                            │◀─────────────────────────────│                          │
  │                            │                              │                          │
  │                            │  ⑧ 执行业务逻辑              │                          │
  │                            │  (验证通过，开始干活)          │                          │
  │                            │                              │                          │
  │                            │  ⑨ POST /settle              │                          │
  │                            │  (请求链上结算)               │                          │
  │                            │─────────────────────────────▶│                          │
  │                            │                              │                          │
  │                            │                              │  ⑩ 提交链上交易          │
  │                            │                              │  (EIP-3009 或 Permit2)   │
  │                            │                              │─────────────────────────▶│
  │                            │                              │                          │
  │                            │                              │  ⑪ 等待链上确认          │
  │                            │                              │  (通常 1-3 个区块)       │
  │                            │                              │◀─────────────────────────│
  │                            │                              │                          │
  │                            │  ⑫ Settlement Response       │                          │
  │                            │  { txHash, blockNumber }     │                          │
  │                            │◀─────────────────────────────│                          │
  │                            │                              │                          │
  │  ⑬ 200 OK + 结果          │                              │                          │
  │  PAYMENT-RESPONSE:         │                              │                          │
  │  (Base64编码的结算凭证)     │                              │                          │
  │◀───────────────────────────│                              │                          │
```

### 4.2 卖家侧（Resource Server）流程

```
                     HTTP Request
                          │
                          ▼
               ┌─────────────────────┐
               │ PaymentInterceptor  │ ← Spring Boot 拦截器
               │ (中间件)            │
               └──────────┬──────────┘
                          │
               是否有 PAYMENT-SIGNATURE?
                     ╱           ╲
                   是             否
                   │              │
                   ▼              ▼
         ┌─────────────┐   ┌──────────────────┐
         │ 验证支付    │   │ 返回 402 响应    │
         │ (verify)    │   │ + PaymentRequirements
         └──────┬──────┘   └──────────────────┘
                │
           验证通过?
           ╱       ╲
         是          否
         │           │
         ▼           ▼
   ┌──────────┐  ┌──────────────┐
   │ 执行业务 │  │ 返回 402    │
   │ 逻辑     │  │ + 错误原因  │
   └────┬─────┘  └──────────────┘
        │
        ▼
   ┌──────────┐
   │ 结算支付 │
   │ (settle) │
   └────┬─────┘
        │
        ▼
   ┌──────────┐
   │ 返回 200 │
   │ + 结果   │
   │ + PAYMENT│
   │ -RESPONSE│
   └──────────┘
```

### 4.3 买家侧（Client）流程

```
                     调用 API
                          │
                          ▼
               ┌─────────────────────┐
               │ 发送 HTTP 请求      │
               └──────────┬──────────┘
                          │
                     响应状态码?
                  ╱       │       ╲
               200       402     其他
                │         │       │
                │         ▼       │
                │   ┌──────────┐  │
                │   │ 解析     │  │
                │   │ Payment  │  │
                │   │ Requirements│
                │   └────┬─────┘  │
                │        │        │
                │        ▼        │
                │   ┌──────────┐  │
                │   │ 选择支付 │  │
                │   │ 方案     │  │
                │   └────┬─────┘  │
                │        │        │
                │        ▼        │
                │   ┌──────────┐  │
                │   │ 签名支付 │  │
                │   │ Payload  │  │
                │   └────┬─────┘  │
                │        │        │
                │        ▼        │
                │   ┌──────────┐  │
                │   │ 重试请求 │  │
                │   │ + SIGNATURE│ │
                │   └──────────┘  │
                │                 │
                ▼                 ▼
           ┌──────────┐     ┌──────────┐
           │ 返回结果 │     │ 返回结果 │
           └──────────┘     └──────────┘
```

### 4.4 交易状态流转

```
PENDING ──→ SETTLED ──→ COMPLETED
   │            │
   ├──→ FAILED  ├──→ REFUNDED
   │
   └──→ DISPUTED ──→ REFUNDED
                  ──→ SETTLED
```

---

## 五、核心组件

### 5.1 X402ResourceServer（卖家侧）

| 组件 | 职责 |
|------|------|
| `X402ResourceServer` | 核心服务，处理支付验证和结算 |
| `PaymentInterceptor` | Spring Boot 拦截器，自动拦截受保护请求 |
| `VerifyResult` | 验证结果封装 |
| `SettleResult` | 结算结果封装 |

### 5.2 X402Client（买家侧）

| 组件 | 职责 |
|------|------|
| `X402Client` | 核心客户端，自动处理 402 → 支付 → 重试 |

### 5.3 Scheme 体系

| 组件 | 职责 |
|------|------|
| `Scheme` | 支付方案接口 |
| `SchemeRegistry` | 支付方案注册表（支持精确匹配 + 通配符匹配） |
| `ExactScheme` | 固定价格方案 |
| `UptoScheme` | 按用量方案（待实现） |
| `BatchSettlementScheme` | 批量结算方案（待实现） |

### 5.4 Facilitator 体系

| 组件 | 职责 |
|------|------|
| `FacilitatorClient` | Facilitator 接口 |
| `HttpFacilitatorClient` | HTTP 实现（OkHttp） |
| `VerifyResponse` | 验证响应 |
| `SettleResponse` | 结算响应 |
| `SupportedNetworksResponse` | 支持网络响应 |

### 5.5 Spring Boot 集成

| 组件 | 职责 |
|------|------|
| `@EnableX402` | 启用 AgentX-4J 自动配置 |
| `@X402Protected` | 声明接口需要支付才能访问 |
| `@AutoSettle` | 自动结算注解 |
| `X402ProtectedAspect` | AOP 切面，拦截 @X402Protected 方法 |
| `AgentX4JAutoConfiguration` | Spring Boot 自动配置类 |
| `AgentX4JProperties` | 配置属性绑定 |

---

## 六、数据流

### 6.1 支付验证数据流

```
HTTP Request
    │
    ├─ PAYMENT-SIGNATURE header (Base64)
    │       │
    │       ▼
    │   X402ResourceServer.verifyPayment()
    │       │
    │       ├─ Scheme.verify() — 本地验证（签名/金额/nonce/有效期）
    │       │
    │       └─ FacilitatorClient.verify() — 链上验证
    │               │
    │               ▼
    │           POST /verify → Facilitator → Blockchain
    │               │
    │               ▼
    │           VerifyResponse { valid, payer }
    │
    ▼
业务逻辑执行
    │
    ▼
X402ResourceServer.settlePayment()
    │
    ├─ Scheme.settle()
    │
    └─ FacilitatorClient.settle() — 链上结算
            │
            ▼
        POST /settle → Facilitator → Blockchain
            │
            ▼
        SettleResponse { txHash, blockNumber }
```

### 6.2 幂等性保障数据流

```
请求到达
    │
    ▼
提取 paymentId（从 PaymentPayload.extensions）
    │
    ▼
IdempotencyStore.get(paymentId)
    │
    ├── 缓存命中 + 指纹匹配 → 返回缓存结果（不重复扣费）
    │
    ├── 缓存命中 + 指纹不匹配 → 返回 409 Conflict
    │
    └── 缓存未命中 → 正常处理 → 缓存结果（TTL 24h）
```

---

## 七、配置体系

```
AgentX4JProperties
├── enabled                    # 是否启用（默认 true）
├── facilitatorUrl            # Facilitator URL（默认 x402.org 测试网）
├── walletPrivateKey          # 钱包私钥（买家侧）
├── network                   # 默认网络（默认 eip155:84532）
├── asset                     # 默认代币（默认 Base Sepolia USDC）
├── defaultPrice              # 默认价格（默认 $0.001）
├── payTo                     # 收款地址（卖家侧）
├── maxTimeoutSeconds         # 超时时间（默认 60 秒）
└── rpcUrls                   # RPC 节点地址（按网络分组）
```

---

## 八、扩展点

### 8.1 自定义 Scheme

```java
public class MyCustomScheme implements Scheme {
    @Override
    public String getName() { return "my-scheme"; }

    @Override
    public PaymentPayload createPayment(SchemeContext context) { ... }

    @Override
    public VerifyResult verify(PaymentPayload payload, PaymentRequirement req) { ... }

    @Override
    public SettleResult settle(PaymentPayload payload, PaymentRequirement req) { ... }

    @Override
    public boolean supportsNetwork(String network) { return true; }
}

// 注册
resourceServer.registerScheme("eip155:*", new MyCustomScheme());
```

### 8.2 自定义 FacilitatorClient

```java
public class MyFacilitatorClient implements FacilitatorClient {
    @Override
    public VerifyResponse verify(PaymentPayload payload, PaymentRequirement req) { ... }

    @Override
    public SettleResponse settle(PaymentPayload payload, PaymentRequirement req) { ... }
}
```

### 8.3 自定义 IdempotencyStore

```java
public class RedisIdempotencyStore implements IdempotencyStore {
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void save(String paymentId, String fingerprint, Object result, Duration ttl) {
        redisTemplate.opsForValue().set(paymentId, result, ttl);
    }

    @Override
    public IdempotencyEntry get(String paymentId) {
        // 从 Redis 获取
    }
}
```

### 8.4 自定义 NetworkAdapter

```java
public class SolanaNetworkAdapter implements NetworkAdapter {
    @Override
    public String getNetworkPrefix() { return "solana"; }

    @Override
    public boolean verifySignature(PaymentPayload payload, PaymentRequirement req) { ... }

    @Override
    public String submitTransaction(PaymentPayload payload, PaymentRequirement req) { ... }
}
```
#远程仓库地址
Sonatype Central Portal (https://central.sonatype.com)，
