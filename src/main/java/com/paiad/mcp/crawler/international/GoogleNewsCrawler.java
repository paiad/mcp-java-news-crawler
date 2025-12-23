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
    protected boolean isInternational() {
        return true;
    }

    @Override
    public List<NewsItem> crawl() {
        List<NewsItem> items = new ArrayList<>();
        try {
            // 设置适合 RSS 的请求头
            Map<String, String> headers = new HashMap<>();
            headers.put("Accept", "application/rss+xml, application/xml, text/xml, */*;q=0.8");
            headers.put("Accept-Language", "en-US,en;q=0.9");
            // 不设置 Accept-Encoding，让 OkHttp 自动处理

            String xml = doGet(API_URL, headers);

            // 调试：检查响应内容
            if (xml == null || xml.isEmpty()) {
                logger.warn("Google News: 响应内容为空");
                return items;
            }

            // 使用 Jsoup 解析 XML
            Document doc = Jsoup.parse(xml, "", Parser.xmlParser());
            Elements itemsList = doc.select("item");

            logger.debug("Google News: 解析到 {} 条 RSS 项目", itemsList.size());

            int rank = 1;
            for (Element element : itemsList) {
                String title = element.select("title").text();

                // RSS 中的 link 元素可能是自闭合标签，需要获取其后的文本节点
                // 也可以尝试从 guid 或其他元素获取链接
                String link = element.select("link").text();
                if (link.isEmpty()) {
                    // 尝试从 link 元素后面的文本节点获取
                    Element linkElement = element.selectFirst("link");
                    if (linkElement != null && linkElement.nextSibling() != null) {
                        link = linkElement.nextSibling().toString().trim();
                    }
                }
                if (link.isEmpty()) {
                    // 尝试从 guid 获取
                    link = element.select("guid").text();
                }

                String pubDate = element.select("pubDate").text();

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
