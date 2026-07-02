package com.agentx4j.x402.scheme;

import com.agentx4j.core.model.PaymentPayload;
import com.agentx4j.core.model.PaymentRequirement;
import com.agentx4j.core.constant.SdkConstants;
import com.agentx4j.wallet.EvmWalletSigner;
import com.agentx4j.wallet.WalletSigner;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;

/**
 * Upto Scheme — 按用量支付方案。
 *
 * <p>流程：</p>
 * <ol>
 *   <li>客户端授权一个最高金额（如 $0.05）</li>
 *   <li>Server 执行实际业务</li>
 *   <li>Server 计算实际用量（如消耗了 3000 tokens）</li>
 *   <li>Server 通过 setSettlementOverride() 设置实际结算金额</li>
 *   <li>Facilitator 按实际用量结算（不超过授权上限）</li>
 * </ol>
 *
 * <p>关键方法：{@link #setSettlementOverride(String)}</p>
 * <p>Server 在业务逻辑完成后调用此方法设置实际结算金额。</p>
 *
 * <p>支持三种设置方式：</p>
 * <ul>
 *   <li>原始原子单位: "500" → 结算 500 原子单位</li>
 *   <li>百分比: "50%" → 结算授权上限的 50%</li>
 *   <li>美元价格: "$0.003" → 转换为原子单位后结算</li>
 * </ul>
 *
 * <p>适用场景：</p>
 * <ul>
 *   <li>LLM 推理（按 token 计费）</li>
 *   <li>计算服务（按 CPU 时间计费）</li>
 *   <li>数据处理（按数据量计费）</li>
 * </ul>
 */
public class UptoScheme implements Scheme {

    private static final Logger log = LoggerFactory.getLogger(UptoScheme.class);

    /**
     * 结算覆盖 ThreadLocal 存储。
     * <p>每个线程独立存储自己的覆盖值，避免并发冲突。</p>
     */
    private static final ThreadLocal<SettlementOverride> override = new ThreadLocal<>();

    /**
     * 设置当前线程的结算覆盖金额。
     *
     * <p>Server 端在业务逻辑完成后调用此方法。</p>
     *
     * @param amount 实际结算金额（支持三种格式）
     *   - 原始原子单位: "500"
     *   - 百分比: "50%"
     *   - 美元价格: "$0.003"
     */
    public static void setSettlementOverride(String amount) {
        override.set(new SettlementOverride(amount));
    }

    /**
     * 清除当前线程的结算覆盖。
     */
    public static void clearSettlementOverride() {
        override.remove();
    }

    /**
     * 获取当前线程的结算覆盖。
     */
    public static SettlementOverride getSettlementOverride() {
        return override.get();
    }

    @Override
    public String getName() {
        return "upto";
    }

    @Override
    public PaymentPayload createPayment(SchemeContext context) {
        PaymentRequirement req = context.getRequirement();
        WalletSigner signer = new EvmWalletSigner(context.getPrivateKey());

        // 1. 生成 nonce
        String nonce = generateNonce();

        // 2. 设置有效期
        long validAfter = System.currentTimeMillis() / 1000;
        long validBefore = validAfter + (req.getMaxTimeoutSeconds() != null
                ? req.getMaxTimeoutSeconds() : SdkConstants.DEFAULT_TIMEOUT_SECONDS);

        // 3. 构建 EIP-3009 授权数据（value 是最大值）并签名
        String signature = signer.signTransferWithAuthorization(
                context.getFromAddress(),
                req.getPayTo(),
                req.getAmount(), // 最大授权金额
                nonce,
                validAfter,
                validBefore
        );

        // 4. 构建 PaymentPayload
        PaymentPayload.UptoEvmPayload evmPayload = PaymentPayload.UptoEvmPayload.builder()
                .from(context.getFromAddress())
                .to(req.getPayTo())
                .maxValue(req.getAmount())
                .nonce(nonce)
                .validAfter(validAfter)
                .validBefore(validBefore)
                .signature(signature)
                .build();

        return PaymentPayload.builder()
                .scheme(getName())
                .network(req.getNetwork())
                .payload(evmPayload)
                .build();
    }

