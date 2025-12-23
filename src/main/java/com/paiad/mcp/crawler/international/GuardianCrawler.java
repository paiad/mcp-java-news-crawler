package com.paiad.mcp.crawler.international;

import com.paiad.mcp.crawler.AbstractCrawler;
import com.paiad.mcp.model.pojo.NewsItem;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Guardian 爬虫 (RSS)
 *
 * @author Paiad
 */
public class GuardianCrawler extends AbstractCrawler {

    private static final String API_URL = "https://www.theguardian.com/world/rss";

    public GuardianCrawler() {
        super("guardian", "The Guardian");
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
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

            String xml = doGet(API_URL, headers);

            if (xml == null || xml.isEmpty()) {
                logger.warn("The Guardian: 响应内容为空");
                return items;
            }
            logger.info("The Guardian: 获取到 XML 内容长度: {}", xml.length());

            Document doc = Jsoup.parse(xml, "", Parser.xmlParser());
            Elements itemsList = doc.select("item");

            int rank = 1;
            for (Element element : itemsList) {
                String title = element.select("title").text();
                String link = element.select("link").text();
                String pubDate = element.select("pubDate").text();

                // Guardian RSS often includes categories, dc:creator etc.
                // Description can be long HTML, simplified here if needed, but not storing desc
                // in NewsItem.

                if (title != null && !title.isEmpty()) {
                    NewsItem newsItem = NewsItem.builder()
                            .id("guardian_" + rank)
                            .title(title)
                            .url(link)
                            .platform(platformId)
                            .platformName(platformName)
                            .rank(rank++)
                            .hotScore(0L)
                            .hotDesc(pubDate)
                            .timestamp(System.currentTimeMillis())
                            .build();
                    items.add(newsItem);

                    if (rank > 30)
                        break;
                }
            }

        } catch (Exception e) {
            logger.error("The Guardian 爬取失败: {}", e.getMessage());
        }
        return items;
    }
}
