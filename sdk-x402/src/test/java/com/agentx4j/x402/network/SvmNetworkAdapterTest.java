package com.agentx4j.x402.network;

import com.agentx4j.core.model.PaymentPayload;
import com.agentx4j.core.model.PaymentRequirement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SvmNetworkAdapter 测试。
 */
class SvmNetworkAdapterTest {

    private SvmNetworkAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new SvmNetworkAdapter("https://api.devnet.solana.com");
    }

    @Test
    void testGetNetworkPrefix() {
        assertEquals("solana", adapter.getNetworkPrefix());
    }

    @Test
    void testVerifySignatureWithInvalidPayloadType() {
        PaymentPayload payload = PaymentPayload.builder()
                .scheme("exact")
                .network("solana:EtWTRABZaYq6iMfeYKouRu166VU2xqa1wcaWoxPkrZBG")
                .payload("invalid-payload")
                .build();
        PaymentRequirement req = PaymentRequirement.builder()
                .scheme("exact")
                .network("solana:EtWTRABZaYq6iMfeYKouRu166VU2xqa1wcaWoxPkrZBG")
                .build();

        boolean result = adapter.verifySignature(payload, req);
        assertFalse(result);
    }

    @Test
    void testVerifySignatureWithExpiredPayment() {
        long now = System.currentTimeMillis() / 1000;
        PaymentPayload.ExactEvmPayload evmPayload = PaymentPayload.ExactEvmPayload.builder()
                .from("solana-wallet-address")
                .to("solana-recipient-address")
                .value("1000")
                .nonce("0xabc")
                .validAfter(now - 200)
                .validBefore(now - 100)
                .signature("0xdef")
                .build();
        PaymentPayload payload = PaymentPayload.builder()
                .scheme("exact")
                .network("solana:EtWTRABZaYq6iMfeYKouRu166VU2xqa1wcaWoxPkrZBG")
                .payload(evmPayload)
                .build();
        PaymentRequirement req = PaymentRequirement.builder()
                .scheme("exact")
                .network("solana:EtWTRABZaYq6iMfeYKouRu166VU2xqa1wcaWoxPkrZBG")
                .build();

        boolean result = adapter.verifySignature(payload, req);
        assertFalse(result);
    }

    @Test
    void testVerifySignatureWithValidPayment() {
        long now = System.currentTimeMillis() / 1000;
        PaymentPayload.ExactEvmPayload evmPayload = PaymentPayload.ExactEvmPayload.builder()
                .from("solana-wallet-address")
                .to("solana-recipient-address")
                .value("1000")
                .nonce("0xabc")
                .validAfter(now - 10)
                .validBefore(now + 60)
                .signature("0xdef")
                .build();
        PaymentPayload payload = PaymentPayload.builder()
                .scheme("exact")
                .network("solana:EtWTRABZaYq6iMfeYKouRu166VU2xqa1wcaWoxPkrZBG")
                .payload(evmPayload)
                .build();
        PaymentRequirement req = PaymentRequirement.builder()
                .scheme("exact")
                .network("solana:EtWTRABZaYq6iMfeYKouRu166VU2xqa1wcaWoxPkrZBG")
                .build();

        boolean result = adapter.verifySignature(payload, req);
        assertTrue(result);
    }

    @Test
    void testSubmitTransaction() {
        PaymentPayload.ExactEvmPayload evmPayload = PaymentPayload.ExactEvmPayload.builder()
                .from("solana-wallet-address")
                .to("solana-recipient-address")
                .value("1000")
                .nonce("0xabc")
                .validAfter(1000L)
                .validBefore(2000L)
                .signature("0xdef")
                .build();
        PaymentPayload payload = PaymentPayload.builder()
                .scheme("exact")
                .network("solana:EtWTRABZaYq6iMfeYKouRu166VU2xqa1wcaWoxPkrZBG")
                .payload(evmPayload)
                .build();
        PaymentRequirement req = PaymentRequirement.builder()
                .scheme("exact")
                .network("solana:EtWTRABZaYq6iMfeYKouRu166VU2xqa1wcaWoxPkrZBG")
                .build();

        String txHash = adapter.submitTransaction(payload, req);
        assertNotNull(txHash);
    }

    @Test
    void testGetConfirmation() {
        NetworkAdapter.TransactionConfirmation confirmation = adapter.getConfirmation("0x-solana-tx-hash");
        assertNotNull(confirmation);
        assertEquals("0x-solana-tx-hash", confirmation.getTxHash());
    }

    @Test
    void testGetTokenBalance() {
        BigInteger balance = adapter.getTokenBalance(
                "solana-wallet-address",
                "solana-token-address"
        );
        assertNotNull(balance);
        assertEquals(BigInteger.ZERO, balance);
    }

    @Test
    void testGetBlockNumber() {
        long blockNumber = adapter.getBlockNumber();
        assertTrue(blockNumber >= 0);
    }
}
