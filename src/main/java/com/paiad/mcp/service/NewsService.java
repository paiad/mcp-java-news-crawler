package com.paiad.mcp.service;

import com.hankcs.hanlp.HanLP;
import com.paiad.mcp.crawler.AbstractCrawler;
import com.paiad.mcp.model.pojo.CrawlResult;
import com.paiad.mcp.model.pojo.NewsItem;
import com.paiad.mcp.model.pojo.PlatformCrawlOutcome;
import com.paiad.mcp.model.pojo.PlatformCrawlStatus;
import com.paiad.mcp.registry.CrawlerRegistry;
import com.paiad.mcp.registry.PlatformRegistry;
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
     * 线程池
     */
    private final ExecutorService executorService;

    /**
     * 平台和爬虫注册中心
     */
    private final PlatformRegistry platformRegistry;
    private final CrawlerRegistry crawlerRegistry;

    public NewsService() {
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.platformRegistry = PlatformRegistry.getInstance();
        this.crawlerRegistry = CrawlerRegistry.getInstance();
        logger.info("初始化 {} 个平台爬虫", crawlerRegistry.getSupportedPlatformIds().size());
        validateRegistryConsistency();
    }

    /**
     * 获取热点新闻
     *
     * @param platforms 平台列表，为空则获取默认平台
     * @param limit     返回条数限制
     * @return 爬取结果（包含数据和失败信息）
     */
    public CrawlResult getHotNews(List<String> platforms, int limit) {
        List<String> sortedPlatforms = resolveTargetPlatforms(platforms, true);
        CrawlResult crawlResult = crawlPlatforms(sortedPlatforms);
        List<NewsItem> result = crawlResult.getData();

        int effectiveLimit = limit > 0 ? limit : 50;
        if (result.size() > effectiveLimit) {
            result = result.subList(0, effectiveLimit);
        }

        return new CrawlResult(result, crawlResult.getFailures(), crawlResult.getOutcomes());
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

        List<String> sortedPlatforms = resolveTargetPlatforms(platforms, false);
        CrawlResult crawlResult = crawlPlatforms(sortedPlatforms);
        List<NewsItem> allNews = crawlResult.getData();

        List<String> queryTerms = HanLP.segment(keyword).stream()
                .map(term -> term.word.toLowerCase())
                .filter(w -> w.length() > 1 || Character.isDigit(w.charAt(0)) || Character.isLetter(w.charAt(0)))
                .collect(Collectors.toList());

        if (queryTerms.isEmpty()) {
            queryTerms.add(keyword);
        }

        logger.info("搜索关键词分词: {} -> {}", keyword, queryTerms);

        List<Map.Entry<NewsItem, Integer>> scoredNews = new ArrayList<>();
        for (NewsItem item : allNews) {
            String title = item.getTitle();
            if (title == null) {
                continue;
            }
            String titleLower = title.toLowerCase();

            int score = 0;
            if (titleLower.contains(keyword)) {
                score += 100;
            }
            for (String term : queryTerms) {
                if (titleLower.contains(term)) {
                    score += 10;
                }
            }
            if (score > 0) {
                scoredNews.add(new AbstractMap.SimpleEntry<>(item, score));
            }
        }

        scoredNews.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        List<NewsItem> matched = scoredNews.stream()
                .map(Map.Entry::getKey)
                .limit(limit > 0 ? limit : 20)
                .collect(Collectors.toList());

        return new CrawlResult(matched, crawlResult.getFailures(), crawlResult.getOutcomes());
    }

    /**
     * 获取支持的平台列表
     */
    public List<Map<String, String>> getSupportedPlatforms() {
        return crawlerRegistry.getAllCrawlers().stream()
                .map(crawler -> {
                    Map<String, String> info = new HashMap<>();
                    info.put("id", crawler.getPlatformId());
                    info.put("name", crawler.getPlatformName());
                    return info;
                })
                .collect(Collectors.toList());
    }

    /**
     * 并行爬取指定平台
     */
    private CrawlResult crawlPlatforms(List<String> platformIds) {
        logger.info("开始爬取 {} 个平台: {}", platformIds.size(), platformIds);
        Map<String, String> failures = new HashMap<>();
        List<NewsItem> allNews = new ArrayList<>();
        List<PlatformCrawlOutcome> outcomes = new ArrayList<>();

        Map<String, Future<PlatformCrawlOutcome>> futures = new LinkedHashMap<>();
        for (String platformId : platformIds) {
            AbstractCrawler crawler = crawlerRegistry.getCrawler(platformId);
            if (crawler != null) {
                futures.put(platformId, executorService.submit(crawler::crawlWithOutcome));
            }
        }

        for (String platformId : platformIds) {
            Future<PlatformCrawlOutcome> future = futures.get(platformId);
            if (future == null) {
                continue;
            }

            try {
                PlatformCrawlOutcome outcome = future.get(45, TimeUnit.SECONDS);
                if (outcome == null) {
                    outcome = new PlatformCrawlOutcome(platformId, platformRegistry.getName(platformId),
                            PlatformCrawlStatus.FAILED, List.of(), "NULL_OUTCOME", "Crawler returned null outcome", 0);
                }
                outcomes.add(outcome);
                allNews.addAll(outcome.items());
                if (outcome.isFailure()) {
                    failures.put(platformId, formatFailure(outcome));
                    logger.error("[{}] 爬取失败: {}", platformId, failures.get(platformId));
                } else {
                    logger.info("[{}] 爬取完成，共 {} 条", platformId, outcome.items().size());
                }
            } catch (TimeoutException e) {
                future.cancel(true);
                PlatformCrawlOutcome timeoutOutcome = new PlatformCrawlOutcome(platformId, platformRegistry.getName(platformId),
                        PlatformCrawlStatus.TIMEOUT, List.of(), "TIMEOUT", "Platform crawl timed out", 45_000);
                outcomes.add(timeoutOutcome);
                failures.put(platformId, formatFailure(timeoutOutcome));
                logger.error("[{}] 爬取超时", platformId);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                PlatformCrawlOutcome failedOutcome = new PlatformCrawlOutcome(platformId, platformRegistry.getName(platformId),
                        PlatformCrawlStatus.FAILED, List.of(), cause.getClass().getSimpleName(), cause.getMessage(), 0);
                outcomes.add(failedOutcome);
                failures.put(platformId, formatFailure(failedOutcome));
                logger.error("[{}] 爬取失败: {}", platformId, cause.getMessage());
            } catch (Exception e) {
                PlatformCrawlOutcome failedOutcome = new PlatformCrawlOutcome(platformId, platformRegistry.getName(platformId),
                        PlatformCrawlStatus.FAILED, List.of(), e.getClass().getSimpleName(), e.getMessage(), 0);
                outcomes.add(failedOutcome);
                failures.put(platformId, formatFailure(failedOutcome));
                logger.error("[{}] 爬取失败: {}", platformId, e.getMessage());
            }
        }

        return new CrawlResult(allNews, failures, outcomes);
    }

    private List<String> resolveTargetPlatforms(List<String> platforms, boolean fallbackWhenDefaultEmpty) {
        Set<String> supported = crawlerRegistry.getSupportedPlatformIds();
        Set<String> targetPlatformIds = new LinkedHashSet<>();

        if (platforms == null || platforms.isEmpty()) {
            for (String pid : platformRegistry.getDefaultPlatformIds()) {
                if (supported.contains(pid) && platformRegistry.isEnabled(pid)) {
                    targetPlatformIds.add(pid);
                }
            }
            if (fallbackWhenDefaultEmpty && targetPlatformIds.isEmpty()) {
                for (String pid : platformRegistry.sortByPriority(supported)) {
                    if (platformRegistry.isEnabled(pid)) {
                        targetPlatformIds.add(pid);
                    }
                }
            }
            logger.info("未指定平台，使用默认优先级平台: {}", targetPlatformIds);
        } else {
            for (String p : platforms) {
                String resolvedId = platformRegistry.resolveId(p);
                if (resolvedId != null && supported.contains(resolvedId)) {
                    if (platformRegistry.isEnabled(resolvedId)) {
                        targetPlatformIds.add(resolvedId);
                    } else {
                        logger.warn("平台已被禁用: {}", resolvedId);
                    }
                } else {
                    logger.warn("未知或不支持的平台: {}", p);
                }
            }
        }

        return platformRegistry.sortByPriority(targetPlatformIds);
    }

    /**
     * 关闭服务
     */
    public void shutdown() {
        executorService.shutdown();
        HttpClientFactory.shutdown();
    }

    private String formatFailure(PlatformCrawlOutcome outcome) {
        String code = outcome.errorCode() != null ? outcome.errorCode() : "UNKNOWN";
        String message = outcome.errorMessage() != null ? outcome.errorMessage() : "Unknown error";
        return code + ": " + message;
    }

    private void validateRegistryConsistency() {
        boolean strict = Boolean.parseBoolean(System.getenv().getOrDefault("STRICT_PLATFORM_VALIDATION", "false"));
        List<String> missing = findMissingCrawlerRegistrations(
                platformRegistry.getEnabledPlatformIdsSorted(),
                crawlerRegistry.getSupportedPlatformIds());
        enforceRegistryConsistency(missing, strict);
        if (!missing.isEmpty()) {
            logger.warn("启用平台未注册爬虫: {}，系统将以降级模式运行", String.join(", ", missing));
        }
    }

    static List<String> findMissingCrawlerRegistrations(Collection<String> enabledPlatformIds,
            Set<String> supportedPlatformIds) {
        if (enabledPlatformIds == null || enabledPlatformIds.isEmpty()) {
            return List.of();
        }
        Set<String> supported = supportedPlatformIds == null ? Set.of() : supportedPlatformIds;
        return enabledPlatformIds.stream()
                .filter(id -> !supported.contains(id))
                .sorted()
                .toList();
    }

    static void enforceRegistryConsistency(List<String> missingPlatformIds, boolean strict) {
        if (missingPlatformIds == null || missingPlatformIds.isEmpty()) {
            return;
        }
        if (strict) {
            throw new IllegalStateException("启用平台未注册爬虫: " + String.join(", ", missingPlatformIds));
        }
    }
}
