package com.agentx4j.x402.server;

import com.agentx4j.core.constant.SdkConstants;
import com.agentx4j.core.model.PaymentPayload;
import com.agentx4j.core.model.PaymentRequirement;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Spring Boot 支付拦截中间件。
 *
 * <p>自动拦截受保护请求，执行 x402 支付验证流程：</p>
 * <ol>
 *   <li>检查请求是否携带 PAYMENT-SIGNATURE header</li>
 *   <li>无签名 → 返回 402 + PaymentRequirements</li>
 *   <li>有签名 → 验证 → 通过则放行，不通过则返回 402 + 错误</li>
 * </ol>
 *
 * <p>使用方式：</p>
 * <pre>{@code
 * @Configuration
 * public class WebConfig implements WebMvcConfigurer {
 *     @Autowired
 *     private PaymentInterceptor paymentInterceptor;
 *
 *     @Override
 *     public void addInterceptors(InterceptorRegistry registry) {
 *         registry.addInterceptor(paymentInterceptor)
 *                 .addPathPatterns("/api/paid/**");
 *     }
 * }
 * }</pre>
 *
 * <p>或者使用 {@code @X402Protected} 注解方式（推荐）。</p>
 */
public class PaymentInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(PaymentInterceptor.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final X402ResourceServer resourceServer;
    private final Map<String, ProtectedRoute> routeConfig = new LinkedHashMap<>();

    public PaymentInterceptor(X402ResourceServer resourceServer) {
        this.resourceServer = resourceServer;
    }

    /**
     * 注册受保护路由。
     *
     * @param pathPattern         路径模式（如 "/api/paid/**"）
     * @param requirementSupplier 该路由的支付要求提供者
     */
    public void addProtectedRoute(String pathPattern, Supplier<List<PaymentRequirement>> requirementSupplier) {
        routeConfig.put(pathPattern, new ProtectedRoute(pathPattern, requirementSupplier));
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        String path = request.getRequestURI();

        // 1. 检查路径是否受保护
        ProtectedRoute route = findMatchingRoute(path);
        if (route == null) {
            return true; // 不受保护，放行
        }

        log.debug("Intercepting payment-protected request: {}", path);

        // 2. 检查是否携带支付签名
        String paymentSignature = request.getHeader(SdkConstants.HEADER_PAYMENT_SIGNATURE);

        if (paymentSignature == null || paymentSignature.isEmpty()) {
            // 无签名 → 返回 402 + PaymentRequirements
            sendPaymentRequired(response, route, path, null);
            return false;
        }

        // 3. 解析 PaymentPayload
        PaymentPayload payload;
        try {
            byte[] decoded = Base64.getDecoder().decode(paymentSignature);
            payload = objectMapper.readValue(decoded, PaymentPayload.class);
        } catch (Exception e) {
            log.warn("Failed to parse PAYMENT-SIGNATURE header", e);
            sendPaymentRequired(response, route, path, "Invalid payment signature format");
            return false;
        }

        // 4. 获取对应的 PaymentRequirement
        PaymentRequirement requirement = findMatchingRequirement(route.getRequirements(), payload);
        if (requirement == null) {
            sendPaymentRequired(response, route, path, "No matching payment requirement found");
            return false;
        }

        // 5. 验证支付
        VerifyResult verifyResult = resourceServer.verifyPayment(payload, requirement);
        if (!verifyResult.isValid()) {
            log.warn("Payment verification failed: {}", verifyResult.getReason());
            sendPaymentRequired(response, route, path, verifyResult.getReason());
            return false;
        }

        // 6. 验证通过 → 放行，并将 payload 存入 request attribute（供后续结算使用）
        request.setAttribute("x402.payload", payload);
        request.setAttribute("x402.requirement", requirement);
        request.setAttribute("x402.payer", verifyResult.getPayer());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                 Object handler, Exception ex) {
        // 请求完成后执行结算（如果验证通过且响应成功）
        if (ex == null && response.getStatus() >= 200 && response.getStatus() < 400) {
            PaymentPayload payload = (PaymentPayload) request.getAttribute("x402.payload");
            PaymentRequirement requirement = (PaymentRequirement) request.getAttribute("x402.requirement");

            if (payload != null && requirement != null) {
                // 异步结算
                new Thread(() -> {
                    try {
                        SettleResult settleResult = resourceServer.settlePayment(payload, requirement);
                        if (settleResult.isSuccess()) {
                            log.debug("Payment settled successfully, txHash={}", settleResult.getTxHash());
                        } else {
                            log.warn("Payment settlement failed: {}", settleResult.getError());
                        }
                    } catch (Exception e) {
                        log.error("Error during settlement", e);
                    }
                }).start();
            }
        }
    }

    // ==================== 内部方法 ====================

    private ProtectedRoute findMatchingRoute(String path) {
        for (Map.Entry<String, ProtectedRoute> entry : routeConfig.entrySet()) {
            if (pathMatches(entry.getKey(), path)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * 路径匹配：支持精确匹配和 Ant 风格通配符。
     */
    private boolean pathMatches(String pattern, String path) {
        // 精确匹配
        if (pattern.equals(path)) {
            return true;
        }
        // Ant 风格通配符: /api/** 匹配 /api/anything
        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return path.startsWith(prefix);
        }
        // 单通配符: /api/* 匹配 /api/something
        if (pattern.endsWith("/*")) {
            String prefix = pattern.substring(0, pattern.length() - 2);
            return path.startsWith(prefix) && path.indexOf('/', prefix.length()) == -1;
        }
        return false;
    }

    private PaymentRequirement findMatchingRequirement(List<PaymentRequirement> requirements,
                                                         PaymentPayload payload) {
        for (PaymentRequirement req : requirements) {
            if (req.getScheme().equals(payload.getScheme())
                    && req.getNetwork().equals(payload.getNetwork())) {
                return req;
            }
        }
        return null;
    }

    private void sendPaymentRequired(HttpServletResponse response, ProtectedRoute route,
                                       String path) throws IOException {
        sendPaymentRequired(response, route, path, null);
    }

    private void sendPaymentRequired(HttpServletResponse response, ProtectedRoute route,
                                       String path, String error) throws IOException {
        // 构建 PaymentRequired 响应体
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("x402Version", SdkConstants.X402_PROTOCOL_VERSION);
        body.put("accepts", route.getRequirements());
        body.put("resource", Map.of("url", path));
        if (error != null) {
            body.put("error", error);
        }

        // Base64 编码放入 header
        String json = objectMapper.writeValueAsString(body);
        String base64 = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));

        response.setStatus(402);
        response.setHeader(SdkConstants.HEADER_PAYMENT_REQUIRED, base64);
        response.setContentType("application/json");
        response.getWriter().write(json);
    }

    // ==================== 内部类 ====================

    private static class ProtectedRoute {
        private final String pathPattern;
        private final Supplier<List<PaymentRequirement>> requirementSupplier;

        ProtectedRoute(String pathPattern, Supplier<List<PaymentRequirement>> requirementSupplier) {
            this.pathPattern = pathPattern;
            this.requirementSupplier = requirementSupplier;
        }

        List<PaymentRequirement> getRequirements() {
            return requirementSupplier.get();
        }
    }
}
