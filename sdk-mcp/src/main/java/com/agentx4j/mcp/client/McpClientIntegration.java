package com.agentx4j.mcp.client;

import com.agentx4j.x402.client.X402Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * MCP Client + x402 集成。
 *
 * <p>让 MCP Client 调用付费工具时自动完成支付。</p>
 *
 * <p>工作原理：</p>
 * <ol>
 *   <li>封装标准 MCP Client</li>
 *   <li>拦截 tools/call 请求</li>
 *   <li>如果收到 402 响应 → 自动完成支付 → 重试请求</li>
 *   <li>业务代码完全无感知</li>
 * </ol>
 *
 * <p>使用方式：</p>
 * <pre>{@code
 * // 创建 x402 客户端
 * X402Client x402Client = new X402Client();
 * x402Client.registerScheme("eip155:*", new ExactScheme());
 * x402Client.setWallet(privateKey, fromAddress);
 *
 * // 创建 MCP 付费客户端
 * McpPaidClient client = new McpPaidClient(mcpClient, x402Client);
 * McpToolResult result = client.callTool("get_weather", Map.of("city", "Beijing"));
 * // 支付自动完成，业务代码只关心结果
 * }</pre>
 */
public class McpClientIntegration {

    private static final Logger log = LoggerFactory.getLogger(McpClientIntegration.class);

    private final Object mcpClient;  // 标准 MCP Client（可以是任何 MCP 客户端实现）
    private final X402Client x402Client;

    /**
     * 创建 MCP 付费客户端。
     *
     * @param mcpClient  标准 MCP Client 对象
     * @param x402Client x402 客户端
     */
    public McpClientIntegration(Object mcpClient, X402Client x402Client) {
        this.mcpClient = mcpClient;
        this.x402Client = x402Client;
    }

    /**
     * 调用付费 MCP 工具（自动处理 402 + 支付）。
     *
     * @param toolName 工具名称
     * @param args     工具参数
     * @return 工具执行结果
     */
    public McpToolResult callTool(String toolName, Map<String, Object> args) {
        log.debug("Calling paid MCP tool: {}", toolName);

        try {
            // 1. 发送 MCP tools/call（通过 HTTP）
            // 这里使用 X402Client 发送请求，自动处理 402
            String url = buildToolUrl(toolName);
            String requestBody = buildToolCallBody(toolName, args);

            X402Client.X402Response response = x402Client.post(url, requestBody);

            // 2. 解析结果
            return McpToolResult.builder()
                    .toolName(toolName)
                    .success(response.isSuccess())
                    .content(response.getBody())
                    .paymentProcessed(response.isPaymentProcessed())
                    .build();

        } catch (Exception e) {
            log.error("Failed to call MCP tool: {}", toolName, e);
            return McpToolResult.builder()
                    .toolName(toolName)
                    .success(false)
                    .error(e.getMessage())
                    .build();
        }
    }

    /**
     * 批量调用（并行处理）。
     */
    public List<McpToolResult> callToolsBatch(List<ToolCall> calls) {
        return calls.stream()
                .map(call -> callTool(call.getToolName(), call.getArgs()))
                .toList();
    }

    /**
     * 获取底层的 X402Client。
     */
    public X402Client getX402Client() {
        return x402Client;
    }

    // ==================== 内部方法 ====================

    private String buildToolUrl(String toolName) {
        // 构建 MCP 工具调用 URL
        // 实际实现取决于 MCP Server 的端点配置
        return "/tools/call";
    }

    private String buildToolCallBody(String toolName, Map<String, Object> args) {
        // 构建 JSON-RPC 2.0 请求体
        // 实际实现需要遵循 MCP 协议规范
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(Map.of(
                    "jsonrpc", "2.0",
                    "method", "tools/call",
                    "params", Map.of("name", toolName, "arguments", args),
                    "id", java.util.UUID.randomUUID().toString()
            ));
        } catch (Exception e) {
            throw new RuntimeException("Failed to build tool call body", e);
        }
    }

    // ==================== 内部数据结构 ====================

    /**
     * 工具调用请求。
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ToolCall {
        private String toolName;
        private Map<String, Object> args;
    }

    /**
     * 工具执行结果。
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class McpToolResult {
        private String toolName;
        private boolean success;
        private String content;
        private String error;
        private boolean paymentProcessed;
    }
}
