package com.paiad.mcp.crawler;

import com.paiad.mcp.model.NewsItem;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reddit 爬虫 (使用 RSS Feed)
 * 
 * @author Paiad
 */
public class RedditCrawler extends AbstractCrawler {

    // 使用 Reddit RSS Feed，更稳定不易被封
    private static final String RSS_URL = "https://www.reddit.com/r/all/hot/.rss?limit=25";

    public RedditCrawler() {
        super("reddit", "Reddit");
    }

    @Override
    protected boolean isInternational() {
        return true;
    }

    @Override
    public List<NewsItem> crawl() {
        List<NewsItem> items = new ArrayList<>();
        try {
            // 使用真实浏览器 User-Agent
            Map<String, String> headers = new HashMap<>();
            headers.put("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            headers.put("Accept", "application/rss+xml, application/xml, text/xml, */*");
            headers.put("Accept-Language", "en-US,en;q=0.9");

            String response = doGet(RSS_URL, headers);

            if (response != null && !response.isEmpty()) {
                items = parseRss(response);
            }

        } catch (Exception e) {
            logger.error("Reddit RSS 爬取失败: {}", e.getMessage());
        }
        return items;
    }

    private List<NewsItem> parseRss(String xml) {
        List<NewsItem> items = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // 禁用外部实体以防止 XXE 攻击
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

            // RSS 2.0 格式：<item> 或 Atom 格式：<entry>
            NodeList entries = doc.getElementsByTagName("entry");
            if (entries.getLength() == 0) {
                entries = doc.getElementsByTagName("item");
            }

            int rank = 1;
            for (int i = 0; i < entries.getLength() && i < 25; i++) {
                Element entry = (Element) entries.item(i);

                String title = getElementText(entry, "title");
                String link = getAtomLink(entry);
                if (link.isEmpty()) {
                    link = getElementText(entry, "link");
                }
                String id = getElementText(entry, "id");
                String category = getElementText(entry, "category");

                if (!title.isEmpty() && !link.isEmpty()) {
                    NewsItem newsItem = NewsItem.builder()
                            .id("reddit_" + (id.isEmpty() ? String.valueOf(i) : id.hashCode()))
                            .title(cleanTitle(title))
                            .url(link)
                            .platform(platformId)
                            .platformName(platformName)
                            .rank(rank++)
                            .hotScore(0L)
                            .hotDesc(category.isEmpty() ? "r/all" : category)
                            .tag(category.isEmpty() ? "hot" : category)
                            .timestamp(System.currentTimeMillis())
                            .build();
                    items.add(newsItem);
                }
            }
        } catch (Exception e) {
            logger.error("解析 Reddit RSS 失败: {}", e.getMessage());
        }
        return items;
    }

    private String getElementText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent().trim();
        }
        return "";
    }

    private String getAtomLink(Element entry) {
        NodeList links = entry.getElementsByTagName("link");
        for (int i = 0; i < links.getLength(); i++) {
            Element link = (Element) links.item(i);
            String href = link.getAttribute("href");
            if (!href.isEmpty()) {
                return href;
            }
        }
        return "";
    }

    private String cleanTitle(String title) {
        // 移除 HTML 实体和多余空格
        return title.replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&quot;", "\"")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
