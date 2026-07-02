# 跨语言使用指南

> 本文档说明：非 Java 的 Agent 服务如何与 AgentX-4J 发布的付费服务进行交互。

---

## 核心认知

**AgentX-4J 是 Java SDK，不是跨语言 SDK。**

```
AgentX-4J 的价值：
  ✅ Java 开发者用它发布付费 API
  ✅ Java 开发者用它消费付费 API
  ✅ 其他语言通过标准 x402 协议与这些 API 互操作

不是：
  ❌ 把 Java SDK 变成 HTTP 服务供其他语言调用
  ❌ 让 Python 项目 import Java 代码
```

**x402 本身就是跨语言的**——它基于 HTTP 402 状态码和标准 HTTP Header，任何能发 HTTP 请求的语言都能使用。

---

## 各语言接入方式

### 方式总览

| 你的语言 | 如何消费 AgentX-4J 发布的付费 API | 如何发布自己的付费 API |
|---------|----------------------------------|----------------------|
| **Java** | 引入 `sdk-spring-boot-starter`，注入 `X402Client` | 引入 SDK，用 `@X402Protected` 注解 |
| **Python** | 使用 x402 官方 Python SDK（`pip install x402`） | 使用 x402 官方 Python SDK |
| **Go** | 使用 x402 官方 Go SDK | 使用 x402 官方 Go SDK |
| **Node.js** | 使用 x402 官方 TypeScript SDK | 使用 x402 官方 TypeScript SDK |
| **其他** | 遵循 x402 协议规范，手动实现 4 步流程 | 遵循 x402 协议规范 |

### 为什么各语言独立实现，而不是"复用 Java SDK"？

| 原因 | 说明 |
|------|------|
| **SDK 本质** | SDK 应该是 import 到项目里的库，不是远程服务 |
| **协议简单** | x402 只有 4 步 HTTP 交互，各语言实现不难 |
| **生态惯例** | Stripe、AWS 等都是各语言独立实现 SDK |
| **已有官方实现** | x402 官方已提供 TypeScript/Python/Go SDK |
| **各有特色** | Python 有 async、Go 有 goroutine，各自优化 |

---

## Python 接入示例

### 消费 AgentX-4J 发布的付费 API

```python
# pip install x402
from x402 import x402Client
from x402.mechanisms.evm import EthAccountSigner
from x402.mechanisms.evm.exact.register import register_exact_evm_client
from eth_account import Account

account = Account.from_key("0xYourPrivateKey")
client = x402Client()
register_exact_evm_client(client, EthAccountSigner(account))

# 自动处理 402 → 支付 → 重试
response = client.get("http://java-agent.example.com/api/weather?city=Beijing")
print(response.json())
```

### 发布自己的付费 API（Python）

```python
from x402.server import x402ResourceServer
from x402.http import FacilitatorConfig, HTTPFacilitatorClient, PaymentOption
from x402.mechanisms.evm.exact import ExactEvmServerScheme

facilitator = HTTPFacilitatorClient(
    FacilitatorConfig(url="https://x402.org/facilitator")
)
server = x402ResourceServer(facilitator)
server.register("eip155:84532", ExactEvmServerScheme())

routes = {
    "GET /api/data": {
        "accepts": [PaymentOption(
            scheme="exact",
            pay_to="0xYourAddress",
            price="$0.001",
            network="eip155:84532",
        )],
    },
}
```

---

## Go 接入示例

```go
import (
    x402 "github.com/x402-foundation/x402/go/v2"
    x402http "github.com/x402-foundation/x402/go/v2/http"
    evm "github.com/x402-foundation/x402/go/v2/mechanisms/evm/exact/client"
    evmsigners "github.com/x402-foundation/x402/go/v2/signers/evm"
)

evmSigner, _ := evmsigners.NewClientSignerFromPrivateKey("0xYourPrivateKey")
client := x402.Newx402Client().
    Register("eip155:*", evm.NewExactEvmScheme(evmSigner))
httpClient := x402http.WrapHTTPClientWithPayment(
    http.DefaultClient,
    x402http.Newx402HTTPClient(client),
)
resp, _ := httpClient.Get("http://java-agent.example.com/api/weather?city=Beijing")
```

