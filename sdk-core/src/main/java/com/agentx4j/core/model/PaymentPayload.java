package com.agentx4j.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * x402 PaymentPayload — 支付载荷。
 *
 * <p>Client 创建的签名支付数据，证明"我授权从我的钱包转 X 代币到 Y 地址"。
 * 包含 scheme 标识、network 标识和具体的签名载荷。</p>
 *
 * <p>类比：PaymentPayload ≈ 一张"已签名的支票"</p>
 *
 * <p>JSON 示例（exact 方案 EVM 链）：</p>
 * <pre>{@code
 * {
 *   "scheme": "exact",
 *   "network": "eip155:84532",
 *   "payload": {
 *     "from": "0xBuyerAddress",
 *     "to": "0xSellerAddress",
 *     "value": "1000",
 *     "nonce": "0xabc123...",
 *     "validAfter": 1719000000,
 *     "validBefore": 1719000300,
 *     "signature": "0xSignature..."
 *   }
 * }
 * }</pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentPayload {

    /** 计费方案（与 PaymentRequirement 对应） */
    private String scheme;

    /** 网络标识 */
    private String network;

    /**
     * 具体签名数据。
     * <p>根据 scheme 不同，结构不同：</p>
     * <ul>
     *   <li>exact → {@link ExactEvmPayload}</li>
     *   <li>upto → {@link UptoEvmPayload}</li>
     * </ul>
     */
    private Object payload;

    /** 扩展信息（幂等 Key 等） */
    private Map<String, Object> extensions;

    // ==================== 内部数据结构 ====================

    /**
     * exact 方案的 EVM 签名数据结构。
     * <p>对应 EIP-3009 transferWithAuthorization 的参数。</p>
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExactEvmPayload {
        /** 付款方地址 */
        private String from;

        /** 收款地址 (payTo) */
        private String to;

        /** 金额（原子单位） */
        private String value;

        /** 防重放 nonce（32 字节 hex） */
        private String nonce;

        /** 有效期起始（Unix 时间戳） */
        private long validAfter;

        /** 有效期截止（Unix 时间戳） */
        private long validBefore;

        /** EIP-712 签名 */
        private String signature;
    }

    /**
     * upto 方案的 EVM 签名数据结构。
     * <p>与 exact 类似，但 value 是最大值而非固定值。</p>
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UptoEvmPayload {
        /** 付款方地址 */
        private String from;

        /** 收款地址 */
        private String to;

        /** 最大授权金额（原子单位） */
        private String maxValue;

        /** 防重放 nonce */
        private String nonce;

        /** 有效期起始 */
        private long validAfter;

        /** 有效期截止 */
        private long validBefore;

        /** EIP-712 签名 */
        private String signature;
    }
}
