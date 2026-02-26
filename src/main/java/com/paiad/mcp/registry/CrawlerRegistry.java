package com.paiad.mcp.registry;

import com.paiad.mcp.crawler.AbstractCrawler;
import com.paiad.mcp.crawler.domestic.*;
import com.paiad.mcp.crawler.international.*;

import java.util.*;

/**
 * Centralized crawler registry.
 */
public class CrawlerRegistry {

    private static CrawlerRegistry instance;

    private final Map<String, AbstractCrawler> crawlers;

    private CrawlerRegistry() {
        this.crawlers = new LinkedHashMap<>();
        register(new ZhihuCrawler());
        register(new WeiboCrawler());
        register(new BilibiliCrawler());
        register(new BaiduCrawler());
        register(new DouyinCrawler());
        register(new ToutiaoCrawler());
        register(new RedditCrawler());
        register(new GoogleNewsCrawler());
        register(new WallStreetCnCrawler());
        register(new BBCCrawler());
        register(new ReutersCrawler());
        register(new APNewsCrawler());
        register(new GuardianCrawler());
        register(new TechCrunchCrawler());
        register(new HackerNewsCrawler());
    }

    public static synchronized CrawlerRegistry getInstance() {
        if (instance == null) {
            instance = new CrawlerRegistry();
        }
        return instance;
    }

    private void register(AbstractCrawler crawler) {
        crawlers.put(crawler.getPlatformId(), crawler);
    }

    public AbstractCrawler getCrawler(String platformId) {
        return crawlers.get(platformId);
    }

    public Collection<AbstractCrawler> getAllCrawlers() {
        return Collections.unmodifiableCollection(crawlers.values());
    }

    public Set<String> getSupportedPlatformIds() {
        return Collections.unmodifiableSet(crawlers.keySet());
    }
}
