package com.agentx4j.x402.client;

import com.agentx4j.core.constant.SdkConstants;
import com.agentx4j.core.model.PaymentPayload;
import com.agentx4j.core.model.PaymentRequirement;
import com.agentx4j.core.model.PaymentResponse;
import com.agentx4j.x402.scheme.Scheme;
import com.agentx4j.x402.scheme.SchemeRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * x402 Client — 服务调用方核心。
 *
 * <p>职责：</p>
 * <ol>
 *   <li>发送 HTTP 请求</li>
 *   <li>收到 402 响应 → 自动解析 PaymentRequirements</li>
 *   <li>选择最优支付方案</li>
 *   <li>构建并签名 PaymentPayload</li>
 *   <li>重试请求（携带 PAYMENT-SIGNATURE）</li>
 *   <li>返回最终结果 + 结算凭证</li>
 * </ol>
 *
 * <p>类比：X402Client ≈ 消费者的"自动钱包"
 *       看到价目表 → 自动选支付方式 → 自动签名 → 自动付款</p>
 *
 * <p>使用方式：</p>
 * <pre>{@code
 * X402Client client = new X402Client();
 * client.registerScheme("eip155:*", new ExactScheme());
 * client.setWallet(privateKeyBytes, "0xYourAddress");
 *
 * // 自动处理 402 → 支付 → 重试
 * X402Response response = client.get("https://api.example.com/weather?city=Beijing");
 * System.out.println(response.getBody());
 * }</pre>
 */
public class X402Client {

    private static final Logger log = LoggerFactory.getLogger(X402Client.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final SchemeRegistry schemeRegistry;
    private final OkHttpClient httpClient;
    private byte[] privateKey;
    private String fromAddress;

    public X402Client() {
        this.schemeRegistry = new SchemeRegistry();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public X402Client(OkHttpClient httpClient) {
        this.schemeRegistry = new SchemeRegistry();
        this.httpClient = httpClient;
    }

    /**
     * 注册支付方案。
     *
     * @param network 网络匹配模式
     * @param scheme  支付方案实现
     * @return this (链式调用)
     */
    public X402Client registerScheme(String network, Scheme scheme) {
        schemeRegistry.register(network, scheme);
        return this;
    }

    /**
     * 设置钱包信息。
     *
     * @param privateKey  私钥字节
     * @param fromAddress 付款方地址
     * @return this (链式调用)
     */
    public X402Client setWallet(byte[] privateKey, String fromAddress) {
        this.privateKey = privateKey;
        this.fromAddress = fromAddress;
        return this;
    }

    /**
     * 发送 GET 请求（自动处理 402 支付流程）。
     *
     * @param url 请求 URL
     * @return 响应（包含 body 和支付状态）
     */
    public X402Response get(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        return executeWithPayment(request);
    }

    /**
     * 发送 POST 请求（自动处理 402 支付流程）。
     *
     * @param url  请求 URL
     * @param body 请求体（JSON）
     * @return 响应
     */
    public X402Response post(String url, String body) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(body, MediaType.parse("application/json")))
                .build();
        return executeWithPayment(request);
    }

    /**
     * 发送自定义请求（自动处理 402 支付流程）。
     *
     * @param request HTTP 请求
     * @return 响应
     */
    public X402Response execute(Request request) throws IOException {
        return executeWithPayment(request);
    }

    /**
     * 选择最佳支付方案。
     *
     * <p>策略：优先选已注册 scheme 中支持的第一个。</p>
     */
    public PaymentRequirement selectPaymentRequirement(List<PaymentRequirement> accepts) {
        if (accepts == null || accepts.isEmpty()) {
            throw new IllegalArgumentException("No payment requirements provided");
        }

        for (PaymentRequirement req : accepts) {
            if (schemeRegistry.isSupported(req.getScheme(), req.getNetwork())) {
                return req;
            }
        }

        // 没有精确匹配，返回第一个
        return accepts.get(0);
    }

    /**
     * 创建并签名支付载荷。
     */
    public PaymentPayload createPayment(PaymentRequirement requirement) {
        if (privateKey == null) {
            throw new IllegalStateException("Wallet not configured. Call setWallet() first.");
        }
        Scheme scheme = schemeRegistry.find(requirement.getScheme(), requirement.getNetwork());
        if (scheme == null) {
            throw new IllegalStateException("No scheme registered for: "
                    + requirement.getScheme() + " / " + requirement.getNetwork());
        }

        Scheme.SchemeContext context = Scheme.SchemeContext.builder()
                .requirement(requirement)
                .fromAddress(fromAddress)
                .privateKey(privateKey)
                .build();

        return scheme.createPayment(context);
    }