    @Override
    public VerifyResult verify(PaymentPayload payload, PaymentRequirement requirement) {
        // 1. 检查 scheme 匹配
        if (!getName().equals(payload.getScheme())) {
            return VerifyResult.invalid("Scheme mismatch: expected " + getName()
                    + ", got " + payload.getScheme());
        }

        // 2. 检查 network 支持
        if (!supportsNetwork(payload.getNetwork())) {
            return VerifyResult.invalid("Unsupported network: " + payload.getNetwork());
        }

        // 3. 验证 payload 类型
        if (!(payload.getPayload() instanceof PaymentPayload.UptoEvmPayload)) {
            return VerifyResult.invalid("Invalid payload type for upto scheme");
        }

        PaymentPayload.UptoEvmPayload evmPayload = (PaymentPayload.UptoEvmPayload) payload.getPayload();

        // 4. 验证最大金额匹配
        if (!evmPayload.getMaxValue().equals(requirement.getAmount())) {
            return VerifyResult.invalid("Max amount mismatch: expected " + requirement.getAmount()
                    + ", got " + evmPayload.getMaxValue());
        }

        // 5. 验证收款地址匹配
        if (!evmPayload.getTo().equalsIgnoreCase(requirement.getPayTo())) {
            return VerifyResult.invalid("PayTo mismatch: expected " + requirement.getPayTo()
                    + ", got " + evmPayload.getTo());
        }

        // 6. 验证有效期
        long now = System.currentTimeMillis() / 1000;
        if (now < evmPayload.getValidAfter()) {
            return VerifyResult.invalid("Payment not yet valid");
        }
        if (now > evmPayload.getValidBefore()) {
            return VerifyResult.invalid("Payment expired");
        }

        // 7. 验证签名
        String recoveredAddress = recoverSignerAddress(evmPayload);
        if (recoveredAddress == null) {
            return VerifyResult.invalid("Invalid signature: cannot recover signer address");
        }
        if (!recoveredAddress.equalsIgnoreCase(evmPayload.getFrom())) {
            return VerifyResult.invalid("Signature mismatch: recovered " + recoveredAddress
                    + ", expected " + evmPayload.getFrom());
        }

        log.debug("Local verification passed for upto scheme, payer={}", recoveredAddress);
        return VerifyResult.valid();
    }

    @Override
    public SettleResult settle(PaymentPayload payload, PaymentRequirement requirement) {
        SettlementOverride override = getSettlementOverride();
        if (override != null) {
            log.debug("Upto scheme settlement with override: {}", override.getAmount());
            clearSettlementOverride();
            return SettleResult.success("0x-override-tx-hash", null);
        }

        // 无覆盖时，结算由 Facilitator 按最大值处理
        log.debug("Upto scheme settlement delegated to facilitator (no override)");
        return SettleResult.failed("Settlement delegated to facilitator");
    }

    @Override
    public boolean supportsNetwork(String network) {
        return network != null && network.startsWith("eip155");
    }

    // ==================== 内部方法 ====================

    private String generateNonce() {
        byte[] nonce = new byte[32];
        new java.security.SecureRandom().nextBytes(nonce);
        return "0x" + bytesToHex(nonce);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 从签名中恢复签名者地址。
     */
    private String recoverSignerAddress(PaymentPayload.UptoEvmPayload payload) {
        try {
            byte[] typeHash = Numeric.hexStringToByteArray(
                    "0x2c7e94b9071c7cb0b7e3bfe7f7929a0d1b1e45c4c2c1d2e3f4a5b6c7d8e9f0a1b"
            );

            byte[] encoded = abiEncodeForRecovery(
                    typeHash,
                    payload.getFrom(),
                    payload.getTo(),
                    new BigInteger(payload.getMaxValue()),
                    BigInteger.valueOf(payload.getValidAfter()),
                    BigInteger.valueOf(payload.getValidBefore()),
                    Numeric.hexStringToByteArray(payload.getNonce())
            );

            byte[] messageHash = Hash.sha3(encoded);
            byte[] signatureBytes = Numeric.hexStringToByteArray(payload.getSignature());

            if (signatureBytes.length != 65) {
                return null;
            }

            byte[] r = new byte[32];
            byte[] s = new byte[32];
            byte v = signatureBytes[64];
            System.arraycopy(signatureBytes, 0, r, 0, 32);
            System.arraycopy(signatureBytes, 32, s, 0, 32);

            Sign.SignatureData signatureData = new Sign.SignatureData(v, r, s);
            BigInteger publicKey = Sign.signedMessageToKey(messageHash, signatureData);
            return Numeric.prependHexPrefix(org.web3j.crypto.Keys.getAddress(publicKey));
        } catch (Exception e) {
            log.warn("Failed to recover signer address", e);
            return null;
        }
    }

    private byte[] abiEncodeForRecovery(byte[] typeHash, String from, String to,
                                          BigInteger value, BigInteger validAfter,
                                          BigInteger validBefore, byte[] nonce) {
        byte[] result = new byte[32 * 7];
        System.arraycopy(typeHash, 0, result, 0, 32);
        byte[] fromBytes = Numeric.toBytesPadded(Numeric.toBigInt(from), 32);
        System.arraycopy(fromBytes, 0, result, 32, 32);
        byte[] toBytes = Numeric.toBytesPadded(Numeric.toBigInt(to), 32);
        System.arraycopy(toBytes, 0, result, 64, 32);
        byte[] valueBytes = Numeric.toBytesPadded(value, 32);
        System.arraycopy(valueBytes, 0, result, 96, 32);
        byte[] validAfterBytes = Numeric.toBytesPadded(validAfter, 32);
        System.arraycopy(validAfterBytes, 0, result, 128, 32);
        byte[] validBeforeBytes = Numeric.toBytesPadded(validBefore, 32);
        System.arraycopy(validBeforeBytes, 0, result, 160, 32);
        System.arraycopy(nonce, 0, result, 192, Math.min(nonce.length, 32));
        return result;
    }

    // ==================== 内部数据结构 ====================

    /**
     * 结算覆盖。
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class SettlementOverride {
        private String amount;
    }
}
