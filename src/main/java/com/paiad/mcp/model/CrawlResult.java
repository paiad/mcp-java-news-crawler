package com.paiad.mcp.model;

import lombok.Getter;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Getter
public class CrawlResult {
    private List<NewsItem> data;
    private Map<String, String> failures;

    public CrawlResult(List<NewsItem> data, Map<String, String> failures) {
        this.data = data != null ? data : Collections.emptyList();
        this.failures = failures != null ? failures : Collections.emptyMap();
    }

    public boolean hasFailures() {
        return !failures.isEmpty();
    }
}
