package com.agentx4j.bazaar;

import com.agentx4j.core.model.PaymentRequirement;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * 服务列表项 — Bazaar 中的一项服务。
 *
 * <p>类比：ServiceListing ≈ 美团上的一个"店铺"，
 *       包含名称、描述、菜单（支付要求）、评分等。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceListing {

    /** 服务 ID */
    private String serviceId;

    /** 资源名称（URL 或 MCP tool 标识） */
    private String resourceName;

    /** 资源类型: "http" / "mcp" */
    private String resourceType;

    /** 服务名称 */
    private String serviceName;

    /** 服务描述 */
    private String description;

    /** MIME 类型 */
    private String mimeType;

    /** 标签 */
    private List<String> tags;

    /** 图标 URL */
    private String iconUrl;

    /** 支付选项（价目表） */
    private List<PaymentRequirement> accepts;

    /** 输入 schema（调用参数定义） */
    private JsonNode inputSchema;

    /** 输出 schema（返回结果定义） */
    private JsonNode outputSchema;

    /** 调用示例 */
    private JsonNode example;

    /** MCP transport 类型（sse/streamable-http） */
    private String transport;

    /** 服务价格（从 accepts 中提取，便于搜索排序） */
    private String price;

    /** 最后更新时间 */
    private Instant lastUpdated;
}
