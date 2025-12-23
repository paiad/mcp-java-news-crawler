package com.paiad.mcp.crawler.domestic;

import com.paiad.mcp.crawler.AbstractCrawler;
import com.paiad.mcp.model.pojo.NewsItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.paiad.mcp.util.JsonUtils;

import java.net.URLEncoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 抖音热榜爬虫
 * 
 * @author Paiad
 */
public class DouyinCrawler extends AbstractCrawler {

    private static final String API_URL = "https://www.douyin.com/aweme/v1/web/hot/search/list/";

    public DouyinCrawler() {
        super("douyin", "抖音");
    }

    @Override
    public List<NewsItem> crawl() {
        List<NewsItem> items = new ArrayList<>();
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("Referer", "https://www.douyin.com/");
            headers.put("Accept", "application/json, text/plain, */*");

            String response = doGet(API_URL, headers);
            JsonNode json = JsonUtils.getMapper().readTree(response);

            Integer statusCode = json.has("status_code") ? json.get("status_code").asInt() : null;
            if (statusCode != null && statusCode == 0) {
                JsonNode data = json.get("data");
                if (data != null) {
                    JsonNode wordList = data.get("word_list");
                    if (wordList != null && wordList.isArray()) {
                        for (int i = 0; i < wordList.size(); i++) {
                            JsonNode item = wordList.get(i);

                            String word = item.has("word") ? item.get("word").asText() : "";
                            Long hotValue = item.has("hot_value") ? item.get("hot_value").asLong() : 0L;
                            String sentence = item.has("sentence_id") ? item.get("sentence_id").asText() : null;
                            Integer label = item.has("label") ? item.get("label").asInt() : null;

                            String url = "https://www.douyin.com/search/" + encodeUrl(word);
                            String tag = getTagFromLabel(label);

                            NewsItem newsItem = NewsItem.builder()
                                    .id("douyin_" + (sentence != null ? sentence : String.valueOf(i)))
                                    .title(word)
                                    .url(url)
                                    .platform(platformId)
                                    .platformName(platformName)
                                    .rank(i + 1)
                                    .hotScore(hotValue)
                                    .hotDesc(formatHotScore(hotValue))
                                    .tag(tag)
                                    .timestamp(System.currentTimeMillis())
                                    .build();

                            items.add(newsItem);
                        }
                    }
                }
            } else {
                // 如果 API 失败，记录日志
                logger.warn("抖音热榜 API 返回非成功状态: {}", response);
            }
        } catch (Exception e) {
            logger.error("抖音热榜爬取失败: {}", e.getMessage(), e);
        }
        return items;
    }

    private String getTagFromLabel(Integer label) {
        if (label == null)
            return null;
        switch (label) {
            case 1:
                return "热";
            case 2:
                return "新";
            case 3:
                return "荐";
            default:
                return null;
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
