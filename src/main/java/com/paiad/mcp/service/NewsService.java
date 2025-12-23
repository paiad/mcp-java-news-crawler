package com.paiad.mcp.service;

import com.hankcs.hanlp.HanLP;
import com.paiad.mcp.config.PlatformPriorityConfig;
import com.paiad.mcp.config.PlatformConfig;
import com.paiad.mcp.crawler.AbstractCrawler;
import com.paiad.mcp.crawler.domestic.*;
import com.paiad.mcp.crawler.international.*;
import com.paiad.mcp.model.pojo.CrawlResult;
import com.paiad.mcp.model.pojo.NewsItem;
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
     * 平台优先级配置
     */
    private final PlatformPriorityConfig priorityConfig;

    public NewsService() {
        this.crawlers = new HashMap<>();
        // 使用虚拟线程 (Java 21+)，每个任务一个虚拟线程，更轻量高效
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        // 加载平台优先级配置
        this.priorityConfig = PlatformPriorityConfig.getInstance();
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
        addCrawler(new RedditCrawler());
        addCrawler(new GoogleNewsCrawler());
        addCrawler(new WallStreetCnCrawler());
        addCrawler(new BBCCrawler());
        addCrawler(new ReutersCrawler());
        addCrawler(new APNewsCrawler());
        addCrawler(new GuardianCrawler());
        addCrawler(new TechCrunchCrawler());
        addCrawler(new HackerNewsCrawler());
        logger.info("初始化 {} 个平台爬虫", crawlers.size());
    }

    private void addCrawler(AbstractCrawler crawler) {
        crawlers.put(crawler.getPlatformId(), crawler);
    }

    /**
     * 获取热点新闻
     *
     * @param platforms 平台列表，为空则获取默认平台
     * @param limit     返回条数限制
     * @return 爬取结果（包含数据和失败信息）
     */
    public CrawlResult getHotNews(List<String> platforms, int limit) {
        // 确定目标平台 (利用 Registry 解析别名)
        Set<String> targetPlatformIds = new LinkedHashSet<>();

        if (platforms == null || platforms.isEmpty()) {
            // 未指定平台时，按优先级配置获取默认平台
            List<String> defaultPlatforms = priorityConfig.getDefaultPlatformIds();
            for (String pid : defaultPlatforms) {
                if (crawlers.containsKey(pid) && priorityConfig.isEnabled(pid)) {
                    targetPlatformIds.add(pid);
                }
            }
            // 如果配置的默认平台不足，补充其他启用的平台
            if (targetPlatformIds.isEmpty()) {
                for (String pid : priorityConfig.sortByPriority(crawlers.keySet())) {
                    if (priorityConfig.isEnabled(pid)) {
                        targetPlatformIds.add(pid);
                    }
                }
            }
            logger.info("未指定平台，使用默认优先级平台: {}", targetPlatformIds);
        } else {
            for (String p : platforms) {
                String resolvedId = PlatformConfig.resolveId(p);
                if (resolvedId != null && crawlers.containsKey(resolvedId)) {
                    if (priorityConfig.isEnabled(resolvedId)) {
                        targetPlatformIds.add(resolvedId);
                    } else {
                        logger.warn("平台已被禁用: {}", resolvedId);
                    }
                } else {
                    logger.warn("未知或不支持的平台: {}", p);
                }
            }
        }

        // 按优先级排序目标平台
        List<String> sortedPlatforms = priorityConfig.sortByPriority(targetPlatformIds);

        // 直接爬取所有目标平台
        CrawlResult crawlResult = crawlPlatforms(sortedPlatforms);
        List<NewsItem> result = crawlResult.getData();

        // 如果用户未指定 limit，使用合理的默认值（AI 调用时通常会传入 limit）
        int effectiveLimit = limit > 0 ? limit : 50;
        if (result.size() > effectiveLimit) {
            result = result.subList(0, effectiveLimit);
        }

        return new CrawlResult(result, crawlResult.getFailures());
    }

    /**
     * 并行爬取指定平台
     *
     * @return 爬取结果（包含数据和失败信息）
     */
    private CrawlResult crawlPlatforms(List<String> platformIds) {
        logger.info("开始爬取 {} 个平台: {}", platformIds.size(), platformIds);
        Map<String, String> failures = new HashMap<>();
        List<NewsItem> allNews = new ArrayList<>();

        // 准备爬虫任务
        Map<String, Future<List<NewsItem>>> futures = new LinkedHashMap<>();
        for (String platformId : platformIds) {
            AbstractCrawler crawler = crawlers.get(platformId);
            if (crawler != null) {
                futures.put(platformId, executorService.submit(crawler::safeCrawl));
            }
        }

        // 按平台顺序收集结果
        for (String platformId : platformIds) {
            Future<List<NewsItem>> future = futures.get(platformId);
            if (future == null)
                continue;

            try {
                List<NewsItem> items = future.get(45, TimeUnit.SECONDS);
                if (items == null)
                    items = Collections.emptyList();

                allNews.addAll(items);
                logger.info("[{}] 爬取完成，共 {} 条", platformId, items.size());
            } catch (Exception e) {
                logger.error("[{}] 爬取失败: {}", platformId, e.getMessage());
                failures.put(platformId, e.getMessage());
            }
        }

        return new CrawlResult(allNews, failures);
    }

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

        // 1. 确定搜索范围（按优先级排序）
        Set<String> targetPlatformIds = new LinkedHashSet<>();
        if (platforms == null || platforms.isEmpty()) {
            // 搜索时使用默认平台
            for (String pid : priorityConfig.getDefaultPlatformIds()) {
                if (crawlers.containsKey(pid) && priorityConfig.isEnabled(pid)) {
                    targetPlatformIds.add(pid);
                }
            }
        } else {
            for (String p : platforms) {
                String resolvedId = PlatformConfig.resolveId(p);
                if (resolvedId != null && crawlers.containsKey(resolvedId) && priorityConfig.isEnabled(resolvedId)) {
                    targetPlatformIds.add(resolvedId);
                }
            }
        }

        // 2. 直接爬取目标平台
        List<String> sortedPlatforms = priorityConfig.sortByPriority(targetPlatformIds);
        CrawlResult crawlResult = crawlPlatforms(sortedPlatforms);
        List<NewsItem> allNews = crawlResult.getData();

        // 3. 准备分词
        // 使用 HanLP 标准分词
        List<String> queryTerms = HanLP.segment(keyword).stream()
                .map(term -> term.word.toLowerCase())
                .filter(w -> w.length() > 1 || Character.isDigit(w.charAt(0)) || Character.isLetter(w.charAt(0)))
                .collect(Collectors.toList());

        // 如果分词结果为空（比如全是标点），退化为原词
        if (queryTerms.isEmpty()) {
            queryTerms.add(keyword);
        }

        logger.info("搜索关键词分词: {} -> {}", keyword, queryTerms);

        // 4. 执行搜索过滤 & 评分
        List<Map.Entry<NewsItem, Integer>> scoredNews = new ArrayList<>();

        for (NewsItem item : allNews) {
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

        // 按分数降序排序
        scoredNews.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        List<NewsItem> matched = scoredNews.stream()
                .map(Map.Entry::getKey)
                .limit(limit > 0 ? limit : 20)
                .collect(Collectors.toList());

        return new CrawlResult(matched, crawlResult.getFailures());
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
