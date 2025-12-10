package com.paiad.mcp.crawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paiad.mcp.model.NewsItem;

import java.util.ArrayList;
import java.util.List;

/**
 * 知乎热榜爬虫
 * 
 * @author Paiad
 */
public class ZhihuCrawler extends AbstractCrawler {

    private static final String API_URL = "https://www.zhihu.com/api/v3/feed/topstory/hot-lists/total?limit=50";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ZhihuCrawler() {
        super("zhihu", "知乎");
    }

    @Override
    public List<NewsItem> crawl() {
        List<NewsItem> items = new ArrayList<>();
        try {
            String response = doGet(API_URL);
            JsonNode json = objectMapper.readTree(response);
            JsonNode data = json.get("data");

            if (data != null && data.isArray()) {
                for (int i = 0; i < data.size(); i++) {
                    JsonNode item = data.get(i);
                    JsonNode target = item.get("target");

                    if (target != null) {
                        String id = target.has("id") ? target.get("id").asText() : "";
                        String title = target.has("title") ? target.get("title").asText() : "";
                        String url = "https://www.zhihu.com/question/" + id;

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
     * 解析热度值
     */
    private Long parseHotScore(String detailText) {
        if (detailText == null || detailText.isEmpty()) {
            return 0L;
        }
        try {
            // 格式如 "1234 万热度"
            String numStr = detailText.replaceAll("[^0-9.]", "");
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
