package com.agentx4j.demo;

import com.agentx4j.x402.client.X402Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 买家控制器 — 演示如何调用付费 API。
 *
 * <p>展示 X402Client 的使用方式。</p>
 *
 * <p>注意：完整功能需要配置钱包私钥。</p>
 */
@RestController
@RequestMapping("/buyer")
public class BuyerController {

    @Autowired(required = false)
    private X402Client x402Client;

    /**
     * 查询天气（自动处理支付）。
     *
     * <p>GET /buyer/weather?city=Beijing&sellerUrl=http://localhost:8080</p>
     */
    @GetMapping("/weather")
    public Map<String, Object> buyWeather(
            @RequestParam String city,
            @RequestParam String sellerUrl) {

        if (x402Client == null) {
            return Map.of("error", "X402Client not configured. Set agent.x402.wallet-private-key.");
        }

        try {
            X402Client.X402Response response = x402Client.get(
                    sellerUrl + "/api/weather?city=" + city);

            return Map.of(
                    "success", response.isSuccess(),
                    "statusCode", response.getStatusCode(),
                    "body", response.getBody(),
                    "paymentProcessed", response.isPaymentProcessed()
            );
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * 获取钱包信息。
     */
    @GetMapping("/wallet-info")
    public Map<String, Object> getWalletInfo() {
        if (x402Client == null) {
            return Map.of("error", "X402Client not configured");
        }
        return Map.of(
                "status", "configured",
                "note", "Wallet address available via X402Client.getWalletInfo()"
        );
    }

    /**
     * 测试 x402 客户端是否可用。
     */
    @GetMapping("/client-status")
    public Map<String, Object> clientStatus() {
        return Map.of(
                "x402Client", x402Client != null ? "configured" : "not configured",
                "note", "To enable buyer mode, set agent.x402.wallet-private-key in application.yml"
        );
    }
}
