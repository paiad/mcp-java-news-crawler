package com.paiad.mcp.crawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.paiad.mcp.model.NewsItem;
import com.paiad.mcp.util.JsonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Reddit 爬虫
 * 
 * @author Paiad
 */
public class RedditCrawler extends AbstractCrawler {

    // Reddit 公开 JSON 接口，加上 .json 后缀即可
    private static final String API_URL = "https://www.reddit.com/r/all/hot.json?limit=25";

    public RedditCrawler() {
        super("reddit", "Reddit");
    }

    @Override
    protected boolean isInternational() {
        return true;
    }

    @Override
    public List<NewsItem> crawl() {
        List<NewsItem> items = new ArrayList<>();
        try {
            // Reddit 需要设置 User-Agent 以避免 429 Too Many Requests
            java.util.Map<String, String> headers = new java.util.HashMap<>();
            headers.put("User-Agent", "MCP-News-Crawler/1.0");

            String response = doGet(API_URL, headers);

            if (response != null && !response.isEmpty()) {
                JsonNode json = JsonUtils.getMapper().readTree(response);
                JsonNode data = json.get("data");
                if (data != null) {
                    JsonNode children = data.get("children");
                    if (children != null && children.isArray()) {
                        int rank = 1;
                        for (JsonNode child : children) {
                            JsonNode itemData = child.get("data");
                            if (itemData != null) {
                                String title = itemData.has("title") ? itemData.get("title").asText() : "";
                                String permalink = itemData.has("permalink") ? itemData.get("permalink").asText() : "";
                                String url = "https://www.reddit.com" + permalink;
                                String id = itemData.has("id") ? itemData.get("id").asText() : "";
                                long ups = itemData.has("ups") ? itemData.get("ups").asLong() : 0L;
                                long numComments = itemData.has("num_comments") ? itemData.get("num_comments").asLong()
                                        : 0L;
                                String subreddit = itemData.has("subreddit") ? itemData.get("subreddit").asText() : "";

                                if (!title.isEmpty()) {
                                    NewsItem newsItem = NewsItem.builder()
                                            .id("reddit_" + id)
                                            .title(title)
                                            .url(url)
                                            .platform(platformId)
                                            .platformName(platformName)
                                            .rank(rank++)
                                            .hotScore(ups)
                                            .hotDesc(formatHotScore(ups) + " upvotes · " + subreddit) // 显示点赞数和子版块
                                            .tag(subreddit)
                                            .timestamp(System.currentTimeMillis())
                                            .build();
                                    items.add(newsItem);
                                }
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Reddit 爬取失败: {}", e.getMessage());
        }
        return items;
    }

    private String formatHotScore(Long score) {
        if (score >= 1000) {
            return String.format("%.1fk", score / 1000.0);
        } else {
            return String.valueOf(score);
        }
    }
}
