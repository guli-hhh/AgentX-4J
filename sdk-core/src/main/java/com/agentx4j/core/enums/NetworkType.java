package com.agentx4j.core.enums;

/**
 * 区块链网络类型枚举。
 *
 * <p>使用 CAIP-2 标准的 namespace 部分作为标识。</p>
 * <p>CAIP-2 格式：{@code namespace:reference}，如 {@code eip155:8453} 表示 Base 主网。</p>
 */
public enum NetworkType {

    /**
     * EVM 兼容链（Base、Ethereum、Polygon、Arbitrum 等）。
     * <p>CAIP-2 前缀：{@code eip155}</p>
     */
    EVM("eip155", "EVM compatible chains (Base/Ethereum/Polygon/Arbitrum/...)"),

    /**
     * Solana 区块链。
     * <p>CAIP-2 前缀：{@code solana}</p>
     */
    SVM("solana", "Solana"),

    /**
     * TON（The Open Network）区块链。
     * <p>CAIP-2 前缀：{@code tvm}</p>
     */
    TVM("tvm", "TON (The Open Network)"),

    /**
     * Algorand 区块链。
     * <p>CAIP-2 前缀：{@code algorand}</p>
     */
    AVM("algorand", "Algorand"),

    /**
     * Stellar 区块链。
     * <p>CAIP-2 前缀：{@code stellar}</p>
     */
    STELLAR("stellar", "Stellar"),

    /**
     * Aptos 区块链。
     * <p>CAIP-2 前缀：{@code aptos}</p>
     */
    APTOS("aptos", "Aptos"),

    /**
     * Hedera 区块链。
     * <p>CAIP-2 前缀：{@code hedera}</p>
     */
    HEDERA("hedera", "Hedera"),

    /**
     * Keeta 区块链。
     * <p>CAIP-2 前缀：{@code keeta}</p>
     */
    KEETA("keeta", "Keeta"),

    /**
     * Concordium 区块链。
     * <p>CAIP-2 前缀：{@code ccd}</p>
     */
    CONCORDIUM("ccd", "Concordium");

    private final String caip2Prefix;
    private final String description;

    NetworkType(String caip2Prefix, String description) {
        this.caip2Prefix = caip2Prefix;
        this.description = description;
    }

    public String getCaip2Prefix() {
        return caip2Prefix;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据 CAIP-2 完整标识符获取网络类型。
     * <p>例如：{@code "eip155:8453"} → {@link #EVM}</p>
     *
     * @param caip2Id CAIP-2 标识符（如 "eip155:8453"）
     * @return 对应的网络类型
     */
    public static NetworkType fromCaip2Id(String caip2Id) {
        if (caip2Id == null || !caip2Id.contains(":")) {
            throw new IllegalArgumentException("Invalid CAIP-2 identifier: " + caip2Id);
        }
        String prefix = caip2Id.split(":")[0];
        for (NetworkType type : values()) {
            if (type.caip2Prefix.equals(prefix)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown network type for CAIP-2: " + caip2Id);
    }
}
