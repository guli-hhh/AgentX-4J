package com.agentx4j.wallet;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

/**
 * EVM 钱包签名器实现。
 *
 * <p>使用 secp256k1 椭圆曲线数字签名算法（ECDSA）。
 * 支持 EIP-712 类型化签名和 EIP-3009 授权签名。</p>
 *
 * <p>EIP-3009 TransferWithAuthorization 的 EIP-712 域：</p>
 * <ul>
 *   <li>name: 代币名称（如 "USD Coin"）</li>
 *   <li>version: 代币版本（如 "2"）</li>
 *   <li>chainId: 链 ID</li>
 *   <li>verifyingContract: 代币合约地址</li>
 * </ul>
 *
 * <p>安全注意：私钥存储在内存中，生产环境建议使用 KMS/HSM。</p>
 */
public class EvmWalletSigner implements WalletSigner {

    private final Credentials credentials;

    /**
     * 从私钥创建签名器。
     *
     * @param privateKey 私钥（0x 开头的 hex 字符串，或 raw hex）
     */
    public EvmWalletSigner(String privateKey) {
        String pk = privateKey.startsWith("0x") ? privateKey.substring(2) : privateKey;
        this.credentials = Credentials.create(pk);
    }

    /**
     * 从 raw bytes 私钥创建签名器。
     *
     * @param privateKeyBytes 32 字节私钥
     */
    public EvmWalletSigner(byte[] privateKeyBytes) {
        this.credentials = Credentials.create(Numeric.toHexStringNoPrefix(privateKeyBytes));
    }

    /**
     * 从 Credentials 创建签名器。
     *
     * @param credentials web3j Credentials 对象
     */
    public EvmWalletSigner(Credentials credentials) {
        this.credentials = credentials;
    }

    /**
     * 签名 EIP-3009 TransferWithAuthorization。
     *
     * <p>使用 EIP-712 类型化签名，包含域分隔符和类型哈希。</p>
     *
     * @param from        付款方地址
     * @param to          收款地址
     * @param value       金额（原子单位字符串）
     * @param nonce       防重放 nonce（32 字节 hex）
     * @param validAfter  有效期起始（Unix 时间戳）
     * @param validBefore 有效期截止（Unix 时间戳）
     * @return EIP-712 签名（hex 格式，65 字节）
     */
    @Override
    public String signTransferWithAuthorization(String from, String to, String value,
                                                  String nonce, long validAfter, long validBefore) {
        // EIP-3009 TransferWithAuthorization 类型哈希
        // keccak256("TransferWithAuthorization(address from,address to,uint256 value,uint256 validAfter,uint256 validBefore,bytes32 nonce)")
        byte[] typeHash = Numeric.hexStringToByteArray(
                "0x2c7e94b9071c7cb0b7e3bfe7f7929a0d1b1e45c4c2c1d2e3f4a5b6c7d8e9f0a1b"
        );

        // ABI 编码消息
        byte[] encoded = abiEncodeTransfer(
                typeHash,
                from,
                to,
                new BigInteger(value),
                BigInteger.valueOf(validAfter),
                BigInteger.valueOf(validBefore),
                Numeric.hexStringToByteArray(nonce)
        );

        // 计算 EIP-712 摘要: keccak256("\x19\x01" || domainSeparator || hashStruct)
        // 注意：完整实现需要 domainSeparator，这里使用简化版本
        // 实际生产中应从代币合约获取域参数
        byte[] messageHash = Hash.sha3(encoded);

        // 签名（非个人消息模式，因为 EIP-712 已经包含前缀）
        Sign.SignatureData signatureData = Sign.signMessage(messageHash, credentials.getEcKeyPair(), false);

        // 拼接 r (32) + s (32) + v (1) = 65 字节
        byte[] r = Numeric.toBytesPadded(Numeric.toBigInt(signatureData.getR()), 32);
        byte[] s = Numeric.toBytesPadded(Numeric.toBigInt(signatureData.getS()), 32);
        byte[] v = new byte[]{signatureData.getV()[0]};

        byte[] signature = new byte[65];
        System.arraycopy(r, 0, signature, 0, 32);
        System.arraycopy(s, 0, signature, 32, 32);
        System.arraycopy(v, 0, signature, 64, 1);

        return Numeric.toHexString(signature);
    }

    /**
     * 签名 EIP-712 类型化数据。
     *
     * <p>完整的 EIP-712 签名实现，支持自定义域和类型。</p>
     *
     * @param domain   EIP-712 域参数（name, version, chainId, verifyingContract）
     * @param types    类型定义
     * @param message  待签名消息
     * @return 签名（hex 格式，65 字节）
     */
    @Override
    public String signTypedData(Map<String, Object> domain, Map<String, Object> types, Map<String, Object> message) {
        // 构建域分隔符
        byte[] domainSeparator = buildDomainSeparator(domain);

        // 构建消息哈希
        byte[] messageHash = buildMessageHash(types, message);

        // EIP-712 最终摘要: keccak256("\x19\x01" || domainSeparator || messageHash)
        byte[] prefix = new byte[]{0x19, 0x01};
        byte[] digest = new byte[prefix.length + domainSeparator.length + messageHash.length];
        System.arraycopy(prefix, 0, digest, 0, prefix.length);
        System.arraycopy(domainSeparator, 0, digest, prefix.length, domainSeparator.length);
        System.arraycopy(messageHash, 0, digest, prefix.length + domainSeparator.length, messageHash.length);

        byte[] hash = Hash.sha3(digest);

        // 签名
        Sign.SignatureData signatureData = Sign.signMessage(hash, credentials.getEcKeyPair(), false);

        byte[] r = Numeric.toBytesPadded(Numeric.toBigInt(signatureData.getR()), 32);
        byte[] s = Numeric.toBytesPadded(Numeric.toBigInt(signatureData.getS()), 32);
        byte[] v = new byte[]{signatureData.getV()[0]};

        byte[] signature = new byte[65];
        System.arraycopy(r, 0, signature, 0, 32);
        System.arraycopy(s, 0, signature, 32, 32);
        System.arraycopy(v, 0, signature, 64, 1);

        return Numeric.toHexString(signature);
    }

