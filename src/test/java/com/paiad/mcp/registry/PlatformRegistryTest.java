package com.paiad.mcp.registry;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlatformRegistryTest {

    private final PlatformRegistry registry = PlatformRegistry.getInstance();

    @Test
    void shouldResolveAliasCaseInsensitively() {
        assertEquals("zhihu", registry.resolveId("ZH"));
        assertEquals("google_news", registry.resolveId("GoogleNews"));
        assertNull(registry.resolveId("unknown-platform"));
    }

    @Test
    void shouldReturnEnabledPlatformsSortedByPriority() {
        List<String> enabled = registry.getEnabledPlatformIdsSorted();
        assertFalse(enabled.isEmpty());
        assertEquals("hacker_news", enabled.get(0));
        assertTrue(enabled.contains("zhihu"));
        assertTrue(enabled.contains("weibo"));
    }

    @Test
    void shouldRespectDefaultPlatformCount() {
        List<String> defaults = registry.getDefaultPlatformIds();
        assertEquals(5, defaults.size());
    }

    @Test
    void shouldSortInputByRegistryPriority() {
        List<String> sorted = registry.sortByPriority(List.of("weibo", "zhihu", "hacker_news"));
        assertEquals(List.of("hacker_news", "zhihu", "weibo"), sorted);
    }
}
