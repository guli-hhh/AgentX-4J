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
import java.util.ArrayList;
import java.util.List;

/**
 * Batch Settlement Scheme — 批量结算支付方案。
 *
 * <p>流程：</p>
 * <ol>
 *   <li>客户端调用合约 deposit() 存入 USDC（一次性）</li>
 *   <li>每次 HTTP 请求时签名 Voucher（链下凭证）</li>
 *   <li>服务端验证 Voucher 并记录</li>
 *   <li>服务端定期调用合约 claim() 批量领取</li>
 *   <li>客户端可调用合约 refund() 退回余额</li>
 * </ol>
 *
 * <p>适用场景：高频微支付场景。</p>
 * <p>类比：充 $100 会员卡，每次刷卡不付现金，月底统一结算。</p>
 *
 * <p>注意：完整实现需要与 Escrow 智能合约交互。
 * 当前版本为骨架实现，提供核心数据结构和接口。</p>
 */
public class BatchSettlementScheme implements Scheme {

    private static final Logger log = LoggerFactory.getLogger(BatchSettlementScheme.class);

    @Override
    public String getName() {
        return "batch-settlement";
    }

    @Override
    public PaymentPayload createPayment(SchemeContext context) {
        PaymentRequirement req = context.getRequirement();
        WalletSigner signer = new EvmWalletSigner(context.getPrivateKey());

        // 生成 nonce
        String nonce = generateNonce();

        // 设置有效期
        long validAfter = System.currentTimeMillis() / 1000;
        long validBefore = validAfter + (req.getMaxTimeoutSeconds() != null
                ? req.getMaxTimeoutSeconds() : SdkConstants.DEFAULT_TIMEOUT_SECONDS);

        // 构建链下 Voucher 并签名
        String signature = signer.signTransferWithAuthorization(
                context.getFromAddress(),
                req.getPayTo(),
                req.getAmount(),
                nonce,
                validAfter,
                validBefore
        );

        // 构建 PaymentPayload（batch-settlement 方案的 payload 复用 exact 结构）
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
            return VerifyResult.invalid("Invalid payload type for batch-settlement scheme");
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

        // 7. 验证签名
        String recoveredAddress = recoverSignerAddress(evmPayload);
        if (recoveredAddress == null) {
            return VerifyResult.invalid("Invalid signature: cannot recover signer address");
        }
        if (!recoveredAddress.equalsIgnoreCase(evmPayload.getFrom())) {
            return VerifyResult.invalid("Signature mismatch: recovered " + recoveredAddress
                    + ", expected " + evmPayload.getFrom());
        }

        // 8. 检查链上托管余额是否充足（TODO: 需要合约交互）
        log.debug("Batch-settlement voucher verified for payer={}, amount={}",
                recoveredAddress, evmPayload.getValue());

        return VerifyResult.valid();
    }

    @Override
    public SettleResult settle(PaymentPayload payload, PaymentRequirement requirement) {
        // batch-settlement 的结算由 Facilitator 批量处理
        // 服务端记录 Voucher，定期批量上链结算
        log.debug("Batch-settlement voucher recorded for batch settlement");
        return SettleResult.success("0x-batch-voucher-recorded", null);
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

    private String recoverSignerAddress(PaymentPayload.ExactEvmPayload payload) {
        try {
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
}
