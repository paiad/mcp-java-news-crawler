package com.paiad.mcp.crawler;

import com.paiad.mcp.model.NewsItem;
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
 * TechCrunch 爬虫 (RSS)
 *
 * @author Paiad
 */
public class TechCrunchCrawler extends AbstractCrawler {

    private static final String API_URL = "https://techcrunch.com/feed/";

    public TechCrunchCrawler() {
        super("techcrunch", "TechCrunch");
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
                logger.warn("TechCrunch: 响应内容为空");
                return items;
            }

            Document doc = Jsoup.parse(xml, "", Parser.xmlParser());
            Elements itemsList = doc.select("item");

            int rank = 1;
            for (Element element : itemsList) {
                String title = element.select("title").text();
                String link = element.select("link").text();
                String pubDate = element.select("pubDate").text();

                if (title != null && !title.isEmpty()) {
                    NewsItem newsItem = NewsItem.builder()
                            .id("techcrunch_" + rank)
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
            logger.error("TechCrunch 爬取失败: {}", e.getMessage());
        }
        return items;
    }
}
