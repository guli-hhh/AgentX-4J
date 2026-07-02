package com.agentx4j.mcp.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP 工具 Schema 定义。
 *
 * <p>用于 Bazaar 发现层，描述工具的输入/输出格式和定价信息。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpToolSchema {

    /** 工具名称 */
    private String toolName;

    /** 工具描述 */
    private String description;

    /** 输入参数 Schema（JSON Schema 格式） */
    private String inputSchema;

    /** 输出结果 Schema（JSON Schema 格式） */
    private String outputSchema;

    /** 调用示例 */
    private String example;

    /** 价格 */
    private String price;

    /** 网络 */
    private String network;

    /** MCP transport 类型 */
    private String transport;
}
