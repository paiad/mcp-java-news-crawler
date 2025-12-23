package com.paiad.mcp.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paiad.mcp.config.PlatformPriorityConfig;
import com.paiad.mcp.model.pojo.CrawlResult;
import com.paiad.mcp.model.pojo.NewsItem;
import com.paiad.mcp.model.vo.NewsItemVO;
import com.paiad.mcp.service.NewsService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 获取热点新闻工具
 * 支持多平台新闻获取和去重聚合
 */
public class GetHotNewsTool implements McpTool {

    private final NewsService newsService;

    public GetHotNewsTool(NewsService newsService) {
        this.newsService = newsService;
    }

    @Override
    public String getName() {
        return "get_hot_news";
    }

    @Override
    public String getDescription() {
        PlatformPriorityConfig config = PlatformPriorityConfig.getInstance();
        List<String> defaultPlatforms = config.getDefaultPlatformIds();
        return "Get current top trending news from multiple platforms. "
                + "IMPORTANT: Do NOT specify 'platforms' parameter - the system will automatically use the top "
                + defaultPlatforms.size() + " platforms by priority: " + String.join(", ", defaultPlatforms) + ". "
                + "Supports deduplication and cross-platform aggregation.";
    }

    @Override
    public JsonNode getInputSchema(ObjectMapper objectMapper) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = objectMapper.createObjectNode();

        ObjectNode platformsProp = objectMapper.createObjectNode();
        platformsProp.put("type", "array");
        ObjectNode items = objectMapper.createObjectNode();
        items.put("type", "string");
        platformsProp.set("items", items);
        // 从配置中动态获取默认平台列表
        PlatformPriorityConfig config = PlatformPriorityConfig.getInstance();
        List<String> defaultPlatforms = config.getDefaultPlatformIds();
        String allPlatforms = config.getEnabledPlatformIdsSorted().stream()
                .collect(Collectors.joining(", "));
        platformsProp.put("description",
                "Optional. Platform IDs to fetch news from. RECOMMENDED: Do NOT specify this parameter. "
                        + "Default: automatically uses top " + defaultPlatforms.size() + " platforms by priority ("
                        + String.join(", ", defaultPlatforms) + "). "
                        + "All available platforms: " + allPlatforms);
        properties.set("platforms", platformsProp);

        ObjectNode limitProp = objectMapper.createObjectNode();
        limitProp.put("type", "integer");
        limitProp.put("description", "Result limit, default 50, max 200");
        limitProp.put("default", 50);
        properties.set("limit", limitProp);

        ObjectNode dedupeProp = objectMapper.createObjectNode();
        dedupeProp.put("type", "boolean");
        dedupeProp.put("description",
                "Remove duplicate news by clustering similar titles and rank by cross-platform coverage. Use this when user wants 'summary' or 'top headlines'. Default false.");
        dedupeProp.put("default", false);
        properties.set("dedupe", dedupeProp);

        schema.set("properties", properties);
        schema.set("required", objectMapper.createArrayNode());

        return schema;
    }

    @Override
    public String execute(JsonNode arguments, ObjectMapper objectMapper) throws Exception {
        List<String> platforms = null;
        if (arguments.has("platforms") && arguments.get("platforms").isArray()) {
            platforms = new ArrayList<>();
            for (JsonNode p : arguments.get("platforms")) {
                platforms.add(p.asText());
            }
        }

        int limit = arguments.has("limit") ? arguments.get("limit").asInt(50) : 50;
        limit = Math.min(limit, 200);

        boolean dedupe = arguments.has("dedupe") && arguments.get("dedupe").asBoolean(false);

        CrawlResult crawlResult = newsService.getHotNews(platforms, dedupe ? 200 : limit);
        List<NewsItem> news = crawlResult.getData();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);

        if (dedupe) {
            // 去重聚合模式
            List<NewsCluster> clusters = clusterNews(news);

            // 按跨平台覆盖度和热度排序
            clusters.sort((a, b) -> {
                int cmp = Integer.compare(b.platformCount, a.platformCount);
                if (cmp != 0)
                    return cmp;
                return Long.compare(b.totalHotScore, a.totalHotScore);
            });

            // 取 Top N
            List<Map<String, Object>> summaryItems = new ArrayList<>();
            int count = 0;
            for (NewsCluster cluster : clusters) {
                if (count >= limit)
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

            result.put("count", summaryItems.size());
            result.put("total_news_analyzed", news.size());
            result.put("data", summaryItems);
        } else {
            // 普通模式 - 转换为简化的 VO 格式
            result.put("count", news.size());
            List<NewsItemVO> formattedNews = news.stream()
                    .map(NewsItem::toVO)
                    .collect(Collectors.toList());
            result.put("data", formattedNews);
        }

        if (crawlResult.hasFailures()) {
            result.put("failures", crawlResult.getFailures());
        }

        result.put("timestamp", System.currentTimeMillis());

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
                matchedCluster.addNews(item);
            } else {
                NewsCluster newCluster = new NewsCluster();
                newCluster.addNews(item);
                clusters.add(newCluster);
            }
        }

        return clusters;
    }

    /**
     * 判断两个标题是否相似 (使用 Jaccard 相似度)
     */
    private boolean isSimilar(String title1, String title2) {
        if (title1 == null || title2 == null)
            return false;

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

        Set<Character> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<Character> union = new HashSet<>(set1);
        union.addAll(set2);

        double similarity = (double) intersection.size() / union.size();
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
        }
    }
}
