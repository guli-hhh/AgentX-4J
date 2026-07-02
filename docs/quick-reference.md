# AgentX-4J 快速参考

> 快速查阅常用 API 和配置。

---

## 配置速查

### 最小配置（卖家侧）

```yaml
agent:
  x402:
    enabled: true
    facilitator-url: https://x402.org/facilitator
    pay-to: "0xYourAddress"
    network: eip155:84532
    asset: "0x036CbD53842c5426634e7929541eC2318f3dCF7e"
```

### 完整配置（买家+卖家）

```yaml
agent:
  x402:
    enabled: true
    facilitator-url: https://x402.org/facilitator
    wallet-private-key: ${WALLET_PRIVATE_KEY}
    pay-to: "0xYourAddress"
    network: eip155:84532
    asset: "0x036CbD53842c5426634e7929541eC2318f3dCF7e"
    default-price: "$0.001"
    max-timeout-seconds: 60
    rpc-urls:
      eip155:84532: https://sepolia.base.org
      eip155:8453: https://mainnet.base.org
```

### 配置属性完整列表

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enabled` | boolean | `true` | 是否启用 AgentX-4J |
| `facilitator-url` | String | `https://x402.org/facilitator` | Facilitator URL |
| `wallet-private-key` | String | - | 钱包私钥（买家侧必填） |
| `pay-to` | String | - | 收款地址（卖家侧必填） |
| `network` | String | `eip155:84532` | 默认网络 |
| `asset` | String | Base Sepolia USDC | 代币合约地址 |
| `default-price` | String | `$0.001` | 默认价格 |
| `max-timeout-seconds` | long | `60` | 超时时间（秒） |
| `rpc-urls` | Map | - | RPC 节点地址（按网络分组） |

---

## 注解速查

### @EnableX402

```java
@SpringBootApplication
@EnableX402
public class MyApp { }
```

### @X402Protected

```java
// 最简用法
@X402Protected(price = "$0.001")
@GetMapping("/api/data")
public Data getData() { }

// 完整配置
@X402Protected(
    price = "$0.005",
    network = "eip155:84532",
    asset = "0x...",
    payTo = "0x...",
    scheme = BillingScheme.EXACT,
    maxTimeoutSeconds = 60
)
```

### @AutoSettle

```java
@AutoSettle
public void handleSettlement(String txId) { }
```

---

## API 速查

### 卖家侧

```java
// 获取 X402ResourceServer（自动注入）
@Autowired
private X402ResourceServer resourceServer;

// 注册自定义 Scheme
resourceServer.registerScheme("eip155:*", new MyScheme());

// 手动验证支付
VerifyResult result = resourceServer.verifyPayment(payload, requirement);
if (result.isValid()) {
    String payer = result.getPayer();
}

// 手动结算
SettleResult settleResult = resourceServer.settlePayment(payload, requirement);
if (settleResult.isSuccess()) {
    String txHash = settleResult.getTxHash();
}
```

### 买家侧

```java
// 获取 X402Client（自动注入）
@Autowired
private X402Client x402Client;

// 选择支付方案
PaymentRequirement req = x402Client.selectPaymentRequirement(accepts);

// 创建签名支付
PaymentPayload payload = x402Client.createPayment(req);
```

### 钱包

```java
// 创建新钱包
EvmWallet wallet = EvmWallet.create();
String address = wallet.getAddress();

// 从私钥导入
EvmWallet wallet = EvmWallet.fromPrivateKey("0x...");

// 获取签名器
WalletSigner signer = wallet.getSigner();

// 签名
String sig = signer.signTransferWithAuthorization(from, to, value, nonce, validAfter, validBefore);
```

### 幂等性

```java
// 生成 Key
String paymentId = IdempotencyKeyGenerator.generate();

// 存储
IdempotencyStore store = new InMemoryIdempotencyStore();
store.save(paymentId, fingerprint, result, Duration.ofHours(24));

// 查询
IdempotencyEntry entry = store.get(paymentId);
if (entry != null && !entry.isExpired()) {
    Object cachedResult = entry.getResult();
}
```

---

## HTTP Headers 速查

| Header | 方向 | 说明 |
|--------|------|------|
| `PAYMENT-REQUIRED` | Server → Client | 402 响应中的支付要求（Base64 JSON） |
| `PAYMENT-SIGNATURE` | Client → Server | 签名后的支付载荷（Base64 JSON） |
| `PAYMENT-RESPONSE` | Server → Client | 结算结果凭证（Base64 JSON） |

---

## 网络标识速查

| 网络 | CAIP-2 | 链 ID |
|------|--------|-------|
| Base Sepolia | `eip155:84532` | 84532 |
| Base Mainnet | `eip155:8453` | 8453 |
| Ethereum Mainnet | `eip155:1` | 1 |
| Polygon | `eip155:137` | 137 |
| Arbitrum One | `eip155:42161` | 42161 |
| Solana Mainnet | `solana:5eykt4UsFv8P8NJdTREpY1vzqKqZKvdp` | - |

---

## 代币地址速查

| 网络 | 代币 | 合约地址 |
|------|------|---------|
| Base Sepolia | USDC | `0x036CbD53842c5426634e7929541eC2318f3dCF7e` |
| Base Mainnet | USDC | `0x833589fCD6eDb6E08f4c7C32D4f71b54bdA02913` |
| Ethereum | USDC | `0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48` |

---

## 枚举速查

### BillingScheme

```java
BillingScheme.EXACT              // "exact" - 固定价格
BillingScheme.UPTO               // "upto" - 按用量
BillingScheme.BATCH_SETTLEMENT   // "batch-settlement" - 批量结算
```

### TransactionStatus

```java
TransactionStatus.PENDING     // 待处理
TransactionStatus.SETTLED     // 已结算
TransactionStatus.COMPLETED   // 已完成
TransactionStatus.FAILED      // 失败
TransactionStatus.REFUNDED    // 已退款
TransactionStatus.DISPUTED    // 争议中
```

### NetworkType

```java
NetworkType.EVM         // eip155 - EVM 兼容链
NetworkType.SVM         // solana - Solana
NetworkType.TVM         // tvm - TON
NetworkType.AVM         // algorand - Algorand
NetworkType.STELLAR     // stellar - Stellar
```

---

## 常见错误

| 错误 | 原因 | 解决方案 |
|------|------|---------|
| `No scheme registered` | 未注册对应网络的 Scheme | 调用 `registerScheme()` 注册 |
| `Insufficient balance` | 钱包余额不足 | 充值 USDC |
| `Invalid signature` | 签名验证失败 | 检查私钥和签名数据 |
| `Payment verification failed` | Facilitator 验证失败 | 检查 nonce、有效期、金额 |
| `Settlement failed` | 链上结算失败 | 检查 Gas、网络状态 |
| `IdempotencyConflictException` | paymentId 被用于不同请求 | 每个请求使用新的 paymentId |
