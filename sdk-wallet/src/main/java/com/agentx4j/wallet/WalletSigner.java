package com.agentx4j.wallet;

import java.util.Map;

/**
 * 钱包签名器 — 用于签名支付载荷。
 *
 * <p>封装了不同链的签名逻辑：</p>
 * <ul>
 *   <li>EVM: ECDSA (secp256k1) 签名，支持 EIP-712 / EIP-3009</li>
 *   <li>Solana: Ed25519 签名（后续实现）</li>
 * </ul>
 *
 * <p>类比：WalletSigner ≈ "电子签名笔"，用它签名的数据具有密码学效力。</p>
 */
public interface WalletSigner {

    /**
     * 签名 EIP-3009 TransferWithAuthorization 数据。
     *
     * @param from      付款方地址
     * @param to        收款地址
     * @param value     金额（原子单位）
     * @param nonce     防重放 nonce（32 字节 hex）
     * @param validAfter 有效期起始（Unix 时间戳）
     * @param validBefore 有效期截止（Unix 时间戳）
     * @return EIP-712 签名（hex 格式，65 字节）
     */
    String signTransferWithAuthorization(String from, String to, String value,
                                          String nonce, long validAfter, long validBefore);

    /**
     * 签名 EIP-712 类型化数据。
     *
     * @param domain   EIP-712 域分隔符
     * @param types    类型定义
     * @param message  待签名消息
     * @return 签名（hex 格式）
     */
    String signTypedData(Map<String, Object> domain, Map<String, Object> types, Map<String, Object> message);

    /**
     * 获取钱包地址。
     *
     * @return 钱包地址（0x 开头）
     */
    String getAddress();

    /**
     * 获取公钥。
     *
     * @return 公钥（hex 格式）
     */
    String getPublicKey();
}
