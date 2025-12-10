package com.paiad.mcp.crawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paiad.mcp.model.NewsItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 今日头条热榜爬虫
 * 
 * @author Paiad
 */
public class ToutiaoCrawler extends AbstractCrawler {

    private static final String API_URL = "https://www.toutiao.com/hot-event/hot-board/?origin=toutiao_pc";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ToutiaoCrawler() {
        super("toutiao", "头条");
    }

    @Override
    public List<NewsItem> crawl() {
        List<NewsItem> items = new ArrayList<>();
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("Referer", "https://www.toutiao.com/");

            String response = doGet(API_URL, headers);
            JsonNode json = objectMapper.readTree(response);

            String status = json.has("status") ? json.get("status").asText() : "";
            if ("success".equals(status)) {
                JsonNode data = json.get("data");
                if (data != null && data.isArray()) {
                    for (int i = 0; i < data.size(); i++) {
                        JsonNode item = data.get(i);

                        String title = item.has("Title") ? item.get("Title").asText() : "";
                        String clusterIdStr = item.has("ClusterIdStr") ? item.get("ClusterIdStr").asText() : "";
                        Long hotValue = item.has("HotValue") ? item.get("HotValue").asLong() : 0L;
                        String label = item.has("LabelUri") ? item.get("LabelUri").asText() : null;
                        String url = item.has("Url") ? item.get("Url").asText() : null;

                        if (url == null || url.isEmpty()) {
                            url = "https://www.toutiao.com/trending/" + clusterIdStr + "/";
                        }

                        NewsItem newsItem = NewsItem.builder()
                                .id("toutiao_" + clusterIdStr)
                                .title(title)
                                .url(url)
                                .platform(platformId)
                                .platformName(platformName)
                                .rank(i + 1)
                                .hotScore(hotValue)
                                .hotDesc(formatHotScore(hotValue))
                                .tag(parseLabel(label))
                                .timestamp(System.currentTimeMillis())
                                .build();

                        items.add(newsItem);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("今日头条热榜爬取失败: {}", e.getMessage(), e);
        }
        return items;
    }

    private String parseLabel(String labelUri) {
        if (labelUri == null || labelUri.isEmpty()) {
            return null;
        }
        if (labelUri.contains("hot")) {
            return "热";
        } else if (labelUri.contains("new")) {
            return "新";
        } else if (labelUri.contains("recommend")) {
            return "荐";
        }
        return null;
    }

    private String formatHotScore(Long score) {
        if (score >= 100000000) {
            return String.format("%.1f亿", score / 100000000.0);
        } else if (score >= 10000) {
            return String.format("%.1f万", score / 10000.0);
        } else {
            return String.valueOf(score);
        }
    }
}
