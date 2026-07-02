package com.agentx4j.mcp.common;

import com.agentx4j.core.enums.BillingScheme;

import java.lang.annotation.*;

/**
 * MCP 工具计费声明注解。
 *
 * <p>标注在 MCP 工具方法上，SDK 自动处理计费。</p>
 *
 * <p>使用方式：</p>
 * <pre>{@code
 * @McpTool(
 *     name = "get_weather",
 *     description = "Get current weather for a city",
 *     price = "$0.001",
 *     network = "eip155:84532"
 * )
 * public WeatherData getWeather(@Param("city") String city) {
 *     return weatherService.fetch(city);
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface McpTool {

    /** 工具名称（默认取方法名） */
    String name() default "";

    /** 工具描述 */
    String description() default "";

    /** 价格（e.g., "$0.001"） */
    String price();

    /** 网络（CAIP-2, 如 "eip155:8453"） */
    String network() default "eip155:8453";

    /** 代币合约地址（可选，默认使用全局配置） */
    String asset() default "";

    /** 收款地址（默认取 Agent 主地址） */
    String payTo() default "";

    /** 支付方案 */
    BillingScheme scheme() default BillingScheme.EXACT;

    /** 超时时间（秒） */
    long maxTimeoutSeconds() default 60;

    /** MCP transport 类型 */
    String transport() default "streamable-http";
}
