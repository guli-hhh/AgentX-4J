package com.agentx4j.x402.facilitator;

import com.agentx4j.core.model.PaymentPayload;
import com.agentx4j.core.model.PaymentRequirement;
import com.agentx4j.core.model.PaymentResponse;
import com.agentx4j.core.model.SettlementResult;
import com.agentx4j.core.enums.TransactionStatus;

/**
 * Facilitator 客户端 — 与支付协调方通信。
 *
 * <p>Facilitator 是 x402 生态中的第三方服务，负责：</p>
 * <ul>
 *   <li>验证支付签名（/verify）— 检查签名是否有效、余额是否充足</li>
 *   <li>执行链上结算（/settle）— 提交交易到区块链并等待确认</li>
 * </ul>
 *
 * <p>公共 Facilitator：</p>
 * <ul>
 *   <li>x402 官方：{@code https://x402.org/facilitator}（测试网）</li>
 *   <li>Coinbase：{@code https://api.cdp.coinbase.com/platform/v2/x402}</li>
 *   <li>PayAI：{@code https://facilitator.payai.network}</li>
 * </ul>
 *
 * <p>也可以自建 Facilitator（需要运行区块链节点 + 钱包）。</p>
 *
 * <p>类比：FacilitatorClient ≈ "POS 机的后台系统"，
 *      商家刷了卡（收到签名），POS 机后台去银行（Facilitator）验证和结算。</p>
 */
public interface FacilitatorClient {

    /**
     * 验证支付签名。
     *
     * <p>POST /verify</p>
     * <p>检查签名是否有效、金额是否匹配、nonce 未使用、余额充足。</p>
     *
     * @param payload     签名后的支付载荷
     * @param requirement 服务端声明的支付要求
     * @return 验证结果
     */
    VerifyResponse verify(PaymentPayload payload, PaymentRequirement requirement);

    /**
     * 结算支付。
     *
     * <p>POST /settle</p>
     * <p>提交链上交易并等待确认。</p>
     *
     * @param payload     签名后的支付载荷
     * @param requirement 服务端声明的支付要求
     * @return 结算结果（包含 txHash）
     */
    SettleResponse settle(PaymentPayload payload, PaymentRequirement requirement);

    /**
     * 获取 Facilitator 支持的网络列表。
     *
     * <p>GET /supported</p>
     *
     * @return 支持的网络和方案列表
     */
    SupportedNetworksResponse getSupportedNetworks();
}
