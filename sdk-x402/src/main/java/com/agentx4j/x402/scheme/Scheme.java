package com.agentx4j.x402.scheme;

import com.agentx4j.core.model.PaymentPayload;
import com.agentx4j.core.model.PaymentRequirement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * x402 Payment Scheme — 支付方案接口。
 *
 * <p>不同 scheme (exact/upto/batch-settlement) 有不同实现。
 * 每个 scheme 定义了：</p>
 * <ul>
 *   <li>如何创建支付载荷（客户端）</li>
 *   <li>如何验证支付（服务端/Facilitator）</li>
 *   <li>如何结算（Facilitator）</li>
 * </ul>
 *
 * <p>类比：Scheme ≈ 不同的"支付方式"</p>
 * <ul>
 *   <li>exact = 现金支付（固定金额）</li>
 *   <li>upto = 信用卡预授权（按实际用量）</li>
 *   <li>batch-settlement = 月结账户（批量结算）</li>
 * </ul>
 */
public interface Scheme {

    /** 方案名称: "exact" / "upto" / "batch-settlement" */
    String getName();

    /**
     * 创建支付载荷（客户端调用）。
     * 根据 PaymentRequirement 生成签名的 PaymentPayload。
     *
     * @param context 支付上下文（包含 requirement、signer 等）
     * @return 签名后的支付载荷
     */
    PaymentPayload createPayment(SchemeContext context);

    /**
     * 验证支付载荷（服务端调用）。
     * 检查签名有效性、金额匹配、nonce 未使用等。
     *
     * @param payload     支付载荷
     * @param requirement 支付要求
     * @return 验证结果
     */
    VerifyResult verify(PaymentPayload payload, PaymentRequirement requirement);

    /**
     * 结算支付。
     * 提交链上交易并等待确认。
     *
     * @param payload     支付载荷
     * @param requirement 支付要求
     * @return 结算结果
     */
    SettleResult settle(PaymentPayload payload, PaymentRequirement requirement);

    /** 是否支持该网络 */
    boolean supportsNetwork(String network);

    // ==================== 内部数据结构 ====================

    /**
     * 支付上下文 — 创建支付载荷时需要的所有信息。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class SchemeContext {
        private PaymentRequirement requirement;
        private String fromAddress;
        private byte[] privateKey;
        private String rpcUrl;
    }

    /**
     * 验证结果。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class VerifyResult {
        private boolean valid;
        private String reason;

        public static VerifyResult valid() {
            return VerifyResult.builder().valid(true).build();
        }

        public static VerifyResult invalid(String reason) {
            return VerifyResult.builder().valid(false).reason(reason).build();
        }
    }

    /**
     * 结算结果。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class SettleResult {
        private boolean success;
        private String txHash;
        private Long blockNumber;
        private String error;

        public static SettleResult success(String txHash, Long blockNumber) {
            return SettleResult.builder()
                    .success(true).txHash(txHash).blockNumber(blockNumber).build();
        }

        public static SettleResult failed(String error) {
            return SettleResult.builder().success(false).error(error).build();
        }
    }
}
