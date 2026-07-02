package com.agentx4j.starter;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AgentX-4J 配置属性。
 *
 * <p>在 application.yml 中通过 {@code agent.x402} 前缀配置。</p>
 *
 * <p>示例：</p>
 * <pre>{@code
 * agent:
 *   x402:
 *     enabled: true
 *     facilitator-url: https://x402.org/facilitator
 *     wallet-private-key: ${WALLET_PRIVATE_KEY}
 *     network: eip155:84532
 *     default-price: "$0.001"
 *     pay-to: "0xYourAddress"
 * }</pre>
 */
@Data
@ConfigurationProperties(prefix = "agent.x402")
public class AgentX4JProperties {

    /** 是否启用 AgentX-4J */
    private boolean enabled = true;

    /** Facilitator URL */
    private String facilitatorUrl = "https://x402.org/facilitator";

    /** 钱包私钥（建议使用环境变量） */
    private String walletPrivateKey;

    /** 默认网络 */
    private String network = "eip155:84532";

    /** 默认代币合约地址 */
    private String asset = "0x036CbD53842c5426634e7929541eC2318f3dCF7e";

    /** 默认价格（美元字符串，如 "$0.001"） */
    private String defaultPrice = "$0.001";

    /** 默认收款地址 */
    private String payTo;

    /** 默认超时时间（秒） */
    private long maxTimeoutSeconds = 60;

    /** RPC URL（按网络分组） */
    private java.util.Map<String, String> rpcUrls;
}
