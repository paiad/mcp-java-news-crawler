package com.paiad.mcp.crawler.international;

import com.paiad.mcp.crawler.AbstractCrawler;

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
 * AP News 爬虫 (HTML)
 *
 * @author Paiad
 */
public class APNewsCrawler extends AbstractCrawler {

    private static final String URL = "https://apnews.com/";

    public APNewsCrawler() {
        super("apnews", "AP News");
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

            String html = doGet(URL, headers);

            if (html == null || html.isEmpty()) {
                logger.warn("AP News: 响应内容为空");
                return items;
            }

            Document doc = Jsoup.parse(html);
            // AP News structure: Generic fallback if specific classes fail
            // Select all links that look like articles
            Elements storyElements = doc.select("div.PagePromo");
            if (storyElements.isEmpty()) {
                // Fallback: look for any link containing "/article/"
                storyElements = doc.select("a[href*='/article/']");
            }

            int rank = 1;
            for (Element element : storyElements) {
                String link = "";
                String title = "";

                if (element.tagName().equals("a")) {
                    link = element.attr("href");
                    title = element.text();
                    // Sometimes title is missing in the A tag text (e.g. image link), try to find a
                    // sibling or child
                    if (title.isEmpty()) {
                        title = element.attr("aria-label"); // Try aria-label
                    }
                } else {
                    Element linkEl = element.selectFirst("a.Link");
                    if (linkEl == null)
                        linkEl = element.selectFirst("a"); // Fallback

                    if (linkEl != null) {
                        link = linkEl.attr("href");
                        Element titleEl = linkEl.selectFirst(".PagePromoContentIcons-text");
                        if (titleEl != null) {
                            title = titleEl.text();
                        } else {
                            title = linkEl.text();
                        }
                    }
                }

                // If still empty, look for h3 within the promo
                if (title.isEmpty()) {
                    Element h3 = element.selectFirst("h3");
                    if (h3 != null)
                        title = h3.text();
                }

                if (title != null && !title.isEmpty() && link != null && !link.isEmpty()) {
                    if (!link.startsWith("http")) {
                        link = "https://apnews.com" + link;
                    }

                    // Filter duplicate titles or bad links
                    if (title.equalsIgnoreCase("AP"))
                        continue;

                    NewsItem newsItem = NewsItem.builder()
                            .id("apnews_" + rank)
                            .title(title)
                            .url(link)
                            .platform(platformId)
                            .platformName(platformName)
                            .rank(rank++)
                            .hotScore(0L)
                            .hotDesc("Top Story")
                            .timestamp(System.currentTimeMillis())
                            .build();
                    items.add(newsItem);

                    if (rank > 20)
                        break;
                }
            }

        } catch (Exception e) {
            logger.error("AP News 爬取失败: {}", e.getMessage());
        }
        return items;
    }
}
