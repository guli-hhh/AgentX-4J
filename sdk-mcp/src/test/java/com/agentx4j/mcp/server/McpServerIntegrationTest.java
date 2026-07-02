package com.agentx4j.mcp.server;

import com.agentx4j.core.model.PaymentRequirement;
import com.agentx4j.mcp.common.McpToolPrice;
import com.agentx4j.x402.server.X402ResourceServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * McpServerIntegration 测试。
 */
class McpServerIntegrationTest {

    private X402ResourceServer resourceServer;
    private McpServerIntegration integration;

    @BeforeEach
    void setUp() {
        resourceServer = mock(X402ResourceServer.class);
        integration = new McpServerIntegration(resourceServer);
    }

    @Test
    void testRegisterTool() {
        McpToolPrice price = McpToolPrice.builder()
                .toolName("get_weather")
                .price("$0.001")
                .network("eip155:84532")
                .build();

        integration.registerTool("get_weather", price, null, null);

        assertTrue(integration.isPaidTool("get_weather"));
        assertFalse(integration.isPaidTool("unknown_tool"));
    }

    @Test
    void testGetToolPaymentRequirements() {
        McpToolPrice price = McpToolPrice.builder()
                .toolName("get_weather")
                .price("$0.001")
                .network("eip155:84532")
                .asset("0x036CbD...")
                .payTo("0xReceiver")
                .scheme(com.agentx4j.core.enums.BillingScheme.EXACT)
                .maxTimeoutSeconds(60)
                .build();

        integration.registerTool("get_weather", price, null, null);

        List<PaymentRequirement> requirements = integration.getToolPaymentRequirements("get_weather");
        assertFalse(requirements.isEmpty());
        assertEquals("1000", requirements.get(0).getAmount()); // $0.001 = 1000 原子单位
    }

    @Test
    void testGetRegisteredTools() {
        McpToolPrice price = McpToolPrice.builder()
                .toolName("tool1")
                .price("$0.001")
                .network("eip155:84532")
                .build();

        integration.registerTool("tool1", price, null, null);

        Set<String> tools = integration.getRegisteredTools();
        assertTrue(tools.contains("tool1"));
        assertEquals(1, tools.size());
    }
}
