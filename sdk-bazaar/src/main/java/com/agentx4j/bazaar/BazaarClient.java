package com.agentx4j.bazaar;

import com.agentx4j.core.model.PaymentRequirement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Bazaar 客户端 — Agent 服务发现。
 *
 * <p>对应 x402 Bazaar 发现层。</p>
 * <p>参考: https://docs.x402.org/extensions/bazaar.md</p>
 *
 * <p>功能：</p>
 * <ol>
 *   <li>发现可用服务（HTTP 端点 + MCP 工具）</li>
 *   <li>按类型/价格/标签过滤</li>
 *   <li>注册自己的服务到 Bazaar</li>
 *   <li>搜索服务（全文搜索）</li>
 * </ol>
 *
 * <p>类比：Bazaar ≈ "美团/大众点评"</p>
 * <ul>
 *   <li>可以搜索附近的餐厅（搜索服务）</li>
 *   <li>可以看菜单和价格（查看支付要求）</li>
 *   <li>也可以注册自己的餐厅（发布服务）</li>
 * </ul>
 *
 * <p>使用方式：</p>
 * <pre>{@code
 * BazaarClient client = new BazaarClient("https://x402.org/facilitator");
 *
 * // 发现所有 HTTP 服务
 * List<ServiceListing> services = client.discoverServices(
 *     DiscoveryFilter.builder().type("http").build());
 *
 * // 发现价格低于 $0.01 的 MCP 工具
 * List<ServiceListing> cheapTools = client.discoverServices(
 *     DiscoveryFilter.builder()
 *         .type("mcp")
 *         .maxPrice(new BigDecimal("0.01"))
 *         .build());
 *
 * // 注册自己的服务
 * client.registerService(ServiceListing.builder()
 *     .resourceName("https://my-agent.com/api/weather")
 *     .resourceType("http")
 *     .serviceName("天气查询")
 *     .description("提供全球天气查询服务")
 *     .accepts(List.of(PaymentRequirement.builder()
 *         .scheme("exact").network("eip155:84532")
 *         .amount("1000").asset("0x...").payTo("0x...")
 *         .build()))
 *     .build());
 * }</pre>
 */
public class BazaarClient {

