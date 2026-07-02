package com.agentx4j.bazaar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BazaarClient 单元测试（不依赖外部服务）。
 */
class BazaarClientTest {

    private BazaarClient client;

    @BeforeEach
    void setUp() {
        client = new BazaarClient("https://x402.org/facilitator");
    }

    @Test
    void testDiscoverServicesWithNullFilter() {
        // 不会抛异常，返回空列表（因为无法连接测试网）
        List<ServiceListing> result = client.discoverServices(null);
        assertNotNull(result);
    }

    @Test
    void testDiscoverHttpServices() {
        List<ServiceListing> result = client.discoverHttpServices("weather");
        assertNotNull(result);
    }

    @Test
    void testDiscoverMcpTools() {
        List<ServiceListing> result = client.discoverMcpTools("llm");
        assertNotNull(result);
    }

    @Test
    void testFilterByMaxPrice() {
        List<ServiceListing> services = Arrays.asList(
                ServiceListing.builder().serviceName("Service A").price("0.001").build(),
                ServiceListing.builder().serviceName("Service B").price("0.01").build(),
                ServiceListing.builder().serviceName("Service C").price("0.1").build()
        );

        List<ServiceListing> filtered = client.filterByMaxPrice(services, new BigDecimal("0.01"));
        assertEquals(2, filtered.size());
    }

    @Test
    void testSearch() {
        List<ServiceListing> services = Arrays.asList(
                ServiceListing.builder()
                        .serviceName("天气查询")
                        .description("提供全球天气查询服务")
                        .tags(Arrays.asList("weather", "forecast"))
                        .build(),
                ServiceListing.builder()
                        .serviceName("翻译服务")
                        .description("多语言文本翻译")
                        .tags(Arrays.asList("translate", "nlp"))
                        .build(),
                ServiceListing.builder()
                        .serviceName("LLM 推理")
                        .description("大语言模型推理服务")
                        .tags(Arrays.asList("llm", "ai"))
                        .build()
        );

        // 按名称搜索
        List<ServiceListing> result = client.search(services, "天气");
        assertEquals(1, result.size());
        assertEquals("天气查询", result.get(0).getServiceName());

        // 按描述搜索
        result = client.search(services, "翻译");
        assertEquals(1, result.size());

        // 按标签搜索
        result = client.search(services, "llm");
        assertEquals(1, result.size());

        // 无结果
        result = client.search(services, "不存在的词");
        assertEquals(0, result.size());
    }

    @Test
    void testSearchWithNullKeyword() {
        List<ServiceListing> services = Arrays.asList(
                ServiceListing.builder().serviceName("Service A").build()
        );
        List<ServiceListing> result = client.search(services, null);
        assertEquals(1, result.size());
    }

    @Test
    void testFilterByMaxPriceWithNull() {
        List<ServiceListing> services = Arrays.asList(
                ServiceListing.builder().price("0.001").build()
        );
        List<ServiceListing> result = client.filterByMaxPrice(services, null);
        assertEquals(1, result.size());
    }

    @Test
    void testRegisterServiceValidation() {
        // 验证注册不会抛异常
        boolean result = client.registerService(ServiceListing.builder()
                .serviceName("Test Service")
                .build());
        // 因为无法连接，应该返回 false
        assertFalse(result);
    }
}
