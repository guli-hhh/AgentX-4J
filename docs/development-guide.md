# AgentX-4J 开发指南

> 本文档帮助开发者快速上手 AgentX-4J SDK 开发。
> 对应设计文档：`agent-sdk-design.md` 第一至第九章。

---

## 一、环境要求

- **Java**: 17+
- **Maven**: 3.8+
- **Spring Boot**: 3.2+（使用 starter 时需要）

---

## 二、快速开始

### 2.1 构建项目

```bash
cd AgentX-4J
mvn clean install -DskipTests
```

### 2.2 创建你的第一个收费 Agent

#### Step 1: 创建 Spring Boot 项目

```xml
<!-- pom.xml -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.5</version>
</parent>

<dependencies>
    <dependency>
        <groupId>com.agentx4j</groupId>
        <artifactId>sdk-spring-boot-starter</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
</dependencies>
```

#### Step 2: 启用 AgentX-4J

```java
@SpringBootApplication
@EnableX402
public class MyAgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyAgentApplication.class, args);
    }
}
```

#### Step 3: 配置

```yaml
# application.yml
agent:
  x402:
    enabled: true
    facilitator-url: https://x402.org/facilitator
    pay-to: "0xYourReceiverAddress"
    network: eip155:84532
    default-price: "$0.001"
```

#### Step 4: 编写收费 API

```java
@RestController
@RequestMapping("/api")
public class MyAgentController {

    @X402Protected(price = "$0.001")
    @GetMapping("/weather")
    public Map<String, Object> getWeather(@RequestParam String city) {
        // 如果执行到这里，说明支付已经验证通过
        return Map.of("city", city, "weather", "sunny", "temperature", 25);
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP");
    }
}
```

#### Step 5: 运行

```bash
mvn spring-boot:run
```

---

## 三、核心概念

### 3.1 支付流程

```
客户端请求 → 402 响应 → 客户端签名 → 重试验证 → 执行业务 → 结算 → 返回结果
```

### 3.2 计费方案

| 方案 | 说明 | 适用场景 |
|------|------|---------|
| `exact` | 固定价格，调用前明确知道费用 | 标准化服务（翻译/天气/搜索） |
| `upto` | 授权上限，按实际用量结算 | LLM 推理、计算服务 |
| `batch-settlement` | 一次存款，链下凭证，批量结算 | 高频微支付 |

### 3.3 网络类型

| 网络 | CAIP-2 | 代币 | 支持阶段 |
|------|--------|------|---------|
| Base Sepolia | `eip155:84532` | USDC | Phase 1 |
| Base Mainnet | `eip155:8453` | USDC | Phase 1 |
| Ethereum | `eip155:1` | USDC | Phase 2 |
| Solana | `solana:5eyk...` | USDC | Phase 3 |

---

## 四、API 参考

### 4.1 注解

#### @EnableX402

启用 AgentX-4J 自动配置。标注在 Spring Boot 主类上。

```java
@SpringBootApplication
@EnableX402
public class MyApp { }
```

#### @X402Protected

声明接口需要支付才能访问。

```java
// 最简单的用法
@X402Protected(price = "$0.001")
@GetMapping("/api/data")
public Data getData() { }

// 完整配置
@X402Protected(
    price = "$0.005",
    network = "eip155:84532",
    asset = "0x036CbD53842c5426634e7929541eC2318f3dCF7e",
    payTo = "0xReceiverAddress",
    scheme = BillingScheme.EXACT,
    maxTimeoutSeconds = 60
)
@PostMapping("/api/generate")
public Result generate(@RequestBody Request req) { }
```

#### @AutoSettle

自动结算标注。

```java
@AutoSettle
public void handleSettlement(String transactionId) { }
```

### 4.2 核心类

#### X402ResourceServer（卖家侧）

