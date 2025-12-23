package com.paiad.mcp.crawler.international;

import com.paiad.mcp.crawler.AbstractCrawler;
import com.paiad.mcp.model.pojo.NewsItem;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hacker News Crawler - 基于 RSS Feed
 * 
 * 使用 hnrss.org 提供的 RSS Feed，比直接抓取 HN 页面更稳定
 * 不会被反爬虫机制阻止
 * 
 * RSS URL: https://hnrss.org/frontpage
 *
 * @author Paiad
 */
public class HackerNewsCrawler extends AbstractCrawler {

    private static final String RSS_URL = "https://hnrss.org/frontpage";

    // 从 description 中提取 Points 的正则
    private static final Pattern POINTS_PATTERN = Pattern.compile("Points:\\s*(\\d+)");
    // 从 description 中提取 Comments 的正则
    private static final Pattern COMMENTS_PATTERN = Pattern.compile("Comments:\\s*(\\d+)");
    // 从 guid 中提取 HN item id 的正则
    private static final Pattern ID_PATTERN = Pattern.compile("id=(\\d+)");

    public HackerNewsCrawler() {
        super("hacker_news", "Hacker News");
    }

    @Override
    protected boolean isInternational() {
        return true;
    }

    @Override
    public List<NewsItem> crawl() {
        List<NewsItem> items = new ArrayList<>();
        try {
            String rss = doGet(RSS_URL);
            if (rss == null || rss.isEmpty()) {
                throw new RuntimeException("Empty response from HN RSS");
            }

            // 使用 Jsoup 的 XML 解析器解析 RSS
            Document doc = Jsoup.parse(rss, "", Parser.xmlParser());
            Elements rssItems = doc.select("item");

            if (rssItems.isEmpty()) {
                logger.warn("No 'item' elements found in RSS. Response length: {}", rss.length());
                throw new RuntimeException("Parse failed: No items found in RSS feed");
            }

            logger.info("HN RSS: Found {} items", rssItems.size());

            int rank = 1;
            for (Element item : rssItems) {
                try {
                    String title = item.selectFirst("title").text();
                    String link = item.selectFirst("link").text();
                    String guid = item.selectFirst("guid") != null
                            ? item.selectFirst("guid").text()
                            : "";
                    String description = item.selectFirst("description") != null
                            ? item.selectFirst("description").text()
                            : "";

                    // 提取 HN item id
                    String hnId = extractId(guid);

                    // 提取分数和评论数
                    long points = extractPoints(description);
                    long comments = extractComments(description);

                    // 热度描述
                    String hotDesc = points > 0
                            ? points + " points, " + comments + " comments"
                            : "N/A";

                    NewsItem newsItem = NewsItem.builder()
                            .id("hn_" + (hnId.isEmpty() ? rank : hnId))
                            .title(title)
                            .url(link)
                            .platform(platformId)
                            .platformName(platformName)
                            .rank(rank++)
                            .hotScore(points)
                            .hotDesc(hotDesc)
                            .tag(points > 100 ? "hot" : "new")
                            .timestamp(System.currentTimeMillis())
                            .build();

                    items.add(newsItem);
                } catch (Exception e) {
                    logger.warn("Parsing individual HN RSS item failed", e);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Hacker News RSS crawl failed: " + e.getMessage(), e);
        }
        return items;
    }

    /**
     * 从 guid 中提取 HN item id
     */
    private String extractId(String guid) {
        Matcher matcher = ID_PATTERN.matcher(guid);
        return matcher.find() ? matcher.group(1) : "";
    }

    /**
     * 从 description 中提取 Points
     */
    private long extractPoints(String description) {
        Matcher matcher = POINTS_PATTERN.matcher(description);
        if (matcher.find()) {
            try {
                return Long.parseLong(matcher.group(1));
            } catch (NumberFormatException e) {
                return 0L;
            }
        }
        return 0L;
    }

    /**
     * 从 description 中提取 Comments 数
     */
    private long extractComments(String description) {
        Matcher matcher = COMMENTS_PATTERN.matcher(description);
        if (matcher.find()) {
            try {
                return Long.parseLong(matcher.group(1));
            } catch (NumberFormatException e) {
                return 0L;
            }
        }
        return 0L;
    }
}
