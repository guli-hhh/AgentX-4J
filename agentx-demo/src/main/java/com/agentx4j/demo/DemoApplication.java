package com.agentx4j.demo;

import com.agentx4j.starter.annotation.EnableX402;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AgentX-4J 全栈示例应用。
 *
 * <p>功能：</p>
 * <ul>
 *   <li>GET  /api/health          — 健康检查（免费）</li>
 *   <li>GET  /api/pricing         — 查询服务定价（免费）</li>
 *   <li>GET  /api/weather         — 天气查询（收费 $0.001）</li>
 *   <li>POST /api/translate       — 翻译（收费 $0.005）</li>
 *   <li>GET  /buyer/weather       — 买家自动支付示例</li>
 *   <li>GET  /buyer/client-status — 客户端状态</li>
 * </ul>
 *
 * <p>运行方式：</p>
 * <pre>{@code
 * # 仅卖家模式
 * java -jar agentx-demo.jar --agent.x402.pay-to=0xYourAddress
 *
 * # 买家+卖家模式
 * java -jar agentx-demo.jar \
 *   --agent.x402.pay-to=0xSellerAddress \
 *   --agent.x402.wallet-private-key=0xBuyerPrivateKey
 * }</pre>
 */
@SpringBootApplication
@EnableX402
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
