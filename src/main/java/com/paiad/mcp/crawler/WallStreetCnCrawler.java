package com.paiad.mcp.crawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.paiad.mcp.model.NewsItem;
import com.paiad.mcp.util.JsonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 华尔街见闻热榜爬虫
 *
 * @author Paiad
 */
public class WallStreetCnCrawler extends AbstractCrawler {

    private static final String API_URL = "https://api-one-wscn.awtmt.com/apiv1/content/articles/hot?limit=50&period=all";

    public WallStreetCnCrawler() {
        super("wallstreetcn", "华尔街见闻");
    }

    @Override
    public List<NewsItem> crawl() {
        List<NewsItem> items = new ArrayList<>();
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            headers.put("Referer", "https://wallstreetcn.com/");

            String response = doGet(API_URL, headers);
            JsonNode json = JsonUtils.getMapper().readTree(response);

            if (json.has("code") && json.get("code").asInt() == 20000) {
                JsonNode data = json.get("data");
                // 优先使用 day_items（今日热榜）
                JsonNode dayItems = data.get("day_items");

                if (dayItems != null && dayItems.isArray()) {
                    for (int i = 0; i < dayItems.size(); i++) {
                        JsonNode item = dayItems.get(i);
                        NewsItem newsItem = parseItem(item, i + 1);
                        if (newsItem != null) {
                            items.add(newsItem);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("华尔街见闻热榜爬取失败: {}", e.getMessage(), e);
        }
        return items;
    }

    /**
     * 解析单条新闻
     */
    private NewsItem parseItem(JsonNode item, int rank) {
        try {
            String id = item.has("id") ? String.valueOf(item.get("id").asLong()) : "";
            String title = item.has("title") ? item.get("title").asText() : "";
            String url = item.has("uri") ? item.get("uri").asText() : "";
            long pageviews = item.has("pageviews") ? item.get("pageviews").asLong() : 0;

            return NewsItem.builder()
                    .id("wallstreetcn_" + id)
                    .title(title)
                    .url(url)
                    .platform(platformId)
                    .platformName(platformName)
                    .rank(rank)
                    .hotScore(pageviews)
                    .hotDesc(formatPageviews(pageviews))
                    .timestamp(System.currentTimeMillis())
                    .build();
        } catch (Exception e) {
            logger.error("解析华尔街见闻新闻失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 格式化阅读量
     */
    private String formatPageviews(long pageviews) {
        if (pageviews >= 10000) {
            return String.format("%.1f万阅读", pageviews / 10000.0);
        }
        return pageviews + "阅读";
    }
}
