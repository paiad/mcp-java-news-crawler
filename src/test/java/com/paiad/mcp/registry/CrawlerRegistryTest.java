package com.paiad.mcp.registry;

import com.paiad.mcp.crawler.AbstractCrawler;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CrawlerRegistryTest {

    private final CrawlerRegistry registry = CrawlerRegistry.getInstance();

    @Test
    void shouldListSupportedPlatforms() {
        Set<String> supported = registry.getSupportedPlatformIds();
        assertEquals(15, supported.size());
        assertTrue(supported.contains("zhihu"));
        assertTrue(supported.contains("hacker_news"));
    }

    @Test
    void shouldLookupCrawlerById() {
        AbstractCrawler zhihuCrawler = registry.getCrawler("zhihu");
        assertNotNull(zhihuCrawler);
        assertEquals("zhihu", zhihuCrawler.getPlatformId());

        assertNull(registry.getCrawler("unknown"));
    }
}
