package com.paiad.mcp.crawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.paiad.mcp.model.NewsItem;
import com.paiad.mcp.util.JsonUtils;

import java.net.URLEncoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 百度热搜爬虫
 *
 * @author Paiad
 */
public class BaiduCrawler extends AbstractCrawler {

    private static final String API_URL = "https://top.baidu.com/board?tab=realtime";

    public BaiduCrawler() {
        super("baidu", "百度");
    }

    @Override
    public List<NewsItem> crawl() {
        List<NewsItem> items = new ArrayList<>();
        try {
            // 添加请求头
            Map<String, String> headers = new HashMap<>();
            headers.put("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

            String html = doGet(API_URL, headers);

            // 方法1: 从 <!--s-data:...--> 注释中提取 (newsnow 方式)
            if (!parseFromSData(html, items)) {
                // 方法2: 尝试从正则匹配
                parseFromRegex(html, items);
            }

        } catch (Exception e) {
            logger.error("百度热搜爬取失败: {}", e.getMessage(), e);
        }
        return items;
    }

    /**
     * 从 <!--s-data:...--> 注释中提取 JSON 数据
     * 参考: https://github.com/ourongxing/newsnow/blob/main/server/sources/baidu.ts
     */
    private boolean parseFromSData(String html, List<NewsItem> items) {
        try {
            // 匹配 <!--s-data:...--> 注释 (newsnow 的方式)
            Pattern pattern = Pattern.compile("<!--s-data:(.*?)-->", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(html);

            if (matcher.find()) {
                String jsonStr = matcher.group(1);
                JsonNode json = JsonUtils.getMapper().readTree(jsonStr);

                // 获取 data.cards[0].content
                JsonNode data = json.get("data");
                if (data != null) {
                    JsonNode cards = data.get("cards");
                    if (cards != null && cards.isArray() && cards.size() > 0) {
                        JsonNode content = cards.get(0).get("content");
                        if (content != null && content.isArray()) {
                            int rank = 1;
                            for (JsonNode item : content) {
                                // 跳过置顶项
                                boolean isTop = item.has("isTop") && item.get("isTop").asBoolean();
                                if (isTop) {
                                    continue;
                                }

                                String word = item.has("word") ? item.get("word").asText() : "";
                                String rawUrl = item.has("rawUrl") ? item.get("rawUrl").asText() : "";
                                String desc = item.has("desc") ? item.get("desc").asText() : "";
                                Long hotScore = item.has("hotScore") ? item.get("hotScore").asLong() : 0L;

                                if (!word.isEmpty()) {
                                    NewsItem newsItem = NewsItem.builder()
                                            .id("baidu_" + rank)
                                            .title(word)
                                            .url(!rawUrl.isEmpty() ? rawUrl
                                                    : "https://www.baidu.com/s?wd=" + encodeUrl(word))
                                            .platform(platformId)
                                            .platformName(platformName)
                                            .rank(rank++)
                                            .hotScore(hotScore)
                                            .hotDesc(formatHotScore(hotScore))
                                            .timestamp(System.currentTimeMillis())
                                            .build();

                                    items.add(newsItem);
                                }
                            }
                            return !items.isEmpty();
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("s-data 解析失败: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 使用正则表达式提取数据 (回退方案)
     */
    private void parseFromRegex(String html, List<NewsItem> items) {
        try {
            // 匹配热搜词条
            Pattern pattern = Pattern
                    .compile("\"word\":\"([^\"]+)\"[^}]*\"rawUrl\":\"([^\"]+)\"[^}]*\"hotScore\":(\\d+)");
            Matcher matcher = pattern.matcher(html);

            int rank = 1;
            while (matcher.find() && rank <= 50) {
                String word = matcher.group(1);
                String rawUrl = matcher.group(2).replace("\\u002F", "/");
                Long hotScore = Long.parseLong(matcher.group(3));

                NewsItem newsItem = NewsItem.builder()
                        .id("baidu_" + rank)
                        .title(word)
                        .url(rawUrl)
                        .platform(platformId)
                        .platformName(platformName)
                        .rank(rank++)
                        .hotScore(hotScore)
                        .hotDesc(formatHotScore(hotScore))
                        .timestamp(System.currentTimeMillis())
                        .build();

                items.add(newsItem);
            }
        } catch (Exception e) {
            logger.debug("正则解析失败: {}", e.getMessage());
        }
    }

    private String encodeUrl(String str) {
        try {
            return URLEncoder.encode(str, "UTF-8");
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
