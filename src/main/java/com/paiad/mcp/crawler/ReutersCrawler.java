package com.paiad.mcp.crawler;

import com.paiad.mcp.model.NewsItem;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.parser.Parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reuters 爬虫 (RSS Feed)
 * 
 * 使用 Reuters RSS Feed 获取新闻，避免网页端的 Bot 检测
 *
 * @author Paiad
 */
public class ReutersCrawler extends AbstractCrawler {

    // FiveFilters 维护的 Reuters RSS Feed (Reuters 官方已于 2020 年停止 RSS 服务)
    private static final String RSS_URL = "https://feeds.feedburner.com/reuters/topNews";
    // 备用: Google News Reuters 专题
    private static final String RSS_URL_BACKUP = "https://news.google.com/rss/search?q=site:reuters.com&hl=en-US&gl=US&ceid=US:en";

    public ReutersCrawler() {
        super("reuters", "Reuters");
    }

    @Override
    protected boolean isInternational() {
        return true;
    }

    @Override
    public List<NewsItem> crawl() {
        List<NewsItem> items = new ArrayList<>();
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            headers.put("Accept", "application/rss+xml, application/xml, text/xml, */*");
            headers.put("Accept-Language", "en-US,en;q=0.9");

            String xml = null;

            // 尝试主 Feed
            try {
                xml = doGet(RSS_URL, headers);
            } catch (Exception e) {
                logger.warn("Reuters 主 Feed 请求失败，尝试备用 Feed: {}", e.getMessage());
            }

            // 如果主 Feed 失败，尝试备用
            if (xml == null || xml.isEmpty()) {
                xml = doGet(RSS_URL_BACKUP, headers);
            }

            if (xml == null || xml.isEmpty()) {
                logger.warn("Reuters: RSS Feed 响应为空");
                return items;
            }

            // 使用 XML 解析器解析 RSS
            Document doc = Jsoup.parse(xml, "", Parser.xmlParser());
            Elements itemElements = doc.select("item");

            int rank = 1;
            for (Element item : itemElements) {
                String title = item.selectFirst("title") != null
                        ? item.selectFirst("title").text()
                        : "";
                String link = item.selectFirst("link") != null
                        ? item.selectFirst("link").text()
                        : "";
                String description = item.selectFirst("description") != null
                        ? item.selectFirst("description").text()
                        : "";
                String pubDate = item.selectFirst("pubDate") != null
                        ? item.selectFirst("pubDate").text()
                        : "";

                if (title.isEmpty() || link.isEmpty()) {
                    continue;
                }

                // 清理 HTML 标签
                title = Jsoup.parse(title).text();
                description = Jsoup.parse(description).text();

                NewsItem newsItem = NewsItem.builder()
                        .id("reuters_" + rank)
                        .title(title)
                        .url(link)
                        .platform(platformId)
                        .platformName(platformName)
                        .rank(rank++)
                        .hotScore(0L)
                        .hotDesc(pubDate.isEmpty() ? "Latest" : pubDate)
                        .timestamp(System.currentTimeMillis())
                        .build();
                items.add(newsItem);

                if (rank > 20) {
                    break;
                }
            }

        } catch (Exception e) {
            logger.error("Reuters 爬取失败: {}", e.getMessage());
        }
        return items;
    }
}
