package com.agentx4j.x402.server;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 验证结果。
 *
 * <p>Resource Server 验证支付后的返回结果。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyResult {

    /** 支付是否有效 */
    private boolean valid;

    /** 验证失败原因 */
    private String reason;

    /** 付款方地址（验证成功时） */
    private String payer;

    public static VerifyResult valid(String payer) {
        return VerifyResult.builder().valid(true).payer(payer).build();
    }

    public static VerifyResult invalid(String reason) {
        return VerifyResult.builder().valid(false).reason(reason).build();
    }

    public static VerifyResult unsupported(String reason) {
        return VerifyResult.builder().valid(false).reason(reason).build();
    }
}
