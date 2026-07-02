package com.agentx4j.starter.aspect;

import com.agentx4j.core.constant.SdkConstants;
import com.agentx4j.core.model.PaymentPayload;
import com.agentx4j.core.model.PaymentRequirement;
import com.agentx4j.starter.AgentX4JProperties;
import com.agentx4j.starter.annotation.X402Protected;
import com.agentx4j.x402.server.VerifyResult;
import com.agentx4j.x402.server.X402ResourceServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code @X402Protected} 注解的 AOP 切面实现。
 *
 * <p>拦截标注了 @X402Protected 的方法，自动执行支付验证流程：</p>
 * <ol>
 *   <li>检查请求中的 PAYMENT-SIGNATURE header</li>
 *   <li>无签名 → 返回 402 + PaymentRequirements</li>
 *   <li>有签名 → 验证 → 通过则执行方法体</li>
 * </ol>
 */
@Aspect
@Component
public class X402ProtectedAspect {

    private static final Logger log = LoggerFactory.getLogger(X402ProtectedAspect.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired(required = false)
    private X402ResourceServer resourceServer;

    @Autowired
    private AgentX4JProperties properties;

    /**
     * 环绕通知：拦截 @X402Protected 方法。
     *
     * @param joinPoint      切点信息
     * @param x402Protected  注解实例
     * @return 方法执行结果，或 null（支付未通过时）
     * @throws Throwable 方法执行异常
     */
    @Around("@annotation(x402Protected)")
    public Object around(ProceedingJoinPoint joinPoint, X402Protected x402Protected) throws Throwable {
        // 获取当前 HTTP 请求/响应
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            // 非 HTTP 请求上下文，直接放行
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        HttpServletResponse response = attributes.getResponse();

        if (response == null) {
            return joinPoint.proceed();
        }

        // 构建 PaymentRequirement
        PaymentRequirement requirement = buildPaymentRequirement(x402Protected, request);

        // 检查支付签名
        String paymentSignature = request.getHeader(SdkConstants.HEADER_PAYMENT_SIGNATURE);

        if (paymentSignature == null || paymentSignature.isEmpty()) {
            // 无签名 → 返回 402
            sendPaymentRequired(response, requirement, request.getRequestURI(), null);
            return null;
        }

        // 解析并验证支付
        try {
            byte[] decoded = Base64.getDecoder().decode(paymentSignature);
            PaymentPayload payload = objectMapper.readValue(decoded, PaymentPayload.class);

            if (resourceServer != null) {
                VerifyResult result = resourceServer.verifyPayment(payload, requirement);
                if (!result.isValid()) {
                    sendPaymentRequired(response, requirement, request.getRequestURI(), result.getReason());
                    return null;
                }
            }

            // 验证通过 → 执行方法体
            return joinPoint.proceed();

        } catch (Exception e) {
            log.warn("Payment verification error", e);
            sendPaymentRequired(response, requirement, request.getRequestURI(),
                    "Invalid payment: " + e.getMessage());
            return null;
        }
    }

    // ==================== 内部方法 ====================

    private PaymentRequirement buildPaymentRequirement(X402Protected annotation, HttpServletRequest request) {
        // 解析价格（将 "$0.001" 转换为原子单位 "1000"）
        String amount = parsePriceToAtomicUnits(annotation.price());

        // 确定代币地址
        String asset = annotation.asset().isEmpty() ? properties.getAsset() : annotation.asset();

        // 确定收款地址
        String payTo = annotation.payTo().isEmpty() ? properties.getPayTo() : annotation.payTo();

        return PaymentRequirement.builder()
                .scheme(annotation.scheme().getSchemeName())
                .network(annotation.network())
                .amount(amount)
                .asset(asset)
                .payTo(payTo)
                .maxTimeoutSeconds(annotation.maxTimeoutSeconds())
                .build();
    }

    /**
     * 将美元价格字符串转换为原子单位。
     * <p>如 "$0.001" → "1000"（USDC 6 位小数）</p>
     */
    private String parsePriceToAtomicUnits(String price) {
        if (price.startsWith("$")) {
            price = price.substring(1);
        }
        java.math.BigDecimal dollars = new java.math.BigDecimal(price);
        java.math.BigDecimal atomicUnits = dollars.multiply(java.math.BigDecimal.valueOf(1_000_000));
        return atomicUnits.toBigInteger().toString();
    }

    private void sendPaymentRequired(HttpServletResponse response, PaymentRequirement requirement,
                                       String path, String error) throws java.io.IOException {
        Map<String, Object> body = new HashMap<>();
        body.put("x402Version", SdkConstants.X402_PROTOCOL_VERSION);
        body.put("accepts", List.of(requirement));
        body.put("resource", Map.of("url", path));
        if (error != null) {
            body.put("error", error);
        }

        String json = objectMapper.writeValueAsString(body);
        String base64 = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));

        response.setStatus(402);
        response.setHeader(SdkConstants.HEADER_PAYMENT_REQUIRED, base64);
        response.setContentType("application/json");
        response.getWriter().write(json);
    }
}
