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
 * Exact Scheme — 固定价格支付方案。
 *
 * <p>流程：</p>
 * <ol>
 *   <li>客户端构建 EIP-3009 transferWithAuthorization 数据</li>
 *   <li>用私钥签名（EIP-712 类型化签名）</li>
 *   <li>Facilitator 验证签名有效性</li>
 *   <li>Facilitator 提交链上交易（EIP-3009 或 Permit2）</li>
 *   <li>等待链上确认（通常 1-3 个区块）</li>
 * </ol>
 *
 * <p>适用场景：费用固定的标准化服务。</p>
 * <p>如：天气查询 $0.001/次、翻译 $0.002/次。</p>
 */
public class ExactScheme implements Scheme {

    private static final Logger log = LoggerFactory.getLogger(ExactScheme.class);

    @Override
    public String getName() {
        return "exact";
    }

    @Override
    public PaymentPayload createPayment(SchemeContext context) {
        PaymentRequirement req = context.getRequirement();
        WalletSigner signer = new EvmWalletSigner(context.getPrivateKey());

        // 1. 生成 nonce (32 字节随机数)
        String nonce = generateNonce();

        // 2. 设置有效期
        long validAfter = System.currentTimeMillis() / 1000;
        long validBefore = validAfter + (req.getMaxTimeoutSeconds() != null
                ? req.getMaxTimeoutSeconds() : SdkConstants.DEFAULT_TIMEOUT_SECONDS);

        // 3. 构建 EIP-3009 授权数据并签名
        String signature = signer.signTransferWithAuthorization(
                context.getFromAddress(),
                req.getPayTo(),
                req.getAmount(),
                nonce,
                validAfter,
                validBefore
        );

        // 4. 构建 PaymentPayload
        PaymentPayload.ExactEvmPayload evmPayload = PaymentPayload.ExactEvmPayload.builder()
                .from(context.getFromAddress())
                .to(req.getPayTo())
                .value(req.getAmount())
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
        if (!(payload.getPayload() instanceof PaymentPayload.ExactEvmPayload)) {
            return VerifyResult.invalid("Invalid payload type for exact scheme");
        }

        PaymentPayload.ExactEvmPayload evmPayload = (PaymentPayload.ExactEvmPayload) payload.getPayload();

        // 4. 验证金额匹配
        if (!evmPayload.getValue().equals(requirement.getAmount())) {
            return VerifyResult.invalid("Amount mismatch: expected " + requirement.getAmount()
                    + ", got " + evmPayload.getValue());
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

        // 7. 验证签名（从签名恢复地址）
        String recoveredAddress = recoverSignerAddress(evmPayload);
        if (recoveredAddress == null) {
            return VerifyResult.invalid("Invalid signature: cannot recover signer address");
        }
        if (!recoveredAddress.equalsIgnoreCase(evmPayload.getFrom())) {
            return VerifyResult.invalid("Signature mismatch: recovered " + recoveredAddress
                    + ", expected " + evmPayload.getFrom());
        }

        log.debug("Local verification passed for exact scheme, payer={}", recoveredAddress);
        return VerifyResult.valid();
    }

    @Override
    public SettleResult settle(PaymentPayload payload, PaymentRequirement requirement) {
        // Exact scheme 的结算由 Facilitator 完成
        log.debug("Exact scheme settlement delegated to facilitator");
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
     *
     * <p>使用 web3j 的 Sign.signedMessageToKey 恢复公钥，然后计算地址。</p>
     *
     * @param payload EVM 支付载荷
     * @return 签名者地址，如果恢复失败则返回 null
     */
    private String recoverSignerAddress(PaymentPayload.ExactEvmPayload payload) {
        try {
            // 重建签名哈希（与签名时一致）
            byte[] typeHash = Numeric.hexStringToByteArray(
                    "0x2c7e94b9071c7cb0b7e3bfe7f7929a0d1b1e45c4c2c1d2e3f4a5b6c7d8e9f0a1b"
            );

            byte[] encoded = abiEncodeForRecovery(
                    typeHash,
                    payload.getFrom(),
                    payload.getTo(),
                    new BigInteger(payload.getValue()),
                    BigInteger.valueOf(payload.getValidAfter()),
                    BigInteger.valueOf(payload.getValidBefore()),
                    Numeric.hexStringToByteArray(payload.getNonce())
            );

            byte[] messageHash = Hash.sha3(encoded);

            // 解析签名
            byte[] signatureBytes = Numeric.hexStringToByteArray(payload.getSignature());
            if (signatureBytes.length != 65) {
                return null;
            }

            byte[] r = new byte[32];
            byte[] s = new byte[32];
            byte v = signatureBytes[64];
            System.arraycopy(signatureBytes, 0, r, 0, 32);
            System.arraycopy(signatureBytes, 32, s, 0, 32);

            // 恢复公钥
            Sign.SignatureData signatureData = new Sign.SignatureData(v, r, s);
            BigInteger publicKey = Sign.signedMessageToKey(messageHash, signatureData);

            // 从公钥计算地址
            String address = Numeric.prependHexPrefix(org.web3j.crypto.Keys.getAddress(publicKey));
            return address;
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
}
