package com.paiad.mcp.model.pojo;

import lombok.Getter;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Getter
public class CrawlResult {
    private final List<NewsItem> data;
    private final Map<String, String> failures;
    private final List<PlatformCrawlOutcome> outcomes;

    public CrawlResult(List<NewsItem> data, Map<String, String> failures) {
        this(data, failures, Collections.emptyList());
    }

    public CrawlResult(List<NewsItem> data, Map<String, String> failures, List<PlatformCrawlOutcome> outcomes) {
        this.data = data != null ? data : Collections.emptyList();
        this.failures = failures != null ? failures : Collections.emptyMap();
        this.outcomes = outcomes != null ? outcomes : Collections.emptyList();
    }

    public boolean hasFailures() {
        return !failures.isEmpty();
    }

    public boolean isPartialSuccess() {
        return hasFailures() && !data.isEmpty();
    }
}
