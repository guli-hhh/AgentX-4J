package com.agentx4j.wallet;

import com.agentx4j.core.enums.NetworkType;
import com.agentx4j.core.model.WalletInfo;
import lombok.Getter;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Keys;
import org.web3j.crypto.WalletUtils;

import java.math.BigInteger;
import java.util.UUID;

/**
 * EVM 钱包实体。
 *
 * <p>封装了以太坊兼容链（Base、Ethereum、Polygon 等）的钱包操作。
 * 支持创建新钱包、导入已有钱包、查询余额、签名等操作。</p>
 *
 * <p>类比：EVMWallet ≈ 你的"数字钱包"，里面装着私钥和地址。</p>
 */
@Getter
public class EvmWallet {

    private final String walletId;
    private final WalletSigner signer;
    private final String address;

    /**
     * 创建新钱包（生成新的密钥对）。
     *
     * @return 新创建的钱包
     */
    public static EvmWallet create() {
        try {
            Credentials credentials = Credentials.create(Keys.createEcKeyPair());
            String walletId = UUID.randomUUID().toString();
            return new EvmWallet(walletId, new EvmWalletSigner(credentials));
        } catch (Exception e) {
            throw new RuntimeException("Failed to create wallet", e);
        }
    }

    /**
     * 从私钥导入钱包。
     *
     * @param privateKey 私钥（0x 开头的 hex 字符串）
     * @return 导入的钱包
     */
    public static EvmWallet fromPrivateKey(String privateKey) {
        String walletId = UUID.randomUUID().toString();
        return new EvmWallet(walletId, new EvmWalletSigner(privateKey));
    }

    /**
     * 从私钥导入钱包（指定 walletId）。
     *
     * @param walletId   钱包 ID
     * @param privateKey 私钥
     * @return 导入的钱包
     */
    public static EvmWallet fromPrivateKey(String walletId, String privateKey) {
        return new EvmWallet(walletId, new EvmWalletSigner(privateKey));
    }

    private EvmWallet(String walletId, WalletSigner signer) {
        this.walletId = walletId;
        this.signer = signer;
        this.address = signer.getAddress();
    }

    /**
     * 转换为 WalletInfo 模型。
     *
     * @param agentId 所属 Agent ID
     * @return WalletInfo
     */
    public WalletInfo toWalletInfo(String agentId) {
        return WalletInfo.builder()
                .walletId(walletId)
                .agentId(agentId)
                .network(NetworkType.EVM)
                .address(address)
                .label("EVM Wallet")
                .build();
    }

    /**
     * 获取钱包地址。
     *
     * @return 钱包地址（0x 开头，EIP-55 校验和格式）
     */
    public String getAddress() {
        return address;
    }

    /**
     * 获取底层签名器。
     *
     * @return 签名器
     */
    public WalletSigner getSigner() {
        return signer;
    }
}