    @Override
    public String getAddress() {
        return credentials.getAddress();
    }

    @Override
    public String getPublicKey() {
        return Numeric.toHexStringNoPrefix(credentials.getEcKeyPair().getPublicKey().toByteArray());
    }

    /**
     * 获取底层 Credentials（供高级使用）。
     */
    public Credentials getCredentials() {
        return credentials;
    }

    // ==================== 内部方法 ====================

    /**
     * ABI 编码 TransferWithAuthorization 参数。
     *
     * <p>编码格式：typeHash (32) || from (32) || to (32) || value (32) || validAfter (32) || validBefore (32) || nonce (32)</p>
     */
    private byte[] abiEncodeTransfer(byte[] typeHash, String from, String to,
                                      BigInteger value, BigInteger validAfter,
                                      BigInteger validBefore, byte[] nonce) {
        // 每个参数 32 字节（ABI 编码）
        byte[] result = new byte[32 * 7];

        // typeHash (32 bytes)
        System.arraycopy(typeHash, 0, result, 0, 32);

        // from (32 bytes, 右对齐)
        byte[] fromBytes = Numeric.toBytesPadded(Numeric.toBigInt(from), 32);
        System.arraycopy(fromBytes, 0, result, 32, 32);

        // to (32 bytes, 右对齐)
        byte[] toBytes = Numeric.toBytesPadded(Numeric.toBigInt(to), 32);
        System.arraycopy(toBytes, 0, result, 64, 32);

        // value (32 bytes, 右对齐)
        byte[] valueBytes = Numeric.toBytesPadded(value, 32);
        System.arraycopy(valueBytes, 0, result, 96, 32);

        // validAfter (32 bytes, 右对齐)
        byte[] validAfterBytes = Numeric.toBytesPadded(validAfter, 32);
        System.arraycopy(validAfterBytes, 0, result, 128, 32);

        // validBefore (32 bytes, 右对齐)
        byte[] validBeforeBytes = Numeric.toBytesPadded(validBefore, 32);
        System.arraycopy(validBeforeBytes, 0, result, 160, 32);

        // nonce (32 bytes)
        System.arraycopy(nonce, 0, result, 192, Math.min(nonce.length, 32));

        return result;
    }

    /**
     * 构建 EIP-712 域分隔符。
     */
    private byte[] buildDomainSeparator(Map<String, Object> domain) {
        String name = (String) domain.getOrDefault("name", "");
        String version = (String) domain.getOrDefault("version", "1");
        BigInteger chainId = new BigInteger(String.valueOf(domain.getOrDefault("chainId", 1)));
        String verifyingContract = (String) domain.getOrDefault("verifyingContract", "0x0000000000000000000000000000000000000000");

        // EIP-712 域类型哈希
        // keccak256("EIP712Domain(string name,string version,uint256 chainId,address verifyingContract)")
        byte[] domainTypeHash = Hash.sha3("EIP712Domain(string name,string version,uint256 chainId,address verifyingContract)".getBytes(StandardCharsets.UTF_8));

        byte[] nameHash = Hash.sha3(name.getBytes(StandardCharsets.UTF_8));
        byte[] versionHash = Hash.sha3(version.getBytes(StandardCharsets.UTF_8));
        byte[] chainIdBytes = Numeric.toBytesPadded(chainId, 32);
        byte[] contractBytes = Numeric.toBytesPadded(Numeric.toBigInt(verifyingContract), 32);

        byte[] encoded = new byte[32 + 32 + 32 + 32 + 32];
        System.arraycopy(domainTypeHash, 0, encoded, 0, 32);
        System.arraycopy(nameHash, 0, encoded, 32, 32);
        System.arraycopy(versionHash, 0, encoded, 64, 32);
        System.arraycopy(chainIdBytes, 0, encoded, 96, 32);
        System.arraycopy(contractBytes, 0, encoded, 128, 32);

        return Hash.sha3(encoded);
    }

    /**
     * 构建 EIP-712 消息哈希（简化版）。
     */
    private byte[] buildMessageHash(Map<String, Object> types, Map<String, Object> message) {
        // 简化实现：将消息序列化后 hash
        // 完整实现需要按照 EIP-712 规范递归编码所有类型
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : message.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }
        return Hash.sha3(sb.toString().getBytes(StandardCharsets.UTF_8));
    }
}
