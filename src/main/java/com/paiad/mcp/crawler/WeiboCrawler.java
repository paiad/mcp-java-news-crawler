package com.paiad.mcp.crawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.paiad.mcp.model.NewsItem;
import com.paiad.mcp.util.JsonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 微博热搜爬虫
 * 
 * @author Paiad
 */
public class WeiboCrawler extends AbstractCrawler {

    private static final String API_URL = "https://weibo.com/ajax/side/hotSearch";

    public WeiboCrawler() {
        super("weibo", "微博");
    }

    @Override
    public List<NewsItem> crawl() {
        List<NewsItem> items = new ArrayList<>();
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("Referer", "https://weibo.com/");
            headers.put("Cookie",
                    "SUB=_2AkMTqH_Sf8NxqwFRmP8TzmLkb4tyywzEieKnRqMJJRMxHRl-yT9jqhALtRB6PaaYU2R-f_xhJaXxVQaZ_nLlGcZf-jXD");

            String response = doGet(API_URL, headers);
            JsonNode json = JsonUtils.getMapper().readTree(response);
            JsonNode data = json.get("data");

            if (data != null) {
                JsonNode realtime = data.get("realtime");
                if (realtime != null && realtime.isArray()) {
                    for (int i = 0; i < realtime.size(); i++) {
                        JsonNode item = realtime.get(i);

                        String word = item.has("word") ? item.get("word").asText() : "";
                        String note = item.has("note") ? item.get("note").asText() : null;
                        Long rawHot = item.has("raw_hot") ? item.get("raw_hot").asLong() : 0L;
                        String labelName = item.has("label_name") ? item.get("label_name").asText() : null;

                        String title = note != null ? note : word;
                        String url = "https://s.weibo.com/weibo?q=" + encodeUrl(word);

                        NewsItem newsItem = NewsItem.builder()
                                .id("weibo_" + i)
                                .title(title)
                                .url(url)
                                .platform(platformId)
                                .platformName(platformName)
                                .rank(i + 1)
                                .hotScore(rawHot)
                                .hotDesc(formatHotScore(rawHot))
                                .tag(labelName)
                                .timestamp(System.currentTimeMillis())
                                .build();

                        items.add(newsItem);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("微博热搜爬取失败: {}", e.getMessage(), e);
        }
        return items;
    }

    private String encodeUrl(String str) {
        try {
            return java.net.URLEncoder.encode(str, "UTF-8");
        } catch (Exception e) {
            return str;
        }
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
