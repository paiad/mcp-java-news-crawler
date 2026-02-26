package com.paiad.mcp.model.pojo;

import java.util.Collections;
import java.util.List;

/**
 * Structured crawl outcome for a single platform.
 */
public record PlatformCrawlOutcome(
        String platformId,
        String platformName,
        PlatformCrawlStatus status,
        List<NewsItem> items,
        String errorCode,
        String errorMessage,
        long latencyMs) {

    public PlatformCrawlOutcome {
        items = items == null ? Collections.emptyList() : List.copyOf(items);
    }

    public boolean isFailure() {
        return status == PlatformCrawlStatus.FAILED || status == PlatformCrawlStatus.TIMEOUT;
    }
}
