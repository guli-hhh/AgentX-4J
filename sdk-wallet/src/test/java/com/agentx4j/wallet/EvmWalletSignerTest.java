package com.agentx4j.wallet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EvmWalletSigner 测试。
 */
class EvmWalletSignerTest {

    // 测试用私钥（仅用于测试，不要在生产环境使用）
    private static final String TEST_PRIVATE_KEY = "0x0123456789012345678901234567890123456789012345678901234567890123";

    @Test
    void testCreateFromPrivateKey() {
        EvmWalletSigner signer = new EvmWalletSigner(TEST_PRIVATE_KEY);
        assertNotNull(signer.getAddress());
        assertTrue(signer.getAddress().startsWith("0x"));
        assertEquals(42, signer.getAddress().length()); // 0x + 40 hex chars
    }

    @Test
    void testCreateFromPrivateKeyWithout0x() {
        EvmWalletSigner signer = new EvmWalletSigner("0123456789012345678901234567890123456789012345678901234567890123");
        assertNotNull(signer.getAddress());
        assertTrue(signer.getAddress().startsWith("0x"));
    }

    @Test
    void testGetPublicKey() {
        EvmWalletSigner signer = new EvmWalletSigner(TEST_PRIVATE_KEY);
        String publicKey = signer.getPublicKey();
        assertNotNull(publicKey);
        assertTrue(publicKey.length() > 0);
    }

    @Test
    void testSignTransferWithAuthorization() {
        EvmWalletSigner signer = new EvmWalletSigner(TEST_PRIVATE_KEY);

        String signature = signer.signTransferWithAuthorization(
                "0x1111111111111111111111111111111111111111",
                "0x2222222222222222222222222222222222222222",
                "1000",
                "0x" + "00".repeat(32),
                System.currentTimeMillis() / 1000,
                System.currentTimeMillis() / 1000 + 60
        );

        assertNotNull(signature);
        assertTrue(signature.startsWith("0x"));
        // 65 字节签名 = 130 hex chars + 0x prefix = 132
        assertEquals(132, signature.length());
    }

    @Test
    void testSameKeySameAddress() {
        EvmWalletSigner signer1 = new EvmWalletSigner(TEST_PRIVATE_KEY);
        EvmWalletSigner signer2 = new EvmWalletSigner(TEST_PRIVATE_KEY);
        assertEquals(signer1.getAddress(), signer2.getAddress());
    }
}
