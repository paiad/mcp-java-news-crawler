package com.paiad.mcp.crawler;

import com.paiad.mcp.model.NewsItem;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reuters 爬虫 (HTML)
 *
 * @author Paiad
 */
public class ReutersCrawler extends AbstractCrawler {

    private static final String URL = "https://www.reuters.com/world/";

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
            headers.put("Accept",
                    "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
            headers.put("Accept-Language", "en-US,en;q=0.9");
            headers.put("Upgrade-Insecure-Requests", "1");
            headers.put("Sec-Fetch-Dest", "document");
            headers.put("Sec-Fetch-Mode", "navigate");
            headers.put("Sec-Fetch-Site", "none");
            headers.put("Sec-Fetch-User", "?1");

            String html = doGet(URL, headers);

            if (html == null || html.isEmpty()) {
                logger.warn("Reuters: 响应内容为空");
                return items;
            }

            Document doc = Jsoup.parse(html);
            // Reuters structure is complex and changes often. Targeting common story
            // containers.
            // Look for links inside main content areas.
            // Simplified selector strategy: look for headings/links in specific list items
            // or grid items

            // Try to find story items. This selector is a guess based on common class names
            // or structure,
            // but might need adjustment as Reuters uses generated class names.
            // A safer bet is searching for specific data-testid attributes if available, or
            // just common semantic tags.

            Elements storyElements = doc.select("li[data-testid='heading'], div[data-testid='media-story-card']");

            // If primary selector fails, try a broader fallback for story links
            if (storyElements.isEmpty()) {
                storyElements = doc.select("a[href^='/world/']");
            }

            int rank = 1;
            for (Element element : storyElements) {
                String title = "";
                String link = "";

                if (element.tagName().equals("a")) {
                    title = element.text();
                    link = element.attr("href");
                } else {
                    Element linkEl = element.selectFirst("a");
                    if (linkEl != null) {
                        title = linkEl.text();
                        if (title.isEmpty()) {
                            // Try finding title inside nested elements usually h3 or spanning text
                            title = linkEl.select("h3").text();
                        }
                        if (title.isEmpty()) {
                            // If still empty, maybe it's just text
                            title = linkEl.text();
                        }
                        link = linkEl.attr("href");
                    }
                }

                if (title != null && !title.isEmpty() && link != null && !link.isEmpty()) {
                    // Fix relative URLs
                    if (!link.startsWith("http")) {
                        link = "https://www.reuters.com" + link;
                    }

                    // Filter out non-news links if possible (e.g. category links) - done by length
                    // or context usually
                    if (title.split(" ").length < 3)
                        continue;

                    NewsItem newsItem = NewsItem.builder()
                            .id("reuters_" + rank)
                            .title(title)
                            .url(link)
                            .platform(platformId)
                            .platformName(platformName)
                            .rank(rank++)
                            .hotScore(0L)
                            .hotDesc("Latest")
                            .timestamp(System.currentTimeMillis())
                            .build();
                    items.add(newsItem);

                    if (rank > 20)
                        break;
                }
            }

        } catch (Exception e) {
            logger.error("Reuters 爬取失败: {}", e.getMessage());
        }
        return items;
    }
}
