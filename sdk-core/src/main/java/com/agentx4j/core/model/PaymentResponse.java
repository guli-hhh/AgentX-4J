package com.agentx4j.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * x402 PaymentResponse — 支付响应（结算凭证）。
 *
 * <p>Resource Server 在成功结算后返回的凭证，证明交易已完成。
 * 包含在 HTTP 响应的 PAYMENT-RESPONSE header 中（Base64 编码）。</p>
 *
 * <p>类比：PaymentResponse ≈ 商家的"收款凭证/发票"</p>
 *
 * <p>JSON 示例：</p>
 * <pre>{@code
 * {
 *   "success": true,
 *   "txHash": "0xabc123...",
 *   "network": "eip155:84532",
 *   "amount": "1000"
 * }
 * }</pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    /** 结算是否成功 */
    private boolean success;

    /** 链上交易哈希 */
    private String txHash;

    /** 网络标识 */
    private String network;

    /** 实际结算金额（原子单位） */
    private String amount;

    /** 区块号（可选） */
    private Long blockNumber;

    /** 失败原因（success=false 时） */
    private String error;
}
