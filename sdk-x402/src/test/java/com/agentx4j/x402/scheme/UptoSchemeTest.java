package com.agentx4j.x402.scheme;

import com.agentx4j.core.model.PaymentPayload;
import com.agentx4j.core.model.PaymentRequirement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UptoScheme 测试。
 */
class UptoSchemeTest {

    @Test
    void testSchemeName() {
        UptoScheme scheme = new UptoScheme();
        assertEquals("upto", scheme.getName());
    }

    @Test
    void testSupportsNetwork() {
        UptoScheme scheme = new UptoScheme();
        assertTrue(scheme.supportsNetwork("eip155:84532"));
        assertTrue(scheme.supportsNetwork("eip155:1"));
        assertFalse(scheme.supportsNetwork("solana:mainnet"));
        assertFalse(scheme.supportsNetwork(null));
    }

    @Test
    void testVerifySchemeMismatch() {
        UptoScheme scheme = new UptoScheme();
        PaymentPayload payload = PaymentPayload.builder()
                .scheme("exact")
                .network("eip155:84532")
                .build();
        PaymentRequirement requirement = PaymentRequirement.builder()
                .scheme("upto")
                .network("eip155:84532")
                .build();

        Scheme.VerifyResult result = scheme.verify(payload, requirement);
        assertFalse(result.isValid());
        assertTrue(result.getReason().contains("Scheme mismatch"));
    }

    @Test
    void testVerifyUnsupportedNetwork() {
        UptoScheme scheme = new UptoScheme();
        PaymentPayload payload = PaymentPayload.builder()
                .scheme("upto")
                .network("solana:mainnet")
                .build();
        PaymentRequirement requirement = PaymentRequirement.builder()
                .scheme("upto")
                .network("solana:mainnet")
                .build();

        Scheme.VerifyResult result = scheme.verify(payload, requirement);
        assertFalse(result.isValid());
        assertTrue(result.getReason().contains("Unsupported network"));
    }

    @Test
    void testVerifyInvalidPayloadType() {
        UptoScheme scheme = new UptoScheme();
        // 使用 ExactEvmPayload 而不是 UptoEvmPayload
        PaymentPayload payload = PaymentPayload.builder()
                .scheme("upto")
                .network("eip155:84532")
                .payload(PaymentPayload.ExactEvmPayload.builder().build())
                .build();
        PaymentRequirement requirement = PaymentRequirement.builder()
                .scheme("upto")
                .network("eip155:84532")
                .amount("5000")
                .build();

        Scheme.VerifyResult result = scheme.verify(payload, requirement);
        assertFalse(result.isValid());
        assertTrue(result.getReason().contains("Invalid payload type"));
    }
}
