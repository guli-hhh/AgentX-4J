package com.agentx4j.x402.scheme;

import com.agentx4j.core.model.PaymentPayload;
import com.agentx4j.core.model.PaymentRequirement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import com.agentx4j.x402.scheme.Scheme.VerifyResult;
import com.agentx4j.x402.scheme.Scheme.SettleResult;

/**
 * BatchSettlementScheme 测试。
 */
class BatchSettlementSchemeTest {

    private BatchSettlementScheme scheme;

    @BeforeEach
    void setUp() {
        scheme = new BatchSettlementScheme();
    }

    @Test
    void testGetName() {
        assertEquals("batch-settlement", scheme.getName());
    }

    @Test
    void testSupportsNetwork() {
        assertTrue(scheme.supportsNetwork("eip155:84532"));
        assertTrue(scheme.supportsNetwork("eip155:8453"));
        assertTrue(scheme.supportsNetwork("eip155:1"));
        assertFalse(scheme.supportsNetwork("solana:mainnet"));
        assertFalse(scheme.supportsNetwork(null));
    }

    @Test
    void testVerifySchemeMismatch() {
        PaymentPayload payload = PaymentPayload.builder()
                .scheme("exact")
                .network("eip155:84532")
                .build();
        PaymentRequirement req = PaymentRequirement.builder()
                .scheme("batch-settlement")
                .network("eip155:84532")
                .build();

        VerifyResult result = scheme.verify(payload, req);
        assertFalse(result.isValid());
        assertTrue(result.getReason().contains("Scheme mismatch"));
    }

    @Test
    void testVerifyUnsupportedNetwork() {
        PaymentPayload payload = PaymentPayload.builder()
                .scheme("batch-settlement")
                .network("solana:mainnet")
                .build();
        PaymentRequirement req = PaymentRequirement.builder()
                .scheme("batch-settlement")
                .network("solana:mainnet")
                .build();

        VerifyResult result = scheme.verify(payload, req);
        assertFalse(result.isValid());
        assertTrue(result.getReason().contains("Unsupported network"));
    }

    @Test
    void testVerifyInvalidPayloadType() {
        PaymentPayload payload = PaymentPayload.builder()
                .scheme("batch-settlement")
                .network("eip155:84532")
                .payload("invalid-payload")
                .build();
        PaymentRequirement req = PaymentRequirement.builder()
                .scheme("batch-settlement")
                .network("eip155:84532")
                .build();

        VerifyResult result = scheme.verify(payload, req);
        assertFalse(result.isValid());
        assertTrue(result.getReason().contains("Invalid payload type"));
    }

    @Test
    void testVerifyAmountMismatch() {
        PaymentPayload.ExactEvmPayload evmPayload = PaymentPayload.ExactEvmPayload.builder()
                .from("0x1234")
                .to("0x5678")
                .value("999")
                .nonce("0xabc")
                .validAfter(1000L)
                .validBefore(2000L)
                .signature("0xdef")
                .build();
        PaymentPayload payload = PaymentPayload.builder()
                .scheme("batch-settlement")
                .network("eip155:84532")
                .payload(evmPayload)
                .build();
        PaymentRequirement req = PaymentRequirement.builder()
                .scheme("batch-settlement")
                .network("eip155:84532")
                .amount("1000")
                .build();

        VerifyResult result = scheme.verify(payload, req);
        assertFalse(result.isValid());
        assertTrue(result.getReason().contains("Amount mismatch"));
    }

    @Test
    void testVerifyPayToMismatch() {
        PaymentPayload.ExactEvmPayload evmPayload = PaymentPayload.ExactEvmPayload.builder()
                .from("0x1234")
                .to("0xAAAA")
                .value("1000")
                .nonce("0xabc")
                .validAfter(1000L)
                .validBefore(2000L)
                .signature("0xdef")
                .build();
        PaymentPayload payload = PaymentPayload.builder()
                .scheme("batch-settlement")
                .network("eip155:84532")
                .payload(evmPayload)
                .build();
        PaymentRequirement req = PaymentRequirement.builder()
                .scheme("batch-settlement")
                .network("eip155:84532")
                .amount("1000")
                .payTo("0xBBBB")
                .build();

        VerifyResult result = scheme.verify(payload, req);
        assertFalse(result.isValid());
        assertTrue(result.getReason().contains("PayTo mismatch"));
    }

    @Test
    void testVerifyExpiredPayment() {
        long now = System.currentTimeMillis() / 1000;
        PaymentPayload.ExactEvmPayload evmPayload = PaymentPayload.ExactEvmPayload.builder()
                .from("0x1234")
                .to("0x5678")
                .value("1000")
                .nonce("0xabc")
                .validAfter(now - 200)
                .validBefore(now - 100)
                .signature("0xdef")
                .build();
        PaymentPayload payload = PaymentPayload.builder()
                .scheme("batch-settlement")
                .network("eip155:84532")
                .payload(evmPayload)
                .build();
        PaymentRequirement req = PaymentRequirement.builder()
                .scheme("batch-settlement")
                .network("eip155:84532")
                .amount("1000")
                .payTo("0x5678")
                .build();

        VerifyResult result = scheme.verify(payload, req);
        assertFalse(result.isValid());
        assertTrue(result.getReason().contains("expired"));
    }

    @Test
    void testVerifyNotYetValidPayment() {
        long now = System.currentTimeMillis() / 1000;
        PaymentPayload.ExactEvmPayload evmPayload = PaymentPayload.ExactEvmPayload.builder()
                .from("0x1234")
                .to("0x5678")
                .value("1000")
                .nonce("0xabc")
                .validAfter(now + 100)
                .validBefore(now + 200)
                .signature("0xdef")
                .build();
        PaymentPayload payload = PaymentPayload.builder()
                .scheme("batch-settlement")
                .network("eip155:84532")
                .payload(evmPayload)
                .build();
        PaymentRequirement req = PaymentRequirement.builder()
                .scheme("batch-settlement")
                .network("eip155:84532")
                .amount("1000")
                .payTo("0x5678")
                .build();

        VerifyResult result = scheme.verify(payload, req);
        assertFalse(result.isValid());
        assertTrue(result.getReason().contains("not yet valid"));
    }

    @Test
    void testSettleReturnsSuccess() {
        PaymentPayload.ExactEvmPayload evmPayload = PaymentPayload.ExactEvmPayload.builder()
                .from("0x1234")
                .to("0x5678")
                .value("1000")
                .nonce("0xabc")
                .validAfter(1000L)
                .validBefore(2000L)
                .signature("0xdef")
                .build();
        PaymentPayload payload = PaymentPayload.builder()
                .scheme("batch-settlement")
                .network("eip155:84532")
                .payload(evmPayload)
                .build();
        PaymentRequirement req = PaymentRequirement.builder()
                .scheme("batch-settlement")
                .network("eip155:84532")
                .amount("1000")
                .build();

        SettleResult result = scheme.settle(payload, req);
        assertNotNull(result);
        assertTrue(result.isSuccess());
    }
}
