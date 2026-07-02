package com.agentx4j.mcp.server;

import com.agentx4j.core.model.PaymentRequirement;
import com.agentx4j.mcp.common.McpToolPrice;
import com.agentx4j.x402.server.VerifyResult;
import com.agentx4j.x402.server.SettleResult;
import com.agentx4j.x402.server.X402ResourceServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP Server + x402 集成。
 *
 * <p>让任何 MCP Server 能声明工具价格，调用方自动付费。</p>
 *
 * <p>工作原理：</p>
 * <ol>
 *   <li>开发者用 @McpTool 注解标注方法，声明价格</li>
 *   <li>SDK 在 MCP Server 启动时扫描所有 @McpTool 方法</li>
 *   <li>当 MCP Client 调用工具时，自动返回 402 + PaymentRequirements</li>
 *   <li>验证通过后执行工具逻辑</li>
 *   <li>结算并返回结果</li>
 * </ol>
 */
public class McpServerIntegration {

    private static final Logger log = LoggerFactory.getLogger(McpServerIntegration.class);

    private final X402ResourceServer resourceServer;
    private final Map<String, McpToolPrice> toolPricing = new ConcurrentHashMap<>();
    private final Map<String, Method> toolMethods = new ConcurrentHashMap<>();
    private final Map<String, Object> toolTargets = new ConcurrentHashMap<>();

    public McpServerIntegration(X402ResourceServer resourceServer) {
        this.resourceServer = resourceServer;
    }

    /**
     * 注册带计费的 MCP 工具。
     *
     * @param toolName 工具名称
     * @param price    定价信息
     * @param method   工具方法
     * @param target   工具方法所属对象
     */
    public void registerTool(String toolName, McpToolPrice price, Method method, Object target) {
        toolPricing.put(toolName, price);
        if (method != null) {
            toolMethods.put(toolName, method);
        }
        if (target != null) {
            toolTargets.put(toolName, target);
        }
        log.info("Registered paid MCP tool: {} (price={})", toolName, price.getPrice());
    }

    /**
     * 扫描并注册对象中所有 @McpTool 标注的方法。
     *
     * @param target 包含 @McpTool 方法的对象
     */
    public void scanTools(Object target) {
        for (Method method : target.getClass().getDeclaredMethods()) {
            com.agentx4j.mcp.common.McpTool annotation = method.getAnnotation(com.agentx4j.mcp.common.McpTool.class);
            if (annotation != null) {
                String toolName = annotation.name().isEmpty() ? method.getName() : annotation.name();
                McpToolPrice price = McpToolPrice.builder()
                        .toolName(toolName)
                        .description(annotation.description())
                        .price(annotation.price())
                        .network(annotation.network())
                        .asset(annotation.asset())
                        .payTo(annotation.payTo())
                        .scheme(annotation.scheme())
                        .maxTimeoutSeconds(annotation.maxTimeoutSeconds())
                        .transport(annotation.transport())
                        .build();
                registerTool(toolName, price, method, target);
            }
        }
    }

    /**
     * 获取工具的支付要求。
     *
     * @param toolName 工具名称
     * @return 支付要求列表，如果工具不存在或免费则返回空列表
     */
    public List<PaymentRequirement> getToolPaymentRequirements(String toolName) {
        McpToolPrice price = toolPricing.get(toolName);
        if (price == null || price.getPrice().equals("$0") || price.getPrice().equals("FREE")) {
            return Collections.emptyList();
        }

        PaymentRequirement requirement = PaymentRequirement.builder()
                .scheme(price.getScheme().getSchemeName())
                .network(price.getNetwork())
                .amount(price.getAtomicAmount(6)) // USDC 6 位小数
                .asset(price.getAsset())
                .payTo(price.getPayTo())
                .maxTimeoutSeconds(price.getMaxTimeoutSeconds())
                .build();

        return Collections.singletonList(requirement);
    }

    /**
     * 验证 MCP 工具调用支付。
     *
     * @param toolName  工具名称
     * @param payload   签名后的支付载荷
     * @param requirement 支付要求
     * @return 验证结果
     */
    public VerifyResult verifyToolPayment(String toolName, com.agentx4j.core.model.PaymentPayload payload,
                                           PaymentRequirement requirement) {
        if (!toolPricing.containsKey(toolName)) {
            return VerifyResult.invalid("Unknown tool: " + toolName);
        }
        return resourceServer.verifyPayment(payload, requirement);
    }

    /**
     * 结算 MCP 工具调用支付。
     *
     * @param toolName  工具名称
     * @param payload   签名后的支付载荷
     * @param requirement 支付要求
     * @return 结算结果
     */
    public SettleResult settleToolPayment(String toolName, com.agentx4j.core.model.PaymentPayload payload,
                                           PaymentRequirement requirement) {
        if (!toolPricing.containsKey(toolName)) {
            return SettleResult.failed("Unknown tool: " + toolName);
        }
        return resourceServer.settlePayment(payload, requirement);
    }

    /**
     * 获取工具价格信息。
     */
    public McpToolPrice getToolPrice(String toolName) {
        return toolPricing.get(toolName);
    }

    /**
     * 获取所有已注册工具名称。
     */
    public Set<String> getRegisteredTools() {
        return Collections.unmodifiableSet(toolPricing.keySet());
    }

    /**
     * 检查工具是否需要付费。
     */
    public boolean isPaidTool(String toolName) {
        McpToolPrice price = toolPricing.get(toolName);
        return price != null && !price.getPrice().equals("$0") && !price.getPrice().equals("FREE");
    }
}