---

## Node.js 接入示例

```typescript
import { wrapFetchWithPayment, x402Client } from "@x402/fetch";
import { ExactEvmScheme } from "@x402/evm/exact/client";
import { privateKeyToAccount } from "viem/accounts";

const account = privateKeyToAccount("0xYourPrivateKey" as `0x${string}`);
const client = new x402Client();
client.register("eip155:*", new ExactEvmScheme(account));
const fetchWithPayment = wrapFetchWithPayment(fetch, client);

const response = await fetchWithPayment(
    "http://java-agent.example.com/api/weather?city=Beijing"
);
const data = await response.json();
```

---

## 任意语言手动实现（协议级）

如果你的语言没有 x402 SDK，可以手动实现 4 步流程：

### 步骤 1: 发送 HTTP 请求

```bash
curl "http://java-agent.example.com/api/weather?city=Beijing"
```

### 步骤 2: 收到 402 响应，解析支付要求

```http
HTTP/1.1 402 Payment Required
PAYMENT-REQUIRED: eyJ4NDAyVmVyc2lvbiI6MiwiYWNjZXB0cyI6...
Content-Type: application/json

{
  "x402Version": 2,
  "accepts": [{
    "scheme": "exact",
    "network": "eip155:84532",
    "amount": "1000",
    "asset": "0x036CbD53842c5426634e7929541eC2318f3dCF7e",
    "payTo": "0xReceiverAddress",
    "maxTimeoutSeconds": 60,
    "extra": { "name": "USD Coin", "version": "2" }
  }]
}
```

### 步骤 3: 签名支付（EIP-712）

```json
{
  "types": {
    "TransferWithAuthorization": [
      { "name": "from", "type": "address" },
      { "name": "to", "type": "address" },
      { "name": "value", "type": "uint256" },
      { "name": "validAfter", "type": "uint256" },
      { "name": "validBefore", "type": "uint256" },
      { "name": "nonce", "type": "bytes32" }
    ]
  },
  "domain": {
    "name": "USD Coin",
    "version": "2",
    "chainId": 84532,
    "verifyingContract": "0x036CbD53842c5426634e7929541eC2318f3dCF7e"
  },
  "message": {
    "from": "0xYourAddress",
    "to": "0xReceiverAddress",
    "value": "1000",
    "validAfter": 1719000000,
    "validBefore": 1719000300,
    "nonce": "0x...32字节随机数"
  }
}
```

### 步骤 4: 重试请求

```http
GET /api/weather?city=Beijing HTTP/1.1
PAYMENT-SIGNATURE: eyJzY2hlbWUiOiJleGFjdCIs...(Base64编码的签名支付载荷)
```

---

## x402 官方多语言 SDK

| 语言 | 包名 | 安装 | 状态 |
|------|------|------|------|
| **TypeScript** | `@x402/fetch`, `@x402/axios` | `npm install @x402/fetch @x402/evm` | ✅ 生产可用 |
| **Python** | `x402` | `pip install x402` | ✅ 生产可用 |
| **Go** | `x402/go/v2` | `go get github.com/x402-foundation/x402/go/v2` | ✅ 生产可用 |
| **Java** | AgentX-4J | `mvn install` | 🔄 开发中 |

> 非 Java 项目优先使用 x402 官方 SDK，AgentX-4J 是 Java 生态的增强实现。

---

## 总结

```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│  AgentX-4J 是 Java SDK，不是跨语言 SDK                      │
│                                                             │
│  Java 开发者 → 用 AgentX-4J 发布/消费付费 API               │
│  Python 开发者 → 用 x402 官方 Python SDK                    │
│  Go 开发者 → 用 x402 官方 Go SDK                            │
│  Node.js 开发者 → 用 x402 官方 TS SDK                       │
│                                                             │
│  它们通过 x402 标准 HTTP 协议互操作                         │
│  不需要"跨语言调用 Java SDK"                                │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```
