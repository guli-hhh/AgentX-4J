package com.agentx4j.x402.facilitator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 验证响应。
 *
 * <p>Facilitator 对支付签名验证的返回结果。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyResponse {

    /** 支付是否有效 */
    private boolean valid;

    /** 验证失败原因（valid=false 时） */
    private String invalidReason;

    /** 付款方地址 */
    private String payer;

    /** 验证时间戳 */
    private long verifiedAt;
}
