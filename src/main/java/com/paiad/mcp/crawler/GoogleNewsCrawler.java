package com.paiad.mcp.crawler;

import com.paiad.mcp.model.NewsItem;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

/**
 * Google News 爬虫
 * 
 * @author Paiad
 */
public class GoogleNewsCrawler extends AbstractCrawler {

    // Google News RSS Feed (US Edition by default, can be localized)
    private static final String API_URL = "https://news.google.com/rss?hl=en-US&gl=US&ceid=US:en";

    public GoogleNewsCrawler() {
        super("google_news", "Google News");
    }

    @Override
    public List<NewsItem> crawl() {
        List<NewsItem> items = new ArrayList<>();
        try {
            String xml = doGet(API_URL);
            // 使用 Jsoup 解析 XML
            Document doc = Jsoup.parse(xml, "", Parser.xmlParser());
            Elements itemsList = doc.select("item");

            int rank = 1;
            for (Element element : itemsList) {
                String title = element.select("title").text();
                String link = element.select("link").text();
                // Guid 有时候包含更多信息，或者用 pubDate
                String pubDate = element.select("pubDate").text();

                // 尝试从 description 提取更多信息有点复杂，暂时只取标题链接

                if (title != null && !title.isEmpty()) {
                    NewsItem newsItem = NewsItem.builder()
                            .id("google_news_" + rank)
                            .title(title)
                            .url(link)
                            .platform(platformId)
                            .platformName(platformName)
                            .rank(rank++)
                            .hotScore(0L) // RSS 不提供热度数值
                            .hotDesc(pubDate)
                            .timestamp(System.currentTimeMillis())
                            .build();
                    items.add(newsItem);

                    if (rank > 30)
                        break; // 限制数量
                }
            }

        } catch (Exception e) {
            logger.error("Google News 爬取失败: {}", e.getMessage());
        }
        return items;
    }
}
