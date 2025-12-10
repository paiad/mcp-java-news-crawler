package com.paiad.mcp.crawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paiad.mcp.model.NewsItem;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 百度热搜爬虫
 * 
 * @author Paiad
 */
public class BaiduCrawler extends AbstractCrawler {

    private static final String API_URL = "https://top.baidu.com/board?tab=realtime";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BaiduCrawler() {
        super("baidu", "百度");
    }

    @Override
    public List<NewsItem> crawl() {
        List<NewsItem> items = new ArrayList<>();
        try {
            String html = doGet(API_URL);
            Document doc = Jsoup.parse(html);

            // 尝试从 script 标签中提取 JSON 数据
            Elements scripts = doc.getElementsByTag("script");
            for (Element script : scripts) {
                String content = script.html();
                if (content.contains("window.__APOLLO_STATE__")) {
                    int start = content.indexOf("window.__APOLLO_STATE__=") + 24;
                    int end = content.indexOf("};", start) + 1;
                    if (start > 24 && end > start) {
                        String jsonStr = content.substring(start, end);
                        parseApolloState(jsonStr, items);
                        break;
                    }
                }
            }

            // 如果 JSON 解析失败，尝试 HTML 解析
            if (items.isEmpty()) {
                parseHtml(doc, items);
            }

        } catch (Exception e) {
            logger.error("百度热搜爬取失败: {}", e.getMessage(), e);
        }
        return items;
    }

    private void parseApolloState(String jsonStr, List<NewsItem> items) {
        try {
            JsonNode json = objectMapper.readTree(jsonStr);
            int rank = 1;

            Iterator<String> fieldNames = json.fieldNames();
            while (fieldNames.hasNext()) {
                String key = fieldNames.next();
                if (key.startsWith("Card:") && key.contains("realtime")) {
                    JsonNode card = json.get(key);
                    JsonNode content = card.get("content");
                    if (content != null && content.isArray()) {
                        for (int i = 0; i < content.size(); i++) {
                            JsonNode ref = content.get(i);
                            String refKey = ref.has("id") ? ref.get("id").asText() : null;
                            if (refKey != null) {
                                JsonNode item = json.get(refKey);
                                if (item != null) {
                                    String word = item.has("word") ? item.get("word").asText() : null;
                                    String url = item.has("rawUrl") ? item.get("rawUrl").asText() : null;
                                    Long hotScore = item.has("hotScore") ? item.get("hotScore").asLong() : 0L;

                                    if (word != null) {
                                        NewsItem newsItem = NewsItem.builder()
                                                .id("baidu_" + rank)
                                                .title(word)
                                                .url(url != null ? url
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
                            }
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            logger.warn("百度热搜 JSON 解析失败: {}", e.getMessage());
        }
    }

    private void parseHtml(Document doc, List<NewsItem> items) {
        Elements elements = doc.select(".category-wrap_iQLoo .content_1YWBm");
        int rank = 1;

        for (Element element : elements) {
            Element titleEl = element.selectFirst(".c-single-text-ellipsis");
            Element hotEl = element.selectFirst(".hot-index_1Bl1a");

            if (titleEl != null) {
                String title = titleEl.text().trim();
                String href = element.selectFirst("a") != null ? element.selectFirst("a").attr("href") : "";
                String hotText = hotEl != null ? hotEl.text() : "";

                NewsItem newsItem = NewsItem.builder()
                        .id("baidu_" + rank)
                        .title(title)
                        .url(href.isEmpty() ? "https://www.baidu.com/s?wd=" + encodeUrl(title) : href)
                        .platform(platformId)
                        .platformName(platformName)
                        .rank(rank++)
                        .hotScore(parseHotScore(hotText))
                        .hotDesc(hotText)
                        .timestamp(System.currentTimeMillis())
                        .build();

                items.add(newsItem);
            }
        }
    }

    private Long parseHotScore(String text) {
        try {
            return Long.parseLong(text.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0L;
        }
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
