package com.agentx4j.x402.client;

import com.agentx4j.core.model.PaymentRequirement;
import com.agentx4j.x402.scheme.ExactScheme;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * X402Client 测试。
 */
class X402ClientTest {

    private X402Client client;

    @BeforeEach
    void setUp() {
        client = new X402Client();
        client.registerScheme("eip155:*", new ExactScheme());
    }

    @Test
    void testSelectPaymentRequirement() {
        List<PaymentRequirement> requirements = Arrays.asList(
                PaymentRequirement.builder()
                        .scheme("exact")
                        .network("eip155:84532")
                        .amount("1000")
                        .build(),
                PaymentRequirement.builder()
                        .scheme("exact")
                        .network("solana:mainnet")
                        .amount("2000")
                        .build()
        );

        PaymentRequirement selected = client.selectPaymentRequirement(requirements);
        assertNotNull(selected);
        assertEquals("exact", selected.getScheme());
        assertEquals("eip155:84532", selected.getNetwork());
    }

    @Test
    void testSelectPaymentRequirementEmpty() {
        assertThrows(IllegalArgumentException.class, () -> {
            client.selectPaymentRequirement(null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            client.selectPaymentRequirement(Arrays.asList());
        });
    }

    @Test
    void testCreatePaymentWithoutWallet() {
        PaymentRequirement requirement = PaymentRequirement.builder()
                .scheme("exact")
                .network("eip155:84532")
                .amount("1000")
                .payTo("0xReceiver")
                .build();

        // 未设置私钥时应抛出异常
        assertThrows(IllegalStateException.class, () -> {
            client.createPayment(requirement);
        });
    }

    @Test
    void testCreatePaymentWithUnsupportedScheme() {
        // 设置钱包但不注册对应的 scheme
        byte[] privateKey = new byte[32];
        client.setWallet(privateKey, "0xSender");

        PaymentRequirement requirement = PaymentRequirement.builder()
                .scheme("unsupported-scheme")
                .network("eip155:84532")
                .amount("1000")
                .payTo("0xReceiver")
                .build();

        assertThrows(IllegalStateException.class, () -> {
            client.createPayment(requirement);
        });
    }

    @Test
    void testSchemeRegistryAccess() {
        assertNotNull(client.getSchemeRegistry());
        assertTrue(client.getSchemeRegistry().isSupported("exact", "eip155:84532"));
    }
}