    /**
     * 获取底层的 SchemeRegistry。
     */
    public SchemeRegistry getSchemeRegistry() {
        return schemeRegistry;
    }

    // ==================== 内部方法 ====================

    /**
     * 执行请求，自动处理 402 支付流程。
     *
     * <p>流程：</p>
     * <ol>
     *   <li>发送初始请求</li>
     *   <li>如果收到 402，解析支付要求</li>
     *   <li>选择方案并签名</li>
     *   <li>重试请求（携带签名）</li>
     *   <li>返回最终结果</li>
     * </ol>
     */
    private X402Response executeWithPayment(Request originalRequest) throws IOException {
        // 第一次请求
        try (Response response = httpClient.newCall(originalRequest).execute()) {
            // 如果不是 402，直接返回
            if (response.code() != 402) {
                return buildResponse(response, false);
            }

            // 解析支付要求
            String paymentRequiredHeader = response.header(SdkConstants.HEADER_PAYMENT_REQUIRED);
            if (paymentRequiredHeader == null || paymentRequiredHeader.isEmpty()) {
                return buildResponse(response, false);
            }

            // 解码 Base64
            byte[] decoded = Base64.getDecoder().decode(paymentRequiredHeader);
            Map<String, Object> paymentReq = objectMapper.readValue(decoded, Map.class);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> accepts = (List<Map<String, Object>>) paymentReq.get("accepts");
            if (accepts == null || accepts.isEmpty()) {
                return buildResponse(response, false);
            }

            // 转换为 PaymentRequirement 列表
            List<PaymentRequirement> requirements = accepts.stream()
                    .map(this::mapToPaymentRequirement)
                    .toList();

            // 选择支付方案
            PaymentRequirement selectedReq = selectPaymentRequirement(requirements);

            // 创建签名支付
            PaymentPayload payload = createPayment(selectedReq);

            // Base64 编码
            String payloadJson = objectMapper.writeValueAsString(payload);
            String payloadB64 = Base64.getEncoder().encodeToString(
                    payloadJson.getBytes(StandardCharsets.UTF_8));

            // 重试请求 + 签名
            Request retryRequest = originalRequest.newBuilder()
                    .header(SdkConstants.HEADER_PAYMENT_SIGNATURE, payloadB64)
                    .build();

            try (Response retryResponse = httpClient.newCall(retryRequest).execute()) {
                return buildResponse(retryResponse, true);
            }
        }
    }

    private PaymentRequirement mapToPaymentRequirement(Map<String, Object> map) {
        @SuppressWarnings("unchecked")
        Map<String, String> extra = (Map<String, String>) map.get("extra");
        return PaymentRequirement.builder()
                .scheme((String) map.get("scheme"))
                .network((String) map.get("network"))
                .amount((String) map.get("amount"))
                .asset((String) map.get("asset"))
                .payTo((String) map.get("payTo"))
                .maxTimeoutSeconds(map.get("maxTimeoutSeconds") != null
                        ? ((Number) map.get("maxTimeoutSeconds")).longValue() : 60)
                .extra(extra)
                .build();
    }

    private X402Response buildResponse(Response response, boolean paymentProcessed) throws IOException {
        ResponseBody body = response.body();
        String bodyStr = body != null ? body.string() : "";

        // 解析结算响应
        PaymentResponse paymentResponse = null;
        String paymentResponseHeader = response.header(SdkConstants.HEADER_PAYMENT_RESPONSE);
        if (paymentResponseHeader != null && !paymentResponseHeader.isEmpty()) {
            try {
                byte[] decoded = Base64.getDecoder().decode(paymentResponseHeader);
                paymentResponse = objectMapper.readValue(decoded, PaymentResponse.class);
            } catch (Exception e) {
                log.debug("Failed to parse PAYMENT-RESPONSE header", e);
            }
        }

        return X402Response.builder()
                .statusCode(response.code())
                .body(bodyStr)
                .paymentProcessed(paymentProcessed)
                .paymentResponse(paymentResponse)
                .build();
    }

    // ==================== 响应封装 ====================

    /**
     * x402 客户端响应。
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class X402Response {
        /** HTTP 状态码 */
        private int statusCode;

        /** 响应体 */
        private String body;

        /** 是否经过了支付处理 */
        private boolean paymentProcessed;

        /** 支付结算响应（200 时可能有） */
        private PaymentResponse paymentResponse;

        /** 是否成功 */
        public boolean isSuccess() {
            return statusCode >= 200 && statusCode < 300;
        }
    }
}
