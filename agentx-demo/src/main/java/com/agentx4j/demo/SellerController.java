package com.agentx4j.demo;

import com.agentx4j.core.model.PaymentRequirement;
import com.agentx4j.starter.annotation.X402Protected;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 卖家控制器 — 提供付费 API。
 *
 * <p>演示 @X402Protected 注解的使用。</p>
 *
 * <p>测试方式：</p>
 * <ol>
 *   <li>curl http://localhost:8080/api/weather?city=Beijing → 收到 402</li>
 *   <li>客户端签名支付后重试 → 收到 200 + 天气数据</li>
 * </ol>
 */
@RestController
@RequestMapping("/api")
public class SellerController {

    @Value("${agent.x402.asset:0x036CbD53842c5426634e7929541eC2318f3dCF7e}")
    private String asset;

    /**
     * 付费天气查询接口。
     *
     * <p>客户端需要先支付 $0.001 USDC 才能访问。</p>
     * <p>x402 流程：</p>
     * <ol>
     *   <li>客户端 GET /api/weather?city=Beijing</li>
     *   <li>服务端返回 402 + PaymentRequirements</li>
     *   <li>客户端签名支付并重试</li>
     *   <li>服务端验证 + 结算 + 返回天气数据</li>
     * </ol>
     */
    @X402Protected(
        price = "$0.001",
        network = "eip155:84532"
    )
    @GetMapping("/weather")
    public Map<String, Object> getWeather(@RequestParam String city) {
        // 如果执行到这里，说明支付已经验证通过
        return Map.of(
                "city", city,
                "weather", "sunny",
                "temperature", 25,
                "humidity", 60,
                "unit", "celsius",
                "message", "Data retrieved successfully (payment verified)"
        );
    }

    /**
     * 付费翻译接口（更贵）。
     */
    @X402Protected(
        price = "$0.005",
        network = "eip155:84532"
    )
    @PostMapping("/translate")
    public Map<String, Object> translate(@RequestBody Map<String, String> request) {
        return Map.of(
                "original", request.getOrDefault("text", ""),
                "translated", "Translation result (demo)",
                "from", request.getOrDefault("from", "zh"),
                "to", request.getOrDefault("to", "en")
        );
    }

    /**
     * 免费健康检查接口。
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "agentx-demo",
                "version", "1.0.0"
        );
    }

    /**
     * 查询支付要求（供客户端参考）。
     *
     * <p>GET /api/pricing</p>
     */
    @GetMapping("/pricing")
    public Map<String, Object> pricing() {
        return Map.of(
                "services", List.of(
                        Map.of("endpoint", "/api/weather", "price", "$0.001", "description", "天气查询"),
                        Map.of("endpoint", "/api/translate", "price", "$0.005", "description", "文本翻译")
                ),
                "network", "eip155:84532",
                "asset", asset,
                "currency", "USDC"
        );
    }
}
