package com.agentx4j.x402.scheme;

import com.agentx4j.core.model.PaymentRequirement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExactScheme 测试。
 */
class ExactSchemeTest {

    @Test
    void testSchemeName() {
        ExactScheme scheme = new ExactScheme();
        assertEquals("exact", scheme.getName());
    }

    @Test
    void testSupportsNetwork() {
        ExactScheme scheme = new ExactScheme();
        assertTrue(scheme.supportsNetwork("eip155:84532"));
        assertTrue(scheme.supportsNetwork("eip155:1"));
        assertTrue(scheme.supportsNetwork("eip155:*"));
        assertFalse(scheme.supportsNetwork("solana:mainnet"));
        assertFalse(scheme.supportsNetwork(null));
    }

    @Test
    void testVerifySchemeMismatch() {
        ExactScheme scheme = new ExactScheme();
        com.agentx4j.core.model.PaymentPayload payload = com.agentx4j.core.model.PaymentPayload.builder()
                .scheme("upto")
                .network("eip155:84532")
                .build();
        PaymentRequirement requirement = PaymentRequirement.builder()
                .scheme("exact")
                .network("eip155:84532")
                .build();

        Scheme.VerifyResult result = scheme.verify(payload, requirement);
        assertFalse(result.isValid());
        assertTrue(result.getReason().contains("Scheme mismatch"));
    }

    @Test
    void testVerifyUnsupportedNetwork() {
        ExactScheme scheme = new ExactScheme();
        com.agentx4j.core.model.PaymentPayload payload = com.agentx4j.core.model.PaymentPayload.builder()
                .scheme("exact")
                .network("solana:mainnet")
                .build();
        PaymentRequirement requirement = PaymentRequirement.builder()
                .scheme("exact")
                .network("solana:mainnet")
                .build();

        Scheme.VerifyResult result = scheme.verify(payload, requirement);
        assertFalse(result.isValid());
        assertTrue(result.getReason().contains("Unsupported network"));
    }
}
