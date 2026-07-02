package com.agentx4j.x402.facilitator;

import com.agentx4j.core.model.PaymentPayload;
import com.agentx4j.core.model.PaymentRequirement;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 基于 HTTP 的 Facilitator 客户端实现。
 *
 * <p>使用 OkHttp 与 Facilitator 服务通信。</p>
 */
public class HttpFacilitatorClient implements FacilitatorClient {

    private static final Logger log = LoggerFactory.getLogger(HttpFacilitatorClient.class);
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final String baseUrl;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public HttpFacilitatorClient(String baseUrl) {
        this(baseUrl, new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build());
    }

    public HttpFacilitatorClient(String baseUrl, OkHttpClient httpClient) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public VerifyResponse verify(PaymentPayload payload, PaymentRequirement requirement) {
        try {
            String json = objectMapper.writeValueAsString(new VerifyRequest(payload, requirement));
            Request request = new Request.Builder()
                    .url(baseUrl + "/verify")
                    .post(RequestBody.create(json, JSON))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                ResponseBody body = response.body();
                String responseBody = body != null ? body.string() : "{}";
                return objectMapper.readValue(responseBody, VerifyResponse.class);
            }
        } catch (IOException e) {
            log.error("Failed to verify payment with facilitator", e);
            return VerifyResponse.builder()
                    .valid(false)
                    .invalidReason("Facilitator communication error: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public SettleResponse settle(PaymentPayload payload, PaymentRequirement requirement) {
        try {
            String json = objectMapper.writeValueAsString(new SettleRequest(payload, requirement));
            Request request = new Request.Builder()
                    .url(baseUrl + "/settle")
                    .post(RequestBody.create(json, JSON))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                ResponseBody body = response.body();
                String responseBody = body != null ? body.string() : "{}";
                return objectMapper.readValue(responseBody, SettleResponse.class);
            }
        } catch (IOException e) {
            log.error("Failed to settle payment with facilitator", e);
            return SettleResponse.builder()
                    .success(false)
                    .error("Facilitator communication error: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public SupportedNetworksResponse getSupportedNetworks() {
        try {
            Request request = new Request.Builder()
                    .url(baseUrl + "/supported")
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                ResponseBody body = response.body();
                String responseBody = body != null ? body.string() : "{}";
                return objectMapper.readValue(responseBody, SupportedNetworksResponse.class);
            }
        } catch (IOException e) {
            log.error("Failed to get supported networks from facilitator", e);
            return SupportedNetworksResponse.builder().build();
        }
    }

    // ==================== 内部请求体 ====================

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class VerifyRequest {
        private PaymentPayload paymentPayload;
        private PaymentRequirement paymentRequirements;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class SettleRequest {
        private PaymentPayload paymentPayload;
        private PaymentRequirement paymentRequirements;
    }
}
