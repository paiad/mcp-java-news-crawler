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
 * B站热榜爬虫
 * 
 * @author Paiad
 */
public class BilibiliCrawler extends AbstractCrawler {

    private static final String HOT_ALL_URL = "https://api.bilibili.com/x/web-interface/popular?ps=50&pn=1";

    public BilibiliCrawler() {
        super("bilibili", "B站");
    }

    @Override
    public List<NewsItem> crawl() {
        List<NewsItem> items = new ArrayList<>();
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("Referer", "https://www.bilibili.com/");

            String response = doGet(HOT_ALL_URL, headers);
            JsonNode json = JsonUtils.getMapper().readTree(response);

            if (json.has("code") && json.get("code").asInt() == 0) {
                JsonNode data = json.get("data");
                JsonNode list = data != null ? data.get("list") : null;

                if (list != null && list.isArray()) {
                    for (int i = 0; i < list.size(); i++) {
                        JsonNode item = list.get(i);

                        String bvid = item.has("bvid") ? item.get("bvid").asText() : "";
                        String title = item.has("title") ? item.get("title").asText() : "";
                        String url = "https://www.bilibili.com/video/" + bvid;

                        JsonNode stat = item.get("stat");
                        Long view = (stat != null && stat.has("view")) ? stat.get("view").asLong() : 0L;

                        NewsItem newsItem = NewsItem.builder()
                                .id("bilibili_" + bvid)
                                .title(title)
                                .url(url)
                                .platform(platformId)
                                .platformName(platformName)
                                .rank(i + 1)
                                .hotScore(view)
                                .hotDesc(formatViewCount(view) + "播放")
                                .timestamp(System.currentTimeMillis())
                                .build();

                        items.add(newsItem);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("B站热榜爬取失败: {}", e.getMessage(), e);
        }
        return items;
    }

    private String formatViewCount(Long view) {
        if (view >= 100000000) {
            return String.format("%.1f亿", view / 100000000.0);
        } else if (view >= 10000) {
            return String.format("%.1f万", view / 10000.0);
        } else {
            return String.valueOf(view);
        }
    }
}
