package com.agentx4j.x402.scheme;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SchemeRegistry 测试。
 */
class SchemeRegistryTest {

    private SchemeRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new SchemeRegistry();
    }

    @Test
    void testRegisterAndFind() {
        ExactScheme scheme = new ExactScheme();
        registry.register("eip155:84532", scheme);

        Scheme found = registry.find("exact", "eip155:84532");
        assertNotNull(found);
        assertSame(scheme, found);
    }

    @Test
    void testWildcardMatch() {
        ExactScheme scheme = new ExactScheme();
        registry.register("eip155:*", scheme);

        assertNotNull(registry.find("exact", "eip155:84532"));
        assertNotNull(registry.find("exact", "eip155:1"));
        assertNotNull(registry.find("exact", "eip155:137"));
    }

    @Test
    void testFindNonExistent() {
        assertNull(registry.find("exact", "eip155:84532"));
        registry.register("eip155:*", new ExactScheme());
        assertNull(registry.find("upto", "eip155:84532"));
    }

    @Test
    void testIsSupported() {
        registry.register("eip155:*", new ExactScheme());

        assertTrue(registry.isSupported("exact", "eip155:84532"));
        assertFalse(registry.isSupported("upto", "eip155:84532"));
    }

    @Test
    void testPreferExactOverWildcard() {
        ExactScheme exactMatch = new ExactScheme();
        ExactScheme wildcardMatch = new ExactScheme();

        registry.register("eip155:84532", exactMatch);
        registry.register("eip155:*", wildcardMatch);

        // 精确匹配优先
        assertSame(exactMatch, registry.find("exact", "eip155:84532"));
        // 其他 EVM 链走通配符
        assertSame(wildcardMatch, registry.find("exact", "eip155:1"));
    }
}
