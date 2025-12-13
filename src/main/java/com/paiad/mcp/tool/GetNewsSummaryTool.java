package com.paiad.mcp.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paiad.mcp.model.CrawlResult;
import com.paiad.mcp.model.NewsItem;
import com.paiad.mcp.service.NewsService;

import java.util.*;

/**
 * 获取新闻摘要工具
 * 聚合多平台新闻，去重并返回最重要的头条
 */
public class GetNewsSummaryTool implements McpTool {

    private final NewsService newsService;

    public GetNewsSummaryTool(NewsService newsService) {
        this.newsService = newsService;
    }

    @Override
    public String getName() {
        return "get_news_summary";
    }

    @Override
    public String getDescription() {
        return "Get a summarized view of today's top news across all platforms. Deduplicates similar headlines and ranks by cross-platform coverage.";
    }

    @Override
    public JsonNode getInputSchema(ObjectMapper objectMapper) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = objectMapper.createObjectNode();

        // top_n 参数
        ObjectNode topNProp = objectMapper.createObjectNode();
        topNProp.put("type", "integer");
        topNProp.put("description", "Number of top headlines to return, default 10");
        topNProp.put("default", 10);
        properties.set("top_n", topNProp);

        // refresh 参数
        ObjectNode refreshProp = objectMapper.createObjectNode();
        refreshProp.put("type", "boolean");
        refreshProp.put("description", "Force refresh all platform caches, default false");
        refreshProp.put("default", false);
        properties.set("refresh", refreshProp);

        schema.set("properties", properties);
        schema.set("required", objectMapper.createArrayNode());

        return schema;
    }

    @Override
    public String execute(JsonNode arguments, ObjectMapper objectMapper) throws Exception {
        int topN = arguments.has("top_n") ? arguments.get("top_n").asInt(10) : 10;
        topN = Math.min(topN, 30);
        boolean refresh = arguments.has("refresh") && arguments.get("refresh").asBoolean(false);

        // 获取所有平台的新闻
        CrawlResult crawlResult = newsService.getHotNews(null, 200, refresh);
        List<NewsItem> allNews = crawlResult.getData();

        // 按标题相似度聚合
        List<NewsCluster> clusters = clusterNews(allNews);

        // 按跨平台覆盖度和热度排序
        clusters.sort((a, b) -> {
            // 优先按平台数量排序
            int cmp = Integer.compare(b.platformCount, a.platformCount);
            if (cmp != 0)
                return cmp;
            // 其次按总热度
            return Long.compare(b.totalHotScore, a.totalHotScore);
        });

        // 取 Top N
        List<Map<String, Object>> summaryItems = new ArrayList<>();
        int count = 0;
        for (NewsCluster cluster : clusters) {
            if (count >= topN)
                break;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("rank", count + 1);
            item.put("title", cluster.representativeTitle);
            item.put("platforms", cluster.platforms);
            item.put("platform_count", cluster.platformCount);

            if (cluster.representativeUrl != null) {
                item.put("url", cluster.representativeUrl);
            }

            if (cluster.totalHotScore > 0) {
                item.put("total_hot_score", cluster.totalHotScore);
            }

            summaryItems.add(item);
            count++;
        }

        // 统计平台分布
        Map<String, Integer> platformDistribution = new LinkedHashMap<>();
        for (NewsItem news : allNews) {
            platformDistribution.merge(news.getPlatform(), 1, Integer::sum);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("summary_count", summaryItems.size());
        result.put("total_news_analyzed", allNews.size());
        result.put("platform_distribution", platformDistribution);

        if (crawlResult.hasFailures()) {
            result.put("failures", crawlResult.getFailures());
        }

        result.put("timestamp", System.currentTimeMillis());
        result.put("headlines", summaryItems);

        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
    }

    /**
     * 将新闻按相似标题聚类
     */
    private List<NewsCluster> clusterNews(List<NewsItem> news) {
        List<NewsCluster> clusters = new ArrayList<>();

        for (NewsItem item : news) {
            String title = item.getTitle();
            if (title == null || title.isEmpty())
                continue;

            // 尝试找到相似的现有聚类
            NewsCluster matchedCluster = null;
            for (NewsCluster cluster : clusters) {
                if (isSimilar(title, cluster.representativeTitle)) {
                    matchedCluster = cluster;
                    break;
                }
            }

            if (matchedCluster != null) {
                // 添加到现有聚类
                matchedCluster.addNews(item);
            } else {
                // 创建新聚类
                NewsCluster newCluster = new NewsCluster();
                newCluster.addNews(item);
                clusters.add(newCluster);
            }
        }

        return clusters;
    }

    /**
     * 判断两个标题是否相似
     * 使用简单的 Jaccard 相似度
     */
    private boolean isSimilar(String title1, String title2) {
        if (title1 == null || title2 == null)
            return false;

        // 简单的字符级别相似度检测
        Set<Character> set1 = new HashSet<>();
        Set<Character> set2 = new HashSet<>();

        for (char c : title1.toLowerCase().toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                set1.add(c);
            }
        }
        for (char c : title2.toLowerCase().toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                set2.add(c);
            }
        }

        if (set1.isEmpty() || set2.isEmpty())
            return false;

        // 计算 Jaccard 相似度
        Set<Character> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<Character> union = new HashSet<>(set1);
        union.addAll(set2);

        double similarity = (double) intersection.size() / union.size();

        // 相似度阈值
        return similarity > 0.6;
    }

    /**
     * 新闻聚类
     */
    private static class NewsCluster {
        String representativeTitle;
        String representativeUrl;
        Set<String> platforms = new LinkedHashSet<>();
        int platformCount = 0;
        long totalHotScore = 0;
        int newsCount = 0;

        void addNews(NewsItem item) {
            if (representativeTitle == null) {
                representativeTitle = item.getTitle();
                representativeUrl = item.getUrl();
            }

            platforms.add(item.getPlatformName() != null ? item.getPlatformName() : item.getPlatform());
            platformCount = platforms.size();

            if (item.getHotScore() != null) {
                totalHotScore += item.getHotScore();
            }
            newsCount++;
        }
    }
}
