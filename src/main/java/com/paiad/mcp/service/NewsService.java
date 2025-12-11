package com.paiad.mcp.service;

import com.paiad.mcp.crawler.*;
import com.paiad.mcp.model.NewsItem;
import com.paiad.mcp.util.HttpClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 新闻服务
 * 
 * @author Paiad
 */
public class NewsService {

    private static final Logger logger = LoggerFactory.getLogger(NewsService.class);

    /**
     * 爬虫实例映射
     */
    private final Map<String, AbstractCrawler> crawlers;

    /**
     * 线程池
     */
    private final ExecutorService executorService;

    /**
     * 缓存的新闻数据
     */
    private volatile List<NewsItem> cachedNews = new ArrayList<>();

    /**
     * 缓存时间
     */
    private volatile long cacheTime = 0;

    /**
     * 缓存有效期（毫秒）
     */
    private static final long CACHE_TTL = 5 * 60 * 1000; // 5分钟

    public NewsService() {
        this.crawlers = new HashMap<>();
        // 线程池大小增加到12，确保能并发处理所有爬虫
        this.executorService = Executors.newFixedThreadPool(12);
        initCrawlers();
    }

    /**
     * 初始化爬虫
     */
    private void initCrawlers() {
        addCrawler(new ZhihuCrawler());
        addCrawler(new WeiboCrawler());
        addCrawler(new BilibiliCrawler());
        addCrawler(new BaiduCrawler());
        addCrawler(new DouyinCrawler());
        addCrawler(new ToutiaoCrawler());
        addCrawler(new XCrawler());
        addCrawler(new RedditCrawler());
        addCrawler(new GoogleNewsCrawler());
        addCrawler(new WallStreetCnCrawler());
        logger.info("初始化 {} 个平台爬虫", crawlers.size());
    }

    private void addCrawler(AbstractCrawler crawler) {
        crawlers.put(crawler.getPlatformId(), crawler);
    }

    /**
     * 获取热点新闻
     *
     * @param platforms    平台列表，为空则获取全部
     * @param limit        返回条数限制
     * @param forceRefresh 是否强制刷新缓存
     * @return 新闻列表
     */
    public List<NewsItem> getHotNews(List<String> platforms, int limit, boolean forceRefresh) {
        // 检查缓存
        if (!forceRefresh && isCacheValid()) {
            return filterAndLimit(cachedNews, platforms, limit);
        }

        // 刷新缓存
        refreshCache(platforms);
        return filterAndLimit(cachedNews, platforms, limit);
    }

    /**
     * 刷新缓存
     */
    private synchronized void refreshCache(List<String> platforms) {
        // 双重检查
        if (isCacheValid()) {
            return;
        }

        logger.info("开始刷新新闻缓存...");
        List<NewsItem> allNews = new ArrayList<>();

        // 确定要爬取的平台
        Collection<AbstractCrawler> targetCrawlers;
        if (platforms == null || platforms.isEmpty()) {
            targetCrawlers = crawlers.values();
        } else {
            targetCrawlers = platforms.stream()
                    .map(crawlers::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        // 并发爬取
        List<Future<List<NewsItem>>> futures = new ArrayList<>();
        for (AbstractCrawler crawler : targetCrawlers) {
            futures.add(executorService.submit(crawler::safeCrawl));
        }

        // 收集结果
        for (Future<List<NewsItem>> future : futures) {
            try {
                List<NewsItem> items = future.get(30, TimeUnit.SECONDS);
                allNews.addAll(items);
            } catch (Exception e) {
                logger.error("爬取任务执行失败: {}", e.getMessage());
            }
        }

        // 更新缓存
        this.cachedNews = allNews;
        this.cacheTime = System.currentTimeMillis();
        logger.info("缓存刷新完成，共 {} 条新闻", allNews.size());
    }

    /**
     * 检查缓存是否有效
     */
    private boolean isCacheValid() {
        return !cachedNews.isEmpty() &&
                (System.currentTimeMillis() - cacheTime) < CACHE_TTL;
    }

    /**
     * 过滤并限制数量
     */
    private List<NewsItem> filterAndLimit(List<NewsItem> news, List<String> platforms, int limit) {
        return news.stream()
                .filter(item -> platforms == null || platforms.isEmpty() ||
                        platforms.contains(item.getPlatform()))
                .limit(limit > 0 ? limit : 50)
                .collect(Collectors.toList());
    }

    /**
     * 搜索新闻
     *
     * @param query     搜索关键词
     * @param platforms 平台列表
     * @param limit     返回条数
     * @return 匹配的新闻列表
     */
    public List<NewsItem> searchNews(String query, List<String> platforms, int limit) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // 确保有缓存数据
        if (cachedNews.isEmpty()) {
            getHotNews(platforms, 100, false);
        }

        String keyword = query.toLowerCase().trim();

        return cachedNews.stream()
                .filter(item -> platforms == null || platforms.isEmpty() ||
                        platforms.contains(item.getPlatform()))
                .filter(item -> item.getTitle() != null &&
                        item.getTitle().toLowerCase().contains(keyword))
                .limit(limit > 0 ? limit : 20)
                .collect(Collectors.toList());
    }

    /**
     * 获取支持的平台列表
     */
    public List<Map<String, String>> getSupportedPlatforms() {
        return crawlers.values().stream()
                .map(crawler -> {
                    Map<String, String> info = new HashMap<>();
                    info.put("id", crawler.getPlatformId());
                    info.put("name", crawler.getPlatformName());
                    return info;
                })
                .collect(Collectors.toList());
    }

    /**
     * 关闭服务
     */
    public void shutdown() {
        executorService.shutdown();
        // 关闭共享的 HttpClient
        HttpClientFactory.shutdown();
    }
}
