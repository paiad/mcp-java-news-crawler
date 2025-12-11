package com.paiad.mcp.crawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paiad.mcp.model.NewsItem;

import java.util.ArrayList;
import java.util.List;

/**
 * X (Twitter) 爬虫
 * 
 * @author Paiad
 */
public class XCrawler extends AbstractCrawler {

    // X/Twitter 必须使用 OAuth 2.0 API 或 Guest Token 机制
    // 公开 URL 通常无法直接获取 JSON
    private static final String API_URL = "https://api.twitter.com/2/trends/by/woeid/1"; // 1 = World, 可改为特定地区
    private final ObjectMapper objectMapper = new ObjectMapper();

    public XCrawler() {
        super("x", "X (Twitter)");
    }

    @Override
    public List<NewsItem> crawl() {
        List<NewsItem> items = new ArrayList<>();
        try {
            // X 需要 Bearer Token (通常来自开发者账号)
            // 1. 尝试从系统环境变量获取
            // 2. 尝试从项目根目录 .env 文件获取
            //
            // 填写示例 (Enc Example):
            // 在项目根目录创建 .env 文件，写入:
            // TWITTER_BEARER_TOKEN=AAAAAAAAAAAAAAAAAAAAANRILgAAAAAAnMw...
            //
            String token = getBearerToken();
            if (token == null || token.isEmpty()) {
                logger.debug("未配置 TWITTER_BEARER_TOKEN，X 爬虫跳过执行");
                return items;
            }

            java.util.Map<String, String> headers = new java.util.HashMap<>();
            headers.put("Authorization", "Bearer " + token);

            String response = doGet(API_URL, headers);

            if (response != null && !response.isEmpty()) {
                JsonNode json = objectMapper.readTree(response);
                if (json.isArray() && json.size() > 0) {
                    JsonNode trends = json.get(0).get("trends");
                    if (trends != null && trends.isArray()) {
                        int rank = 1;
                        for (JsonNode item : trends) {
                            String name = item.has("name") ? item.get("name").asText() : "";
                            String url = item.has("url") ? item.get("url").asText() : "";
                            long tweetVolume = item.has("tweet_volume") && !item.get("tweet_volume").isNull()
                                    ? item.get("tweet_volume").asLong()
                                    : 0L;

                            if (!name.isEmpty()) {
                                NewsItem newsItem = NewsItem.builder()
                                        .id("x_" + rank)
                                        .title(name)
                                        .url(url)
                                        .platform(platformId)
                                        .platformName(platformName)
                                        .rank(rank++)
                                        .hotScore(tweetVolume)
                                        .hotDesc(formatHotScore(tweetVolume))
                                        .timestamp(System.currentTimeMillis())
                                        .build();
                                items.add(newsItem);
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            logger.error("X (Twitter) 爬取失败: {}", e.getMessage());
        }
        return items;
    }

    private String getBearerToken() {
        // 1. Env Var
        String token = System.getenv("TWITTER_BEARER_TOKEN");
        if (token != null && !token.isEmpty())
            return token;

        // 2. .env file
        try {
            java.io.File envFile = new java.io.File(".env");
            if (envFile.exists()) {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(envFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.trim().startsWith("TWITTER_BEARER_TOKEN=")) {
                            return line.split("=", 2)[1].trim();
                        }
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    private String formatHotScore(Long score) {
        if (score == null || score == 0)
            return "";
        if (score >= 1000000) {
            return String.format("%.1fM Tweets", score / 1000000.0);
        } else if (score >= 1000) {
            return String.format("%.1fK Tweets", score / 1000.0);
        } else {
            return score + " Tweets";
        }
    }
}
