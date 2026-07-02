package com.agentx4j.x402.facilitator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 支持的网络列表响应。
 *
 * <p>Facilitator 返回的它支持哪些区块链网络和代币。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportedNetworksResponse {

    /** 支持的网络列表 */
    private List<NetworkInfo> networks;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NetworkInfo {
        /** CAIP-2 网络标识 */
        private String network;

        /** 支持的 scheme 列表 */
        private List<String> schemes;

        /** 支持的代币信息 */
        private Map<String, TokenInfo> tokens;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenInfo {
        /** 代币名称 */
        private String name;

        /** 代币合约地址 */
        private String address;

        /** 小数位数 */
        private int decimals;
    }
}
