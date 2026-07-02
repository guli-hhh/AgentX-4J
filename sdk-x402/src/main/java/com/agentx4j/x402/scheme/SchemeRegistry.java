package com.agentx4j.x402.scheme;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 支付方案注册表。
 *
 * <p>管理所有已注册的 Scheme 实现。
 * 支持按 scheme 名称 + 网络标识查找对应的实现。</p>
 *
 * <p>类比：SchemeRegistry ≈ "支付方式管理器"，
 *      根据"用什么付"（scheme）+"在哪条链上付"（network）找到对应的处理器。</p>
 */
public class SchemeRegistry {

    private static final Logger log = LoggerFactory.getLogger(SchemeRegistry.class);

    /** 注册表: schemeName → (network → Scheme) */
    private final Map<String, Map<String, Scheme>> registry = new ConcurrentHashMap<>();

    /**
     * 注册支付方案。
     *
     * @param network 网络匹配模式（如 "eip155:84532" 或 "eip155:*" 表示所有 EVM 链）
     * @param scheme  该网络的支付方案实现
     */
    public void register(String network, Scheme scheme) {
        registry.computeIfAbsent(scheme.getName(), k -> new ConcurrentHashMap<>())
                .put(network, scheme);
        log.info("Registered scheme '{}' for network '{}'", scheme.getName(), network);
    }

    /**
     * 查找支付方案。
     *
     * <p>查找策略：</p>
     * <ol>
     *   <li>精确匹配：scheme + network（如 "exact" + "eip155:84532"）</li>
     *   <li>通配符匹配：scheme + network 前缀（如 "exact" + "eip155:*"）</li>
     * </ol>
     *
     * @param schemeName 方案名称
     * @param network    网络标识
     * @return 对应的 Scheme 实现，如果未找到则返回 null
     */
    public Scheme find(String schemeName, String network) {
        Map<String, Scheme> networkMap = registry.get(schemeName);
        if (networkMap == null) {
            return null;
        }

        // 1. 精确匹配
        Scheme scheme = networkMap.get(network);
        if (scheme != null) {
            return scheme;
        }

        // 2. 通配符匹配（如 "eip155:*" 匹配所有 EVM 链）
        if (network.contains(":")) {
            String prefix = network.split(":")[0] + ":*";
            scheme = networkMap.get(prefix);
            if (scheme != null) {
                return scheme;
            }
        }

        return null;
    }

    /**
     * 检查是否支持指定的 scheme + network 组合。
     */
    public boolean isSupported(String schemeName, String network) {
        return find(schemeName, network) != null;
    }
}
