package com.agentx4j.demo;

import com.agentx4j.core.enums.BillingScheme;
import com.agentx4j.mcp.common.McpTool;
import com.agentx4j.mcp.common.McpToolPrice;
import com.agentx4j.mcp.server.McpServerIntegration;
import com.agentx4j.x402.scheme.UptoScheme;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * MCP Agent 示例控制器。
 *
 * <p>演示 MCP + x402 集成：</p>
 * <ul>
 *   <li>通过 @McpTool 注解声明 MCP 工具价格</li>
 *   <li>自动处理支付验证和结算</li>
 *   <li>支持 UptoScheme 按用量计费</li>
 * </ul>
 *
 * <p>使用方式：</p>
 * <pre>{@code
 * // MCP Client 调用（自动处理支付）
 * POST /mcp/tools/call
 * {
 *   "name": "get_weather",
 *   "arguments": { "city": "Beijing" }
 * }
 *
 * // 第一次调用 → 返回 402 + PaymentRequirements
 * // 客户端签名后重试 → 返回 200 + 结果
 * }</pre>
 */
@RestController
@RequestMapping("/mcp")
public class McpAgentController {

    @Autowired(required = false)
    private McpServerIntegration mcpServerIntegration;

    @Value("${agent.x402.pay-to:0xSellerAddress}")
    private String payTo;

    @Value("${agent.x402.asset:0x036CbD53842c5426634e7929541eC2318f3dCF7e}")
    private String asset;