```java
@Autowired
private X402ResourceServer resourceServer;

// 注册自定义 Scheme
resourceServer.registerScheme("eip155:*", new MyScheme());

// 手动验证支付
VerifyResult result = resourceServer.verifyPayment(payload, requirement);

// 手动结算
SettleResult settleResult = resourceServer.settlePayment(payload, requirement);
```

#### X402Client（买家侧）

```java
@Autowired
private X402Client x402Client;

// 选择支付方案
PaymentRequirement req = x402Client.selectPaymentRequirement(accepts);

// 创建签名支付
PaymentPayload payload = x402Client.createPayment(req);
```

#### EvmWallet（钱包）

```java
// 创建新钱包
EvmWallet wallet = EvmWallet.create();
String address = wallet.getAddress();

// 从私钥导入
EvmWallet wallet = EvmWallet.fromPrivateKey("0x...");

// 获取签名器
WalletSigner signer = wallet.getSigner();

// 签名 EIP-3009 授权
String signature = signer.signTransferWithAuthorization(
    from, to, value, nonce, validAfter, validBefore
);
```

#### Idempotency（幂等性）

```java
// 生成幂等 Key
String paymentId = IdempotencyKeyGenerator.generate();
String orderId = IdempotencyKeyGenerator.generate("order_");

// 验证格式
boolean valid = IdempotencyKeyGenerator.isValid(paymentId);

// 存储
IdempotencyStore store = new InMemoryIdempotencyStore();
store.save(paymentId, fingerprint, result, Duration.ofHours(24));
IdempotencyEntry entry = store.get(paymentId);
```

---

## 五、测试

### 5.1 单元测试

```java
@ExtendWith(MockitoExtension.class)
class ExactSchemeTest {

    @Test
    void testSchemeName() {
        ExactScheme scheme = new ExactScheme();
        assertEquals("exact", scheme.getName());
    }

    @Test
    void testSupportsNetwork() {
        ExactScheme scheme = new ExactScheme();
        assertTrue(scheme.supportsNetwork("eip155:84532"));
        assertFalse(scheme.supportsNetwork("solana:mainnet"));
    }
}
```

### 5.2 集成测试

使用 Base Sepolia 测试网进行端到端测试：

1. 获取测试网 USDC（从水龙头领取）
2. 启动 Agent 服务
3. 发送请求 → 收到 402 → 签名支付 → 重试 → 收到结果

---

## 六、常见问题

### Q: 私钥如何安全存储？

**A**: 推荐方案：
1. **开发环境**: 环境变量注入 (`${WALLET_PRIVATE_KEY}`)
2. **生产环境**: 使用 KMS（AWS KMS / HashiCorp Vault）
3. **绝不**: 将私钥硬编码或提交到 Git

### Q: 支持哪些区块链？

**A**: 目前支持 EVM 兼容链（Base、Ethereum、Polygon、Arbitrum）。
Solana、TON、Stellar 等将在后续版本支持。

### Q: Facilitator 是什么？需要自己搭建吗？

**A**: Facilitator 是支付协调方，负责验证签名和执行链上结算。
可以使用公共 Facilitator（如 x402 官方），也可以自建。

### Q: Gas 费谁出？

**A**: Facilitator 提交链上交易时支付 Gas 费。
在 EIP-3009 方案下，用户只需链下签名，不需要支付 Gas。

### Q: 其他语言能调用 AgentX-4J 发布的服务吗？

**A**: 能！AgentX-4J 发布的服务是标准的 HTTP API，遵循 x402 协议。
其他语言只需要使用 x402 官方 SDK，或者手动实现 4 步支付流程即可。

### Q: 各语言的 x402 SDK 在哪里？

| 语言 | 包名 | 安装 |
|------|------|------|
| TypeScript | `@x402/fetch` | `npm install @x402/fetch @x402/evm` |
| Python | `x402` | `pip install x402` |
| Go | `x402/go/v2` | `go get github.com/x402-foundation/x402/go/v2` |
| Java | AgentX-4J | `mvn install` |
