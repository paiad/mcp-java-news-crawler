package com.paiad.mcp.crawler.domestic;

import com.paiad.mcp.crawler.AbstractCrawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.paiad.mcp.model.NewsItem;
import com.paiad.mcp.util.JsonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知乎热榜爬虫
 *
 * @author Paiad
 */
public class ZhihuCrawler extends AbstractCrawler {

    // 关键: 使用 api.zhihu.com 而非 www.zhihu.com
    private static final String API_URL = "https://api.zhihu.com/topstory/hot-lists/total?limit=50";

    public ZhihuCrawler() {
        super("zhihu", "知乎");
    }

    @Override
    public List<NewsItem> crawl() {
        List<NewsItem> items = new ArrayList<>();
        try {
            // 使用移动端 User-Agent
            Map<String, String> headers = new HashMap<>();
            headers.put("User-Agent",
                    "Mozilla/5.0 (iPhone; CPU iPhone OS 14_2_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0.1 Mobile/15E148 Safari/604.1");

            String response = doGet(API_URL, headers);
            JsonNode json = JsonUtils.getMapper().readTree(response);
            JsonNode data = json.get("data");

            if (data != null && data.isArray()) {
                for (int i = 0; i < data.size(); i++) {
                    JsonNode item = data.get(i);
                    JsonNode target = item.get("target");

                    if (target != null) {
                        String id = target.has("id") ? target.get("id").asText() : "";
                        String title = target.has("title") ? target.get("title").asText() : "";

                        // 从 url 字段提取问题 ID
                        String targetUrl = target.has("url") ? target.get("url").asText() : "";
                        String questionId = extractQuestionId(targetUrl, id);
                        String url = "https://www.zhihu.com/question/" + questionId;

                        // 获取热度
                        String detailText = item.has("detail_text") ? item.get("detail_text").asText() : "";
                        Long hotScore = parseHotScore(detailText);

                        NewsItem newsItem = NewsItem.builder()
                                .id("zhihu_" + id)
                                .title(title)
                                .url(url)
                                .platform(platformId)
                                .platformName(platformName)
                                .rank(i + 1)
                                .hotScore(hotScore)
                                .hotDesc(detailText)
                                .timestamp(System.currentTimeMillis())
                                .build();

                        items.add(newsItem);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("知乎热榜爬取失败: {}", e.getMessage(), e);
        }
        return items;
    }

    /**
     * 从 URL 中提取问题 ID
     */
    private String extractQuestionId(String url, String fallbackId) {
        if (url != null && !url.isEmpty()) {
            String[] parts = url.split("/");
            if (parts.length > 0) {
                return parts[parts.length - 1];
            }
        }
        return fallbackId;
    }

    /**
     * 解析热度值
     */
    private Long parseHotScore(String detailText) {
        if (detailText == null || detailText.isEmpty()) {
            return 0L;
        }
        try {
            // 格式如 "1234 万热度"
            String numStr = detailText.replaceAll("[^0-9.]", "");
            if (numStr.isEmpty()) {
                return 0L;
            }
            double num = Double.parseDouble(numStr);
            if (detailText.contains("亿")) {
                return (long) (num * 100000000);
            } else if (detailText.contains("万")) {
                return (long) (num * 10000);
            } else {
                return (long) num;
            }
        } catch (Exception e) {
            return 0L;
        }
    }
}