    private static final Logger log = LoggerFactory.getLogger(BazaarClient.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final String DISCOVERY_PATH = "/discovery/resources";
    private static final String REGISTER_PATH = "/discovery/register";

    private final String facilitatorUrl;
    private final OkHttpClient httpClient;

    public BazaarClient(String facilitatorUrl) {
        this.facilitatorUrl = facilitatorUrl.endsWith("/")
                ? facilitatorUrl.substring(0, facilitatorUrl.length() - 1)
                : facilitatorUrl;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();
    }

    public BazaarClient(String facilitatorUrl, OkHttpClient httpClient) {
        this.facilitatorUrl = facilitatorUrl.endsWith("/")
                ? facilitatorUrl.substring(0, facilitatorUrl.length() - 1)
                : facilitatorUrl;
        this.httpClient = httpClient;
    }

    /**
     * 发现可用服务。
     *
     * <p>GET /discovery/resources</p>
     *
     * @param filter 过滤条件
     * @return 服务列表
     */
    public List<ServiceListing> discoverServices(DiscoveryFilter filter) {
        try {
            StringBuilder url = new StringBuilder(facilitatorUrl + DISCOVERY_PATH);
            StringBuilder params = new StringBuilder();

            if (filter != null) {
                if (filter.getType() != null) {
                    appendParam(params, "type", filter.getType());
                }
                if (filter.getQuery() != null) {
                    appendParam(params, "query", filter.getQuery());
                }
                if (filter.getMaxPrice() != null) {
                    appendParam(params, "maxPrice", filter.getMaxPrice().toPlainString());
                }
                if (filter.getLimit() > 0) {
                    appendParam(params, "limit", String.valueOf(filter.getLimit()));
                }
                if (filter.getOffset() > 0) {
                    appendParam(params, "offset", String.valueOf(filter.getOffset()));
                }
            }

            if (params.length() > 0) {
                url.append("?").append(params);
            }

            Request request = new Request.Builder()
                    .url(url.toString())
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.warn("Discovery request failed: {}", response.code());
                    return Collections.emptyList();
                }

                ResponseBody body = response.body();
                if (body == null) return Collections.emptyList();

                String json = body.string();
                return parseServiceListings(json);
            }
        } catch (IOException e) {
            log.error("Failed to discover services", e);
            return Collections.emptyList();
        }
    }

    /**
     * 发现 HTTP 服务。
     */
    public List<ServiceListing> discoverHttpServices(String query) {
        return discoverServices(DiscoveryFilter.builder()
                .type("http")
                .query(query)
                .build());
    }

    /**
     * 发现 MCP 工具。
     */
    public List<ServiceListing> discoverMcpTools(String query) {
        return discoverServices(DiscoveryFilter.builder()
                .type("mcp")
                .query(query)
                .build());
    }

    /**
     * 按价格过滤。
     */
    public List<ServiceListing> filterByMaxPrice(List<ServiceListing> services, BigDecimal maxPrice) {
        if (services == null || maxPrice == null) return services;
        return services.stream()
                .filter(s -> s.getPrice() != null)
                .filter(s -> {
                    try {
                        return new BigDecimal(s.getPrice()).compareTo(maxPrice) <= 0;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * 搜索服务（本地过滤）。
     */
    public List<ServiceListing> search(List<ServiceListing> services, String keyword) {
        if (services == null || keyword == null || keyword.isEmpty()) return services;
        String lowerKeyword = keyword.toLowerCase();
        return services.stream()
                .filter(s -> {
                    if (s.getServiceName() != null && s.getServiceName().toLowerCase().contains(lowerKeyword))
                        return true;
                    if (s.getDescription() != null && s.getDescription().toLowerCase().contains(lowerKeyword))
                        return true;
                    if (s.getTags() != null && s.getTags().stream().anyMatch(t -> t.toLowerCase().contains(lowerKeyword)))
                        return true;
                    return false;
                })
                .collect(Collectors.toList());
    }

    /**
     * 注册服务到 Bazaar。
     *
     * <p>POST /discovery/register</p>
     */
    public boolean registerService(ServiceListing service) {
        try {
            Map<String, Object> body = buildRegistrationBody(service);
            String json = objectMapper.writeValueAsString(body);

            Request request = new Request.Builder()
                    .url(facilitatorUrl + REGISTER_PATH)
                    .post(RequestBody.create(json, MediaType.parse("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                boolean success = response.isSuccessful();
                if (success) {
                    log.info("Service registered: {}", service.getServiceName());
                } else {
                    log.warn("Service registration failed: {}", response.code());
                }
                return success;
            }
        } catch (IOException e) {
            log.error("Failed to register service", e);
            return false;
        }
    }

    /**
     * 更新服务信息。
     */
    public boolean updateService(String serviceId, ServiceListing update) {
        try {
            Map<String, Object> body = buildRegistrationBody(update);
            body.put("serviceId", serviceId);
            String json = objectMapper.writeValueAsString(body);

            Request request = new Request.Builder()
                    .url(facilitatorUrl + REGISTER_PATH)
                    .put(RequestBody.create(json, MediaType.parse("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (IOException e) {
            log.error("Failed to update service", e);
            return false;
        }
    }

    /**
     * 下架服务。
     */
    public boolean deregisterService(String serviceId) {
        try {
            Request request = new Request.Builder()
                    .url(facilitatorUrl + REGISTER_PATH + "?serviceId=" + serviceId)
                    .delete()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (IOException e) {
            log.error("Failed to deregister service", e);
            return false;
        }
    }

    // ==================== 内部方法 ====================

    private void appendParam(StringBuilder sb, String key, String value) {
        if (sb.length() > 0) sb.append("&");
        sb.append(key).append("=").append(value);
    }

    private List<ServiceListing> parseServiceListings(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode items = root.has("items") ? root.get("items") : root;
            if (!items.isArray()) return Collections.emptyList();

            List<ServiceListing> listings = new ArrayList<>();
            for (JsonNode item : items) {
                listings.add(parseServiceListing(item));
            }
            return listings;
        } catch (Exception e) {
            log.warn("Failed to parse service listings", e);
            return Collections.emptyList();
        }
    }

    private ServiceListing parseServiceListing(JsonNode node) {
        try {
            ServiceListing.ServiceListingBuilder builder = ServiceListing.builder();

            if (node.has("serviceId")) builder.serviceId(node.get("serviceId").asText());
            if (node.has("resource")) builder.resourceName(node.get("resource").asText());
            if (node.has("type")) builder.resourceType(node.get("type").asText());
            if (node.has("serviceName")) builder.serviceName(node.get("serviceName").asText());
            if (node.has("description")) builder.description(node.get("description").asText());
            if (node.has("mimeType")) builder.mimeType(node.get("mimeType").asText());
            if (node.has("iconUrl")) builder.iconUrl(node.get("iconUrl").asText());
            if (node.has("transport")) builder.transport(node.get("transport").asText());
            if (node.has("lastUpdated")) builder.lastUpdated(java.time.Instant.parse(node.get("lastUpdated").asText()));

            // 解析 tags
            if (node.has("tags") && node.get("tags").isArray()) {
                List<String> tags = new ArrayList<>();
                for (JsonNode tag : node.get("tags")) {
                    tags.add(tag.asText());
                }
                builder.tags(tags);
            }

            // 解析 accepts（支付要求）
            if (node.has("accepts") && node.get("accepts").isArray()) {
                List<PaymentRequirement> accepts = new ArrayList<>();
                for (JsonNode accept : node.get("accepts")) {
                    accepts.add(parsePaymentRequirement(accept));
                }
                builder.accepts(accepts);
            }

            return builder.build();
        } catch (Exception e) {
            log.warn("Failed to parse service listing", e);
            return ServiceListing.builder().build();
        }
    }

    private PaymentRequirement parsePaymentRequirement(JsonNode node) {
        PaymentRequirement.PaymentRequirementBuilder builder = PaymentRequirement.builder();
        if (node.has("scheme")) builder.scheme(node.get("scheme").asText());
        if (node.has("network")) builder.network(node.get("network").asText());
        if (node.has("amount")) builder.amount(node.get("amount").asText());
        if (node.has("asset")) builder.asset(node.get("asset").asText());
        if (node.has("payTo")) builder.payTo(node.get("payTo").asText());
        if (node.has("maxTimeoutSeconds"))
            builder.maxTimeoutSeconds(node.get("maxTimeoutSeconds").longValue());
        return builder.build();
    }

    private Map<String, Object> buildRegistrationBody(ServiceListing service) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (service.getServiceId() != null) body.put("serviceId", service.getServiceId());
        if (service.getResourceName() != null) body.put("resource", service.getResourceName());
        if (service.getResourceType() != null) body.put("type", service.getResourceType());
        if (service.getServiceName() != null) body.put("serviceName", service.getServiceName());
        if (service.getDescription() != null) body.put("description", service.getDescription());
        if (service.getMimeType() != null) body.put("mimeType", service.getMimeType());
        if (service.getIconUrl() != null) body.put("iconUrl", service.getIconUrl());
        if (service.getTags() != null) body.put("tags", service.getTags());
        if (service.getTransport() != null) body.put("transport", service.getTransport());
        if (service.getAccepts() != null) body.put("accepts", service.getAccepts());
        return body;
    }
}
