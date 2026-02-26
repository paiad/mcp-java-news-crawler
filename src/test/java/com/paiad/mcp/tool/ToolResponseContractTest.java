package com.paiad.mcp.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paiad.mcp.model.pojo.CrawlResult;
import com.paiad.mcp.model.pojo.NewsItem;
import com.paiad.mcp.model.pojo.PlatformCrawlOutcome;
import com.paiad.mcp.model.pojo.PlatformCrawlStatus;
import com.paiad.mcp.service.NewsService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ToolResponseContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void getHotNewsShouldExposePartialSuccessAndFailureDetails() throws Exception {
        NewsItem item = NewsItem.builder()
                .title("AI breakthrough")
                .platform("zhihu")
                .platformName("知乎")
                .url("https://example.com/a")
                .hotScore(100L)
                .build();

        PlatformCrawlOutcome successOutcome = new PlatformCrawlOutcome(
                "zhihu", "知乎", PlatformCrawlStatus.SUCCESS, List.of(item), null, null, 120);
        PlatformCrawlOutcome failedOutcome = new PlatformCrawlOutcome(
                "reddit", "Reddit", PlatformCrawlStatus.TIMEOUT, List.of(), "TIMEOUT", "Platform crawl timed out", 45000);

        CrawlResult crawlResult = new CrawlResult(List.of(item), Map.of("reddit", "TIMEOUT: Platform crawl timed out"),
                List.of(successOutcome, failedOutcome));

        NewsService fakeService = new NewsService() {
            @Override
            public CrawlResult getHotNews(List<String> platforms, int limit) {
                return crawlResult;
            }
        };

        GetHotNewsTool tool = new GetHotNewsTool(fakeService);
        JsonNode node = objectMapper.readTree(tool.execute(objectMapper.createObjectNode(), objectMapper));

        assertTrue(node.get("success").asBoolean());
        assertTrue(node.get("partial_success").asBoolean());
        assertEquals(1, node.get("failure_count").asInt());
        assertTrue(node.has("failure_details"));
        assertEquals("reddit", node.get("failure_details").get(0).get("platform").asText());
        assertTrue(node.has("data"));
        assertEquals("https://example.com/a", node.get("data").get(0).get("url").asText());
    }

    @Test
    void searchNewsShouldExposePartialSuccess() throws Exception {
        PlatformCrawlOutcome failedOutcome = new PlatformCrawlOutcome(
                "bbc", "BBC", PlatformCrawlStatus.FAILED, List.of(), "IOException", "Request failed", 220);
        CrawlResult crawlResult = new CrawlResult(List.of(), Map.of("bbc", "IOException: Request failed"),
                List.of(failedOutcome));

        NewsService fakeService = new NewsService() {
            @Override
            public CrawlResult searchNews(String query, List<String> platforms, int limit) {
                return crawlResult;
            }
        };

        SearchNewsTool tool = new SearchNewsTool(fakeService);
        JsonNode args = objectMapper.createObjectNode().put("query", "ai");
        JsonNode node = objectMapper.readTree(tool.execute(args, objectMapper));

        assertTrue(node.get("success").asBoolean());
        assertFalse(node.get("partial_success").asBoolean());
        assertEquals(1, node.get("failure_count").asInt());
        assertEquals("bbc", node.get("failure_details").get(0).get("platform").asText());
    }
}
