# AgentX-4J

> Agent Economy SDK for Java — 基于 x402 协议的 Agent 交易结算 SDK

[![Java](https://img.shields.io/badge/Java-17+-orange)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2+-green)]()
[![License](https://img.shields.io/badge/License-Apache%202.0-blue)]()

## 项目简介

AgentX-4J 是一个 Java SDK，为 AI Agent 提供标准化的交易结算能力。基于 [x402 支付协议](https://x402.org)，让 Agent 能够：

- **作为卖家**：暴露 API，声明价格，自动收取加密货币费用
- **作为买家**：调用其他 Agent 的服务时自动完成支付
- **作为平台方**：提供完整的 Agent 经济基础设施

## 特性

- 🔒 **x402 协议兼容** — 基于开放标准，与其他 x402 实现互操作
- ☕ **Java 原生** — Java 17+，Spring Boot 3.x 深度集成
- 🔌 **可插拔架构** — 支付网络、存储、规则引擎均可替换
- 🛡️ **幂等性保障** — 防止网络重试导致重复扣费
- ⚡ **声明式计费** — 一个注解 `@X402Protected` 即可收费
- 🔐 **EIP-3009 签名** — 支持链下签名授权，无需 approve
- 🤖 **MCP 集成** — Agent 间调用自动结算
- 🛡️ **风控引擎** — 限流 + 欺诈检测 + 规则引擎
- 💰 **结算引擎** — 单笔/批量/净额/退款

## 模块结构

```
AgentX-4J/
├── sdk-core/                  # 核心领域模型（枚举、实体、异常）
├── sdk-wallet/                # 钱包管理（EVM 钱包、签名）
├── sdk-x402/                  # x402 协议实现（ExactScheme + UptoScheme）
├── sdk-idempotency/           # 幂等性保障（内存 + Redis）
├── sdk-mcp/                   # MCP + x402 集成
├── sdk-risk/                  # 风控引擎（限流 + 欺诈检测 + 规则）
├── sdk-settlement/            # 结算引擎（单笔/批量/净额/退款）
├── sdk-storage/               # 持久化层（Repository + 内存实现）
├── sdk-spring-boot-starter/   # Spring Boot 自动装配
├── agentx-demo/               # 全栈示例项目
└── docs/                      # 文档
```

## 快速开始

### 1. 构建项目

```bash
mvn clean install -DskipTests
```

### 2. 添加依赖

```xml
<dependency>
    <groupId>io.github.guli-hhh</groupId>
    <artifactId>sdk-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 3. 启用并配置

```java
@SpringBootApplication
@EnableX402
public class MyAgentApp {
    public static void main(String[] args) {
        SpringApplication.run(MyAgentApp.class, args);
    }
}
```

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

### 4. 声明收费 API

```java
@RestController
public class MyAgentController {

    // 固定价格
    @X402Protected(price = "$0.001")
    @GetMapping("/api/weather")
    public WeatherData getWeather(@RequestParam String city) {
        return weatherService.fetch(city);
    }

    // 按用量（LLM 推理等）
    @X402Protected(price = "$0.05", scheme = BillingScheme.UPTO)
    @PostMapping("/api/generate")
    public GeneratedText generate(@RequestBody GenerateRequest req) {
        GeneratedText result = llmService.generate(req.getPrompt());
        // 根据实际用量设置结算金额
        SettlementOverride.setDollar(calculateCost(result.getTokenCount()));
        return result;
    }
}
```

### 5. 运行

```bash
WALLET_PRIVATE_KEY=0x... java -jar your-app.jar
```

## 跨语言支持

**AgentX-4J 是 Java SDK，但所有语言都能通过 x402 协议互操作！**

| 你的语言 | 如何接入 |
|---------|---------|
| **Java** | 直接引入 `sdk-spring-boot-starter` |
| **Python** | 使用 x402 官方 Python SDK（`pip install x402`） |
| **Go** | 使用 x402 官方 Go SDK |
| **Node.js** | 使用 x402 官方 TypeScript SDK |
| **其他** | 遵循 x402 协议规范，手动实现 4 步支付流程 |

> 📖 [跨语言使用指南](docs/cross-language-guide.md) — 详细说明 + 各语言示例代码

## 文档

| 文档 | 说明 |
|------|------|
| [架构设计](docs/architecture.md) | 系统架构、协议栈、数据流、扩展点 |
| [开发指南](docs/development-guide.md) | 快速开始、API 参考、FAQ |
| [快速参考](docs/quick-reference.md) | 配置、注解、API 速查 |
| [跨语言指南](docs/cross-language-guide.md) | Python/Go/Node.js 如何接入 |
| [数据库表结构](docs/sql-schema.md) | MySQL 建表脚本 + ER 图 |
| [设计文档](../agent-sdk-design.md) | 完整设计方案（2200+ 行） |
| [开发进度](docs/progress.md) | 当前进度和计划 |

## 功能矩阵

| 功能 | 状态 | 说明 |
|------|------|------|
| ExactScheme（固定价格） | ✅ | 完整的创建/验证/结算/签名恢复 |
| UptoScheme（按用量） | ✅ | 授权上限 + 实际用量结算 |
| EVM 钱包管理 | ✅ | 创建/导入/签名/余额查询 |
| EIP-3009 签名 | ✅ | 链下签名授权 |
| EIP-712 类型化签名 | ✅ | 标准类型化数据签名 |
| Spring Boot 自动装配 | ✅ | @EnableX402 + 配置属性 |
| 声明式计费 | ✅ | @X402Protected 注解 |
| MCP 集成 | 🟡 | Server + Client 集成完成 |
| 幂等性保障 | ✅ | 内存 + Redis 实现 |
| 限流 | ✅ | Token Bucket 算法 |
| 欺诈检测 | ✅ | 速度/金额/循环/新 Agent |
| 规则引擎 | ✅ | 优先级 + 条件评估 |
| 结算引擎 | 🟡 | 接口 + 默认实现 |
| 对账服务 | 🟡 | 接口定义 |
| 持久化层 | 🟡 | Repository + 内存实现 |
| BatchSettlement | ⬜ | 待实现 |
| Solana 支持 | ⬜ | 待实现 |
| Bazaar 发现 | ⬜ | 待实现 |

## 协议参考

- [x402 协议](https://x402.org) — 互联网原生支付协议
- [MCP 协议](https://modelcontextprotocol.io) — 模型上下文协议
- [EIP-3009](https://eips.ethereum.org/EIPS/eip-3009) — 代币授权标准
- [web3j](https://docs.web3j.io) — Java 以太坊库

## License

Apache 2.0