    /**
     * 列出所有可用的 MCP 工具。
     *
     * <p>GET /mcp/tools/list</p>
     */
    @GetMapping("/tools/list")
    public Map<String, Object> listTools() {
        // 返回工具列表（实际应从 McpServerIntegration 获取）
        return Map.of(
                "tools", List.of(
                        Map.of(
                                "name", "get_weather",
                                "description", "Get current weather for a city",
                                "inputSchema", Map.of(
                                        "type", "object",
                                        "properties", Map.of("city", Map.of("type", "string")),
                                        "required", List.of("city")
                                ),
                                "pricing", Map.of("price", "$0.001", "scheme", "exact")
                        ),
                        Map.of(
                                "name", "generate_text",
                                "description", "AI text generation (billed by token)",
                                "inputSchema", Map.of(
                                        "type", "object",
                                        "properties", Map.of("prompt", Map.of("type", "string")),
                                        "required", List.of("prompt")
                                ),
                                "pricing", Map.of("price", "$0.05", "scheme", "upto")
                        ),
                        Map.of(
                                "name", "translate",
                                "description", "Translate text between languages",
                                "inputSchema", Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "text", Map.of("type", "string"),
                                                "from", Map.of("type", "string"),
                                                "to", Map.of("type", "string")
                                        ),
                                        "required", List.of("text")
                                ),
                                "pricing", Map.of("price", "$0.005", "scheme", "exact")
                        )
                )
        );
    }

    /**
     * 调用 MCP 工具（自动处理支付）。
     *
     * <p>POST /mcp/tools/call</p>
     *
     * <p>请求体：</p>
     * <pre>{@code
     * {
     *   "name": "get_weather",
     *   "arguments": { "city": "Beijing" }
     * }
     * }</pre>
     *
     * <p>如果未支付，返回 402 + PaymentRequirements。</p>
     * <p>如果已支付（PAYMENT-SIGNATURE header），执行工具并返回结果。</p>
     */
    @PostMapping("/tools/call")
    public Map<String, Object> callTool(@RequestBody Map<String, Object> request,
                                         @RequestHeader(value = "PAYMENT-SIGNATURE", required = false) String paymentSignature) {

        String toolName = (String) request.get("Name");
        @SuppressWarnings("unchecked")
        Map<String, Object> args = (Map<String, Object>) request.getOrDefault("arguments", Map.of());

        // 获取工具定价
        Map<String, Object> toolPricing = lookupToolPricing(toolName);
        if (toolPricing == null) {
            return Map.of("error", "Unknown tool: " + toolName);
        }

        // 如果没有支付签名 → 返回 402
        if (paymentSignature == null || paymentSignature.isEmpty()) {
            return createPaymentRequiredResponse(toolName, toolPricing);
        }

        // 有支付签名 → 执行工具（实际应验证签名）
        return executeTool(toolName, args, toolPricing);
    }

    /**
     * 获取工具定价信息。
     */
    @GetMapping("/tools/{toolName}/pricing")
    public Map<String, Object> getToolPricing(@PathVariable String toolName) {
        Map<String, Object> pricing = getToolPricing(toolName);
        if (pricing == null) {
            return Map.of("error", "Unknown tool: " + toolName);
        }
        return pricing;
    }

    // ==================== 内部方法 ====================

    private Map<String, Object> lookupToolPricing(String toolName) {
        return switch (toolName) {
            case "get_weather" -> Map.of(
                    "toolName", "get_weather",
                    "price", "$0.001",
                    "scheme", "exact",
                    "network", "eip155:84532",
                    "asset", asset,
                    "payTo", payTo
            );
            case "generate_text" -> Map.of(
                    "toolName", "generate_text",
                    "price", "$0.05",
                    "scheme", "upto",
                    "network", "eip155:84532",
                    "asset", asset,
                    "payTo", payTo,
                    "description", "按实际 token 用量计费，上限 $0.05"
            );
            case "translate" -> Map.of(
                    "toolName", "translate",
                    "price", "$0.005",
                    "scheme", "exact",
                    "network", "eip155:84532",
                    "asset", asset,
                    "payTo", payTo
            );
            default -> null;
        };
    }

    private Map<String, Object> createPaymentRequiredResponse(String toolName, Map<String, Object> pricing) {
        // 构建 PaymentRequirement
        Map<String, Object> requirement = new LinkedHashMap<>();
        requirement.put("scheme", pricing.get("scheme"));
        requirement.put("network", pricing.get("network"));
        requirement.put("amount", pricing.get("amount") != null ? pricing.get("amount") : "1000");
        requirement.put("asset", pricing.get("asset"));
        requirement.put("payTo", pricing.get("payTo"));
        requirement.put("maxTimeoutSeconds", 60);
        requirement.put("extra", Map.of("name", "USD Coin", "version", "2"));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("x402Version", 2);
        body.put("accepts", List.of(requirement));
        body.put("resource", "mcp://tool/" + toolName);
        body.put("error", "Payment required");

        return body;
    }

    private Map<String, Object> executeTool(String toolName, Map<String, Object> args,
                                             Map<String, Object> pricing) {
        return switch (toolName) {
            case "get_weather" -> {
                String city = (String) args.getOrDefault("city", "Unknown");
                // 模拟 UptoScheme 按用量计费
                if ("upto".equals(pricing.get("scheme"))) {
                    UptoScheme.setSettlementOverride("0.001");
                }
                yield Map.of(
                        "content", List.of(Map.of(
                                "type", "text",
                                "text", String.format("{\"city\":\"%s\",\"weather\":\"sunny\",\"temperature\":25}", city)
                        )),
                        "payment", Map.of("settled", true, "amount", "0.001")
                );
            }
            case "generate_text" -> {
                String prompt = (String) args.getOrDefault("prompt", "");
                // 模拟按 token 用量计费
                int tokensUsed = prompt.length() * 2; // 简化计算
                BigDecimal cost = new BigDecimal(tokensUsed).multiply(new BigDecimal("0.00001"));
                UptoScheme.setSettlementOverride(cost.toPlainString());
                yield Map.of(
                        "content", List.of(Map.of(
                                "type", "text",
                                "text", String.format("{\"result\":\"AI response for: %s\",\"tokens\":%d}", prompt, tokensUsed)
                        )),
                        "payment", Map.of("settled", true, "amount", cost.toPlainString(), "tokens", tokensUsed)
                );
            }
            case "translate" -> {
                String text = (String) args.getOrDefault("text", "");
                String from = (String) args.getOrDefault("from", "zh");
                String to = (String) args.getOrDefault("to", "en");
                yield Map.of(
                        "content", List.of(Map.of(
                                "type", "text",
                                "text", String.format("{\"original\":\"%s\",\"translated\":\"Translation of: %s\",\"from\":\"%s\",\"to\":\"%s\"}",
                                        text, text, from, to)
                        )),
                        "payment", Map.of("settled", true, "amount", "0.005")
                );
            }
            default -> Map.of("error", "Unknown tool: " + toolName);
        };
    }
}
