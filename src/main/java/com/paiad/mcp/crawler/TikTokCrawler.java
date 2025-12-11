package com.paiad.mcp.crawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paiad.mcp.model.NewsItem;

import java.util.ArrayList;
import java.util.List;

/**
 * TikTok 爬虫
 * 
 * @author Paiad
 */
public class TikTokCrawler extends AbstractCrawler {

    // TikTok 官方 API 通常需要签名，这里使用非官方或模拟的公开 Endpoint
    // 注意：TikTok 有严格的反爬限制，此 URL 可能随时间失效或需要验证码
    private static final String API_URL = "https://www.tiktok.com/node/share/discover";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TikTokCrawler() {
        super("tiktok", "TikTok");
    }

    @Override
    public List<NewsItem> crawl() {
        List<NewsItem> items = new ArrayList<>();
        try {
            // 模拟浏览器 Headers
            java.util.Map<String, String> headers = new java.util.HashMap<>();
            headers.put("Referer", "https://www.tiktok.com/");
            headers.put("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

            // 在实际生产环境中，TikTok 爬取极难绕过风控，这里仅作为代码结构演示
            // 如果无法直接获取，建议对接第三方商业 API 或使用 Selenium/Puppeteer
            String response = doGet(API_URL, headers);

            // 解析逻辑视具体返回结构而定，以下为假定结构
            if (response != null && !response.isEmpty()) {
                JsonNode json = objectMapper.readTree(response);
                JsonNode body = json.get("body");
                if (body != null) {
                    JsonNode list = body.get("item_list");
                    if (list != null && list.isArray()) {
                        int rank = 1;
                        for (JsonNode item : list) {
                            String desc = item.has("desc") ? item.get("desc").asText() : "";
                            String id = item.has("id") ? item.get("id").asText() : "";
                            String url = "https://www.tiktok.com/@" +
                                    (item.has("author") && item.get("author").has("uniqueId")
                                            ? item.get("author").get("uniqueId").asText()
                                            : "user")
                                    +
                                    "/video/" + id;

                            // 统计数据
                            JsonNode stats = item.get("stats");
                            Long diggCount = stats != null && stats.has("diggCount") ? stats.get("diggCount").asLong()
                                    : 0L;

                            if (!desc.isEmpty()) {
                                NewsItem newsItem = NewsItem.builder()
                                        .id("tiktok_" + id)
                                        .title(desc)
                                        .url(url)
                                        .platform(platformId)
                                        .platformName(platformName)
                                        .rank(rank++)
                                        .hotScore(diggCount)
                                        .hotDesc(formatHotScore(diggCount))
                                        .timestamp(System.currentTimeMillis())
                                        .build();
                                items.add(newsItem);
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            logger.warn("TikTok 爬取失败 (可能是风控限制): {}", e.getMessage());
            // 可以在此添加备用策略或返回空列表
        }
        return items;
    }

    private String formatHotScore(Long score) {
        if (score >= 1000000000) { // 1B
            return String.format("%.1fB", score / 1000000000.0);
        } else if (score >= 1000000) { // 1M
            return String.format("%.1fM", score / 1000000.0);
        } else if (score >= 1000) { // 1K
            return String.format("%.1fK", score / 1000.0);
        } else {
            return String.valueOf(score);
        }
    }
}
