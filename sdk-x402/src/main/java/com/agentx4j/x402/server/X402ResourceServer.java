package com.agentx4j.x402.server;

import com.agentx4j.core.model.PaymentPayload;
import com.agentx4j.core.model.PaymentRequirement;
import com.agentx4j.core.model.PaymentResponse;
import com.agentx4j.core.model.SettlementResult;
import com.agentx4j.core.enums.TransactionStatus;
import com.agentx4j.x402.facilitator.FacilitatorClient;
import com.agentx4j.x402.facilitator.SettleResponse;
import com.agentx4j.x402.facilitator.VerifyResponse;
import com.agentx4j.x402.scheme.Scheme;
import com.agentx4j.x402.scheme.SchemeRegistry;
import com.agentx4j.x402.network.NetworkAdapter;
import com.agentx4j.x402.network.NetworkAdapterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * x402 Resource Server — 服务提供方核心。
 *
 * <p>职责：</p>
 * <ol>
 *   <li>拦截请求，检查是否携带支付签名</li>
 *   <li>无签名 → 返回 402 + PaymentRequirements（告诉买家怎么付）</li>
 *   <li>有签名 → 验证签名有效性</li>
 *   <li>验证通过 → 执行业务逻辑</li>
 *   <li>执行完成 → 通过 Facilitator 结算</li>
 * </ol>
 *
 * <p>类比：X402ResourceServer ≈ 商家的"收银系统"</p>
 * <ul>
 *   <li>没付钱 → 显示价目表</li>
 *   <li>付了钱 → 验钞 + 出货 + 记账</li>
 * </ul>
 */
public class X402ResourceServer {

    private static final Logger log = LoggerFactory.getLogger(X402ResourceServer.class);

    private final FacilitatorClient facilitatorClient;
    private final SchemeRegistry schemeRegistry;
    private final NetworkAdapterFactory networkAdapterFactory;

    public X402ResourceServer(FacilitatorClient facilitatorClient) {
        this.facilitatorClient = facilitatorClient;
        this.schemeRegistry = new SchemeRegistry();
        this.networkAdapterFactory = new NetworkAdapterFactory();
    }

    /**
     * 注册支付方案。
     *
     * @param network 网络匹配模式（如 "eip155:84532" 或 "eip155:*")
     * @param scheme  该网络的支付方案实现
     * @return this (链式调用)
     */
    public X402ResourceServer registerScheme(String network, Scheme scheme) {
        schemeRegistry.register(network, scheme);
        return this;
    }

    /**
     * 注册网络适配器。
     *
     * @param network 网络匹配模式
     * @param adapter 适配器实现
     * @return this (链式调用)
     */
    public X402ResourceServer registerNetworkAdapter(String network, NetworkAdapter adapter) {
        networkAdapterFactory.register(network, adapter);
        return this;
    }

    /**
     * 获取网络适配器工厂。
     */
    public NetworkAdapterFactory getNetworkAdapterFactory() {
        return networkAdapterFactory;
    }

    /**
     * 获取支付要求列表（用于返回 402 响应）。
     *
     * @param resource   受保护的资源路径
     * @param payTo      收款地址
     * @param requirements 该资源支持的支付要求列表
     * @return 支付要求列表
     */
    public List<PaymentRequirement> getPaymentRequirements(String resource, String payTo,
                                                              List<PaymentRequirement> requirements) {
        return requirements;
    }

    /**
     * 验证支付。
     *
     * <p>调用 Facilitator 的 /verify 端点验证支付签名。</p>
     *
     * @param payload     签名后的支付载荷
     * @param requirement 服务端声明的支付要求
     * @return 验证结果
     */
    public VerifyResult verifyPayment(PaymentPayload payload, PaymentRequirement requirement) {
        log.debug("Verifying payment for resource, scheme={}, network={}",
                payload.getScheme(), payload.getNetwork());

        // 1. 查找对应的 Scheme 实现
        Scheme scheme = schemeRegistry.find(payload.getScheme(), payload.getNetwork());
        if (scheme == null) {
            return VerifyResult.unsupported("No scheme registered for: "
                    + payload.getScheme() + " / " + payload.getNetwork());
        }

        // 2. Scheme 级别验证（本地验证）
        Scheme.VerifyResult localResult = scheme.verify(payload, requirement);
        if (!localResult.isValid()) {
            return VerifyResult.invalid(localResult.getReason());
        }

        // 3. Facilitator 验证（链上验证）
        VerifyResponse facilitatorResult = facilitatorClient.verify(payload, requirement);
        if (!facilitatorResult.isValid()) {
            return VerifyResult.invalid(facilitatorResult.getInvalidReason());
        }

        return VerifyResult.valid(facilitatorResult.getPayer());
    }

    /**
     * 结算支付。
     *
     * <p>调用 Facilitator 的 /settle 端点执行链上结算。</p>
     *
     * @param payload     签名后的支付载荷
     * @param requirement 服务端声明的支付要求
     * @return 结算结果
     */
    public SettleResult settlePayment(PaymentPayload payload, PaymentRequirement requirement) {
        log.debug("Settling payment, scheme={}, network={}",
                payload.getScheme(), payload.getNetwork());

        // 1. 查找对应的 Scheme 实现
        Scheme scheme = schemeRegistry.find(payload.getScheme(), payload.getNetwork());
        if (scheme == null) {
            return SettleResult.failed("No scheme registered for: "
                    + payload.getScheme() + " / " + payload.getNetwork());
        }

        // 2. Scheme 级别结算
        Scheme.SettleResult schemeResult = scheme.settle(payload, requirement);
        if (schemeResult.isSuccess()) {
            return SettleResult.success(schemeResult.getTxHash(), schemeResult.getBlockNumber());
        }

        // 3. 如果 Scheme 级别结算失败，尝试 Facilitator 结算
        SettleResponse facilitatorResult = facilitatorClient.settle(payload, requirement);
        if (facilitatorResult.isSuccess()) {
            return SettleResult.success(facilitatorResult.getTxHash(), facilitatorResult.getBlockNumber());
        }

        return SettleResult.failed(facilitatorResult.getError());
    }
}
