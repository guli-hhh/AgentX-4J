package com.agentx4j.starter;

import com.agentx4j.x402.facilitator.FacilitatorClient;
import com.agentx4j.x402.facilitator.HttpFacilitatorClient;
import com.agentx4j.x402.server.X402ResourceServer;
import com.agentx4j.x402.client.X402Client;
import com.agentx4j.x402.scheme.ExactScheme;
import com.agentx4j.x402.scheme.UptoScheme;
import com.agentx4j.x402.scheme.BatchSettlementScheme;
import com.agentx4j.x402.network.EvmNetworkAdapter;
import com.agentx4j.x402.network.SvmNetworkAdapter;
import com.agentx4j.mcp.server.McpServerIntegration;
import com.agentx4j.risk.FraudDetectionService;
import com.agentx4j.risk.RateLimitService;
import com.agentx4j.risk.RiskControlEngine;
import com.agentx4j.settlement.DefaultSettlementEngine;
import com.agentx4j.settlement.DefaultNettingService;
import com.agentx4j.settlement.SettlementEngine;
import com.agentx4j.settlement.NettingService;
import com.agentx4j.bazaar.BazaarClient;
import com.agentx4j.settlement.DefaultReconciliationService;
import com.agentx4j.settlement.ReconciliationService;
import com.agentx4j.settlement.SettlementScheduler;
import com.agentx4j.starter.aspect.X402ProtectedAspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * AgentX-4J Spring Boot 自动配置。
 *
 * <p>当 classpath 中存在相关类且配置了 {@code agent.x402.enabled=true} 时自动装配。</p>
 *
 * <p>自动创建的 Bean：</p>
 * <ul>
 *   <li>{@link FacilitatorClient} — Facilitator 客户端</li>
 *   <li>{@link X402ResourceServer} — 服务端支付处理</li>
 *   <li>{@link X402Client} — 客户端自动支付</li>
 *   <li>{@link McpServerIntegration} — MCP Server 计费集成</li>
 *   <li>{@link RiskControlEngine} — 风控引擎</li>
 *   <li>{@link SettlementEngine} — 结算引擎</li>
 * </ul>
 */
@Configuration
@EnableAspectJAutoProxy
@ConditionalOnClass({X402ResourceServer.class, X402Client.class})
@ConditionalOnProperty(prefix = "agent.x402", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AgentX4JProperties.class)
public class AgentX4JAutoConfiguration {

    /**
     * 创建 FacilitatorClient Bean。
     */
    @Bean
    @ConditionalOnMissingBean
    public FacilitatorClient facilitatorClient(AgentX4JProperties properties) {
        return new HttpFacilitatorClient(properties.getFacilitatorUrl());
    }

    /**
     * 创建 X402ResourceServer Bean（卖家侧）。
     * 自动注册 ExactScheme 支持所有 EVM 网络。
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "agent.x402", name = "pay-to")
    public X402ResourceServer x402ResourceServer(FacilitatorClient facilitatorClient,
                                                   AgentX4JProperties properties) {
        X402ResourceServer server = new X402ResourceServer(facilitatorClient);
        // 注册 exact、upto 和 batch-settlement 方案
        server.registerScheme("eip155:*", new ExactScheme());
        server.registerScheme("eip155:*", new UptoScheme());
        server.registerScheme("eip155:*", new BatchSettlementScheme());
        // 注册 EVM 网络适配器
        String rpcUrl = properties.getRpcUrls() != null
                ? properties.getRpcUrls().getOrDefault("eip155:84532", "https://sepolia.base.org")
                : "https://sepolia.base.org";
        server.registerNetworkAdapter("eip155:*", new EvmNetworkAdapter(rpcUrl, "84532"));
        // 注册 Solana 网络适配器
        String solanaRpcUrl = properties.getRpcUrls() != null
                ? properties.getRpcUrls().getOrDefault("solana:EtWTRABZaYq6iMfeYKouRu166VU2xqa1wcaWoxPkrZBG",
                        "https://api.devnet.solana.com")
                : "https://api.devnet.solana.com";
        server.registerNetworkAdapter("solana:*", new SvmNetworkAdapter(solanaRpcUrl));
        return server;
    }

    /**
     * 创建 X402Client Bean（买家侧）。
     * 自动注册 ExactScheme 支持所有 EVM 网络。
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "agent.x402", name = "wallet-private-key")
    public X402Client x402Client(AgentX4JProperties properties) {
        X402Client client = new X402Client();
        // 注册 exact、upto 和 batch-settlement 方案
        client.registerScheme("eip155:*", new ExactScheme());
        client.registerScheme("eip155:*", new UptoScheme());
        client.registerScheme("eip155:*", new BatchSettlementScheme());
        // 设置钱包
        if (properties.getWalletPrivateKey() != null) {
            byte[] privateKeyBytes = hexStringToByteArray(properties.getWalletPrivateKey());
            client.setWallet(privateKeyBytes, properties.getPayTo());
        }
        return client;
    }

    /**
     * 创建 McpServerIntegration Bean（MCP Server 计费集成）。
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(X402ResourceServer.class)
    public McpServerIntegration mcpServerIntegration(X402ResourceServer resourceServer) {
        return new McpServerIntegration(resourceServer);
    }

    /**
     * 创建 RiskControlEngine Bean（风控引擎）。
     */
    @Bean
    @ConditionalOnMissingBean
    public RiskControlEngine riskControlEngine() {
        FraudDetectionService fraudDetection = new FraudDetectionService();
        RateLimitService rateLimit = new RateLimitService(100, 10); // 桶容量 100，每秒补充 10
        return new RiskControlEngine(fraudDetection, rateLimit);
    }

    /**
     * 创建 SettlementEngine Bean（结算引擎）。
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(FacilitatorClient.class)
    public SettlementEngine settlementEngine(FacilitatorClient facilitatorClient) {
        return new DefaultSettlementEngine(facilitatorClient);
    }

    /**
     * 创建 NettingService Bean（净额结算服务）。
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(FacilitatorClient.class)
    public NettingService nettingService(FacilitatorClient facilitatorClient) {
        return new DefaultNettingService(facilitatorClient, null);
    }

    /**
     * 创建 BazaarClient Bean（服务发现）。
     */
    @Bean
    @ConditionalOnMissingBean
    public BazaarClient bazaarClient(AgentX4JProperties properties) {
        return new BazaarClient(properties.getFacilitatorUrl());
    }

    /**
     * 创建 ReconciliationService Bean（对账服务）。
     */
    @Bean
    @ConditionalOnMissingBean
    public ReconciliationService reconciliationService() {
        return new DefaultReconciliationService();
    }

    /**
     * 创建 SettlementScheduler Bean（日终结算定时任务）。
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "agent.x402.settlement", name = "scheduler-enabled", havingValue = "true")
    public SettlementScheduler settlementScheduler(SettlementEngine settlementEngine,
                                                     ReconciliationService reconciliationService) {
        return new SettlementScheduler(settlementEngine, reconciliationService);
    }

    private static byte[] hexStringToByteArray(String hex) {
        if (hex.startsWith("0x") || hex.startsWith("0X")) {
            hex = hex.substring(2);
        }
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
