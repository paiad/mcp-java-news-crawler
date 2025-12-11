package com.paiad.mcp.service;

import com.hankcs.hanlp.HanLP;
import com.paiad.mcp.config.PlatformRegistry;
import com.paiad.mcp.crawler.*;
import com.paiad.mcp.model.CrawlResult;
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
     * @return 爬取结果（包含数据和失败信息）
     */
    public CrawlResult getHotNews(List<String> platforms, int limit, boolean forceRefresh) {
        // 确定目标平台 (利用 Registry 解析别名)
        Set<String> targetPlatformIds = new HashSet<>();
        if (platforms == null || platforms.isEmpty()) {
            targetPlatformIds.addAll(crawlers.keySet());
        } else {
            for (String p : platforms) {
                String resolvedId = PlatformRegistry.resolveId(p);
                if (resolvedId != null && crawlers.containsKey(resolvedId)) {
                    targetPlatformIds.add(resolvedId);
                } else {
                    logger.warn("未知或不支持的平台: {}", p);
                }
            }
        }

        // 找出需要刷新的平台
        List<String> platformsToRefresh = new ArrayList<>();
        for (String platformId : targetPlatformIds) {
            if (forceRefresh || !isPlatformCacheValid(platformId)) {
                platformsToRefresh.add(platformId);
            }
        }

        Map<String, String> failures = new HashMap<>();

        // 刷新需要更新的平台
        if (!platformsToRefresh.isEmpty()) {
            Map<String, String> refreshFailures = refreshPlatformCache(platformsToRefresh);
            failures.putAll(refreshFailures);
        }

        // 从缓存收集结果
        List<NewsItem> result = new ArrayList<>();
        for (String platformId : targetPlatformIds) {
            List<NewsItem> cached = platformCache.get(platformId);
            if (cached != null) {
                result.addAll(cached);
            }
        }

        // 限制返回数量 (按整体而非单平台，或者保持原样)
        // 这里简单做个截断，也可以按热度排序后截断
        if (result.size() > limit && limit > 0) {
            result = result.subList(0, limit);
        }

        return new CrawlResult(result, failures);
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
     *
     * @return 失败的平台及错误信息
     */
    private Map<String, String> refreshPlatformCache(List<String> platformIds) {
        logger.info("开始刷新 {} 个平台的缓存: {}", platformIds.size(), platformIds);
        Map<String, String> failures = new HashMap<>();

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
                List<NewsItem> items = entry.getValue().get(45, TimeUnit.SECONDS); // 稍微增加超时时间
                if (items == null)
                    items = Collections.emptyList();

                // 只有当结果不为空，或者确实没数据时才算成功?
                // Crawler safeCrawl should return partial list or empty.
                // 如果 safeCrawl 内部catch了异常返回空list，我们不好区分是失败还是没数据。
                // 建议 Crawler 抛出异常让我们捕获，或者 Crawler 返回特定结构。
                // 目前 Crawler.safeCrawl 只是简单的 try-catch 打印日志返回空。
                // 既然无法区分，暂且认为返回空列表且日志有错就是失败。
                // 但为了不改动 Crawler 接口，这里先假设都成功，除非 Future 抛异常。
                // 更好的做法是修改 Crawler 接口，但这里先不做大改动。

                platformCache.put(platformId, items);
                platformCacheTime.put(platformId, System.currentTimeMillis());
                logger.info("[{}] 缓存更新完成，共 {} 条", platformId, items.size());

                if (items.isEmpty()) {
                    // 如果返回空，可能是失败了（基于现有实现习惯）
                    // failures.put(platformId, "No data returned (crawl failed or empty)");
                }

            } catch (Exception e) {
                logger.error("[{}] 爬取任务执行失败: {}", platformId, e.getMessage());
                failures.put(platformId, e.getMessage());
                // 失败时保留旧缓存或设置空列表
                if (!platformCache.containsKey(platformId)) {
                    platformCache.put(platformId, Collections.emptyList());
                    platformCacheTime.put(platformId, System.currentTimeMillis());
                }
            }
        }
        return failures;
    }

    // getAllCachedNews removed

    /**
     * 搜索新闻
     *
     * @param query     搜索关键词
     * @param platforms 平台列表
     * @param limit     返回条数
     * @return 匹配的新闻列表（包装在 CrawlResult 中，包含可能的错误）
     */
    public CrawlResult searchNews(String query, List<String> platforms, int limit) {
        if (query == null || query.trim().isEmpty()) {
            return new CrawlResult(Collections.emptyList(), Collections.emptyMap());
        }
        String keyword = query.trim().toLowerCase();

        // 1. 确定搜索范围
        Set<String> targetPlatformIds = new HashSet<>();
        if (platforms == null || platforms.isEmpty()) {
            targetPlatformIds.addAll(crawlers.keySet());
        } else {
            for (String p : platforms) {
                String resolvedId = PlatformRegistry.resolveId(p);
                if (resolvedId != null && crawlers.containsKey(resolvedId)) {
                    targetPlatformIds.add(resolvedId);
                }
            }
        }

        // 2. 检查缓存，如果有平台未缓存或过期，则触发爬取
        List<String> needToCrawl = new ArrayList<>();
        for (String pid : targetPlatformIds) {
            if (!isPlatformCacheValid(pid)) {
                needToCrawl.add(pid);
            }
        }
        Map<String, String> failures = new HashMap<>();
        if (!needToCrawl.isEmpty()) {
            logger.info("搜索触发爬取: {}", needToCrawl);
            failures.putAll(refreshPlatformCache(needToCrawl));
        }

        // 3. 准备分词
        // 使用 HanLP 标准分词
        List<String> queryTerms = HanLP.segment(keyword).stream()
                .map(term -> term.word.toLowerCase())
                .filter(w -> w.length() > 1 || Character.isDigit(w.charAt(0)) || Character.isLetter(w.charAt(0))) // 过滤掉单字标点等，保留数字字母
                .collect(Collectors.toList());

        // 如果分词结果为空（比如全是标点），退化为原词
        if (queryTerms.isEmpty()) {
            queryTerms.add(keyword);
        }

        logger.info("搜索关键词分词: {} -> {}", keyword, queryTerms);

        // 4. 执行搜索过滤 & 评分
        List<Map.Entry<NewsItem, Integer>> scoredNews = new ArrayList<>();

        for (String pid : targetPlatformIds) {
            List<NewsItem> cached = platformCache.get(pid);
            if (cached != null) {
                for (NewsItem item : cached) {
                    String title = item.getTitle();
                    if (title == null)
                        continue;
                    String titleLower = title.toLowerCase();

                    int score = 0;
                    // 完整包含关键词，加分
                    if (titleLower.contains(keyword)) {
                        score += 100;
                    }

                    // 分词匹配 (OR 逻辑)
                    for (String term : queryTerms) {
                        if (titleLower.contains(term)) {
                            score += 10;
                        }
                    }

                    // 只要有分词匹配或全词匹配，就保留
                    if (score > 0) {
                        scoredNews.add(new AbstractMap.SimpleEntry<>(item, score));
                    }
                }
            }
        }

        // 按分数降序排序
        scoredNews.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        List<NewsItem> matched = scoredNews.stream()
                .map(Map.Entry::getKey)
                .limit(limit > 0 ? limit : 20)
                .collect(Collectors.toList());

        return new CrawlResult(matched, failures);
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
