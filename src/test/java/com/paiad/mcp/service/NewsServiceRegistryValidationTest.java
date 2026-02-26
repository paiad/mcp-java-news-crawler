package com.paiad.mcp.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class NewsServiceRegistryValidationTest {

    @Test
    void shouldFindMissingRegistrationsDeterministically() {
        List<String> missing = NewsService.findMissingCrawlerRegistrations(
                List.of("weibo", "zhihu", "unknown_a", "unknown_b"),
                Set.of("weibo", "zhihu"));

        assertEquals(List.of("unknown_a", "unknown_b"), missing);
    }

    @Test
    void shouldThrowInStrictModeWhenMissingRegistrations() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> NewsService.enforceRegistryConsistency(List.of("unknown"), true));
        assertTrue(ex.getMessage().contains("unknown"));
    }

    @Test
    void shouldNotThrowInDegradedModeWhenMissingRegistrations() {
        assertDoesNotThrow(() -> NewsService.enforceRegistryConsistency(List.of("unknown"), false));
    }
}
