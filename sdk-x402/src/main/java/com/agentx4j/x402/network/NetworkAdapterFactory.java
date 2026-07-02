package com.agentx4j.x402.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 网络适配器工厂。
 *
 * <p>管理所有已注册的 NetworkAdapter 实例。
 * 根据网络标识符查找对应的适配器。</p>
 */
public class NetworkAdapterFactory {

    private static final Logger log = LoggerFactory.getLogger(NetworkAdapterFactory.class);

    private final Map<String, NetworkAdapter> adapters = new ConcurrentHashMap<>();

    /**
     * 注册网络适配器。
     *
     * @param network 网络匹配模式（如 "eip155:*" 或 "solana:mainnet"）
     * @param adapter 适配器实现
     */
    public void register(String network, NetworkAdapter adapter) {
        adapters.put(network, adapter);
        log.info("Registered network adapter for: {}", network);
    }

    /**
     * 根据 CAIP-2 网络标识符查找适配器。
     *
     * @param network CAIP-2 标识符（如 "eip155:84532"）
     * @return 对应的适配器，如果未找到则返回 null
     */
    public NetworkAdapter find(String network) {
        if (network == null) return null;

        // 精确匹配
        NetworkAdapter adapter = adapters.get(network);
        if (adapter != null) return adapter;

        // 通配符匹配（如 "eip155:*" 匹配所有 EVM 链）
        if (network.contains(":")) {
            String prefix = network.split(":")[0] + ":*";
            adapter = adapters.get(prefix);
            if (adapter != null) return adapter;
        }

        return null;
    }

    /**
     * 检查是否支持指定的网络。
     */
    public boolean isSupported(String network) {
        return find(network) != null;
    }
}
