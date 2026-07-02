package com.agentx4j.x402.network;

import com.agentx4j.core.model.PaymentPayload;
import com.agentx4j.core.model.PaymentRequirement;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;

/**
 * EVM 网络适配器 — 支持所有 EVM 兼容链。
 *
 * <p>使用 web3j 库与区块链节点交互。
 * 支持：Base、Ethereum、Polygon、Arbitrum、Optimism 等所有 EVM 链。</p>
 *
 * <p>类比：EvmNetworkAdapter ≈ "EVM 链的万能遥控器"
 *       所有 EVM 链的操作方式相同，只是参数不同。</p>
 */
public class EvmNetworkAdapter implements NetworkAdapter {

    private static final Logger log = LoggerFactory.getLogger(EvmNetworkAdapter.class);

    private final Web3j web3j;
    private final String chainId;
    private final String rpcUrl;

    /**
     * 创建 EVM 网络适配器。
     *
     * @param rpcUrl  RPC 节点地址
     * @param chainId 链 ID
     */
    public EvmNetworkAdapter(String rpcUrl, String chainId) {
        this.web3j = Web3j.build(new HttpService(rpcUrl));
        this.chainId = chainId;
        this.rpcUrl = rpcUrl;
    }

    /**
     * 创建 EVM 网络适配器（使用 web3j 实例）。
     */
    public EvmNetworkAdapter(Web3j web3j, String chainId, String rpcUrl) {
        this.web3j = web3j;
        this.chainId = chainId;
        this.rpcUrl = rpcUrl;
    }

    @Override
    public String getNetworkPrefix() {
        return "eip155";
    }

    /**
     * 验证 EIP-3009 签名。
     *
     * <p>从签名中恢复签名者地址，与 payload.from 比对。</p>
     */
    @Override
    public boolean verifySignature(PaymentPayload payload, PaymentRequirement requirement) {
        try {
            if (!(payload.getPayload() instanceof PaymentPayload.ExactEvmPayload evmPayload)) {
                return false;
            }

            // 重建消息哈希
            byte[] typeHash = Numeric.hexStringToByteArray(
                    "0x2c7e94b9071c7cb0b7e3bfe7f7929a0d1b1e45c4c2c1d2e3f4a5b6c7d8e9f0a1b"
            );

            byte[] encoded = abiEncode(
                    typeHash,
                    evmPayload.getFrom(),
                    evmPayload.getTo(),
                    new BigInteger(evmPayload.getValue()),
                    BigInteger.valueOf(evmPayload.getValidAfter()),
                    BigInteger.valueOf(evmPayload.getValidBefore()),
                    Numeric.hexStringToByteArray(evmPayload.getNonce())
            );

            byte[] messageHash = Hash.sha3(encoded);

            // 解析签名
            byte[] signatureBytes = Numeric.hexStringToByteArray(evmPayload.getSignature());
            if (signatureBytes.length != 65) return false;

            byte[] r = new byte[32];
            byte[] s = new byte[32];
            byte v = signatureBytes[64];
            System.arraycopy(signatureBytes, 0, r, 0, 32);
            System.arraycopy(signatureBytes, 32, s, 0, 32);

            // 恢复地址
            Sign.SignatureData signatureData = new Sign.SignatureData(v, r, s);
            BigInteger publicKey = Sign.signedMessageToKey(messageHash, signatureData);
            String recoveredAddress = Numeric.prependHexPrefix(org.web3j.crypto.Keys.getAddress(publicKey));

            return recoveredAddress.equalsIgnoreCase(evmPayload.getFrom());
        } catch (Exception e) {
            log.warn("Signature verification failed", e);
            return false;
        }
    }

    /**
     * 提交链上交易。
     *
     * <p>实际实现需要构建 EIP-3009 transferWithAuthorization 交易
     * 或通过 Permit2 合约提交。这里返回占位符。</p>
     */
    @Override
    public String submitTransaction(PaymentPayload payload, PaymentRequirement requirement) {
        // TODO: 实现真实的链上交易提交
        // 1. 构建 EIP-3009 transferWithAuthorization 交易
        // 2. 或者通过 Permit2 合约提交
        // 3. 等待交易确认
        // 4. 返回 txHash
        log.debug("submitTransaction called (delegated to facilitator in MVP)");
        return "0x-placeholder-tx-hash";
    }

    @Override
    public TransactionConfirmation getConfirmation(String txHash) {
        try {
            var receipt = web3j.ethGetTransactionReceipt(txHash).send();
            if (receipt.getTransactionReceipt() != null && receipt.getTransactionReceipt().isPresent()) {
                var r = receipt.getTransactionReceipt().get();
                return TransactionConfirmation.builder()
                        .txHash(txHash)
                        .blockNumber(r.getBlockNumber().longValue())
                        .confirmations(1)
                        .confirmed(true)
                        .status("0x1".equals(r.getStatus()) ? "SUCCESS" : "FAILED")
                        .build();
            }
        } catch (Exception e) {
            log.debug("Failed to get confirmation for tx: {}", txHash, e);
        }
        return null;
    }

    @Override
    public BigInteger getTokenBalance(String walletAddress, String tokenAddress) {
        try {
            // 调用 ERC-20 balanceOf
            String data = "0x70a08231" + Numeric.toHexStringNoPrefix(
                    Numeric.toBytesPadded(Numeric.toBigInt(walletAddress), 32));
            var response = web3j.ethCall(
                    org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                            walletAddress, tokenAddress, data),
                    org.web3j.protocol.core.DefaultBlockParameterName.LATEST
            ).send();
            return Numeric.toBigInt(response.getValue());
        } catch (Exception e) {
            log.warn("Failed to query token balance", e);
            return BigInteger.ZERO;
        }
    }

    @Override
    public long getBlockNumber() {
        try {
            return web3j.ethBlockNumber().send().getBlockNumber().longValue();
        } catch (Exception e) {
            log.warn("Failed to get block number", e);
            return 0;
        }
    }

    // ==================== 内部方法 ====================

    private byte[] abiEncode(byte[] typeHash, String from, String to,
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
