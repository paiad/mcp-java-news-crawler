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
     * 按平台缓存的新闻数据
     */
    private final Map<String, List<NewsItem>> platformCache = new ConcurrentHashMap<>();

    /**
     * 按平台缓存的时间
     */
    private final Map<String, Long> platformCacheTime = new ConcurrentHashMap<>();

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
        // 确定目标平台
        List<String> targetPlatformIds;
        if (platforms == null || platforms.isEmpty()) {
            targetPlatformIds = new ArrayList<>(crawlers.keySet());
        } else {
            targetPlatformIds = platforms.stream()
                    .filter(crawlers::containsKey)
                    .collect(Collectors.toList());
        }

        // 找出需要刷新的平台
        List<String> platformsToRefresh = new ArrayList<>();
        for (String platformId : targetPlatformIds) {
            if (forceRefresh || !isPlatformCacheValid(platformId)) {
                platformsToRefresh.add(platformId);
            }
        }

        // 刷新需要更新的平台
        if (!platformsToRefresh.isEmpty()) {
            refreshPlatformCache(platformsToRefresh);
        }

        // 从缓存收集结果
        List<NewsItem> result = new ArrayList<>();
        for (String platformId : targetPlatformIds) {
            List<NewsItem> cached = platformCache.get(platformId);
            if (cached != null) {
                result.addAll(cached);
            }
        }

        // 限制返回数量
        int actualLimit = limit > 0 ? limit : 50;
        if (result.size() > actualLimit) {
            return result.subList(0, actualLimit);
        }
        return result;
    }

    /**
     * 检查指定平台的缓存是否有效
     */
    private boolean isPlatformCacheValid(String platformId) {
        Long cacheTime = platformCacheTime.get(platformId);
        if (cacheTime == null) {
            return false;
        }
        return (System.currentTimeMillis() - cacheTime) < CACHE_TTL;
    }

    /**
     * 刷新指定平台的缓存
     */
    private void refreshPlatformCache(List<String> platformIds) {
        logger.info("开始刷新 {} 个平台的缓存: {}", platformIds.size(), platformIds);

        // 准备爬虫任务
        Map<String, Future<List<NewsItem>>> futures = new HashMap<>();
        for (String platformId : platformIds) {
            AbstractCrawler crawler = crawlers.get(platformId);
            if (crawler != null) {
                futures.put(platformId, executorService.submit(crawler::safeCrawl));
            }
        }

        // 收集结果并更新缓存
        for (Map.Entry<String, Future<List<NewsItem>>> entry : futures.entrySet()) {
            String platformId = entry.getKey();
            try {
                List<NewsItem> items = entry.getValue().get(30, TimeUnit.SECONDS);
                platformCache.put(platformId, items);
                platformCacheTime.put(platformId, System.currentTimeMillis());
                logger.info("[{}] 缓存更新完成，共 {} 条", platformId, items.size());
            } catch (Exception e) {
                logger.error("[{}] 爬取任务执行失败: {}", platformId, e.getMessage());
                // 失败时保留旧缓存或设置空列表
                if (!platformCache.containsKey(platformId)) {
                    platformCache.put(platformId, Collections.emptyList());
                    platformCacheTime.put(platformId, System.currentTimeMillis());
                }
            }
        }
    }

    /**
     * 获取所有平台的缓存数据（用于搜索）
     */
    private List<NewsItem> getAllCachedNews() {
        List<NewsItem> all = new ArrayList<>();
        for (List<NewsItem> items : platformCache.values()) {
            all.addAll(items);
        }
        return all;
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
        if (platformCache.isEmpty()) {
            getHotNews(platforms, 100, false);
        }

        String keyword = query.toLowerCase().trim();

        return getAllCachedNews().stream()
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
