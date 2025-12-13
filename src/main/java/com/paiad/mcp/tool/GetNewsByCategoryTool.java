package com.paiad.mcp.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paiad.mcp.config.CategoryConfig;
import com.paiad.mcp.config.PreferencesConfig;
import com.paiad.mcp.model.CrawlResult;
import com.paiad.mcp.model.NewsItem;
import com.paiad.mcp.service.NewsService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 按分类获取新闻工具
 * 
 * 支持两种模式：
 * 1. 指定分类：返回该分类的新闻
 * 2. 不指定分类：根据用户偏好配置(preferences.yml)的权重比例混合返回
 */
public class GetNewsByCategoryTool implements McpTool {

    private final NewsService newsService;

    public GetNewsByCategoryTool(NewsService newsService) {
        this.newsService = newsService;
    }

    @Override
    public String getName() {
        return "get_news_by_category";
    }

    @Override
    public String getDescription() {
        return "Get news by category (ai, tech, finance, entertainment, sports, world, society). " +
                "If no category specified, returns mixed news based on user's preference weights in preferences.yml.";
    }

    @Override
    public JsonNode getInputSchema(ObjectMapper objectMapper) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = objectMapper.createObjectNode();

        // category 参数（可选）
        ObjectNode categoryProp = objectMapper.createObjectNode();
        categoryProp.put("type", "string");
        String availableCategories = CategoryConfig.getInstance().getCategoryIds().stream()
                .collect(Collectors.joining(", "));
        categoryProp.put("description",
                "Category ID (optional). Available: " + availableCategories +
                        ". If not specified, returns news based on user preference weights.");
        ArrayNode enumValues = objectMapper.createArrayNode();
        for (String catId : CategoryConfig.getInstance().getCategoryIds()) {
            enumValues.add(catId);
        }
        categoryProp.set("enum", enumValues);
        properties.set("category", categoryProp);

        // limit 参数
        ObjectNode limitProp = objectMapper.createObjectNode();
        limitProp.put("type", "integer");
        limitProp.put("description", "Maximum number of news to return, default from preferences.yml");
        properties.set("limit", limitProp);

        schema.set("properties", properties);
        // category 不再是必选
        schema.set("required", objectMapper.createArrayNode());

        return schema;
    }

    @Override
    public String execute(JsonNode arguments, ObjectMapper objectMapper) throws Exception {
        String categoryId = arguments.has("category") ? arguments.get("category").asText() : null;

        CategoryConfig categoryConfig = CategoryConfig.getInstance();
        PreferencesConfig preferencesConfig = PreferencesConfig.getInstance();

        int limit = arguments.has("limit")
                ? arguments.get("limit").asInt(preferencesConfig.getDefaultLimit())
                : preferencesConfig.getDefaultLimit();
        limit = Math.min(limit, 100);

        Map<String, Object> result = new LinkedHashMap<>();
        List<NewsItem> allFilteredNews = new ArrayList<>();
        Map<String, String> allFailures = new HashMap<>();
        Map<String, Integer> categoryDistribution = new LinkedHashMap<>();

        if (categoryId != null && !categoryId.isEmpty()) {
            // 模式1：指定分类
            CategoryConfig.CategoryInfo categoryInfo = categoryConfig.getCategory(categoryId);
            if (categoryInfo == null) {
                result.put("success", false);
                result.put("error", "Unknown category: " + categoryId);
                result.put("available_categories", categoryConfig.getCategoryIds());
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
            }

            List<NewsItem> news = fetchNewsByCategory(categoryInfo, limit, allFailures);
            allFilteredNews.addAll(news);
            categoryDistribution.put(categoryInfo.getName(), news.size());

            result.put("mode", "single_category");
            result.put("category", categoryInfo.getName());
            result.put("category_id", categoryId);

        } else {
            // 模式2：根据用户偏好混合
            Map<String, Integer> categoryLimits = preferencesConfig.calculateCategoryLimits(limit);

            result.put("mode", "mixed_by_preference");
            result.put("preference_weights", preferencesConfig.getAllWeights());

            for (Map.Entry<String, Integer> entry : categoryLimits.entrySet()) {
                String catId = entry.getKey();
                int catLimit = entry.getValue();

                CategoryConfig.CategoryInfo categoryInfo = categoryConfig.getCategory(catId);
                if (categoryInfo == null)
                    continue;

                List<NewsItem> news = fetchNewsByCategory(categoryInfo, catLimit, allFailures);
                allFilteredNews.addAll(news);
                if (!news.isEmpty()) {
                    categoryDistribution.put(categoryInfo.getName(), news.size());
                }
            }
        }

        result.put("success", true);
        result.put("count", allFilteredNews.size());
        result.put("category_distribution", categoryDistribution);

        if (!allFailures.isEmpty()) {
            result.put("failures", allFailures);
        }

        result.put("timestamp", System.currentTimeMillis());
        result.put("data", allFilteredNews);

        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
    }

    /**
     * 根据分类获取新闻
     */
    private List<NewsItem> fetchNewsByCategory(
            CategoryConfig.CategoryInfo categoryInfo,
            int limit,
            Map<String, String> failures) {

        // 获取推荐平台（权重 >= 2）
        List<String> recommendedPlatforms = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : categoryInfo.getPlatformWeights().entrySet()) {
            if (entry.getValue() >= 2) {
                recommendedPlatforms.add(entry.getKey());
            }
        }

        if (recommendedPlatforms.isEmpty()) {
            recommendedPlatforms = categoryInfo.getPlatformWeights().keySet().stream()
                    .limit(3)
                    .collect(Collectors.toList());
        }

        // 获取新闻
        CrawlResult crawlResult = newsService.getHotNews(recommendedPlatforms, limit * 3, false);
        failures.putAll(crawlResult.getFailures());

        List<NewsItem> allNews = crawlResult.getData();

        // 按关键词过滤并评分
        List<Map.Entry<NewsItem, Integer>> scoredNews = new ArrayList<>();
        for (NewsItem item : allNews) {
            String title = item.getTitle();
            if (title == null)
                continue;
            String titleLower = title.toLowerCase();

            int score = 0;

            // 关键词匹配
            for (String keyword : categoryInfo.getKeywords()) {
                if (titleLower.contains(keyword.toLowerCase())) {
                    score += 10;
                }
            }

            // 平台权重加分
            int platformWeight = categoryInfo.getWeight(item.getPlatform());
            score += platformWeight * 3;

            if (score > 0) {
                scoredNews.add(new AbstractMap.SimpleEntry<>(item, score));
            }
        }

        // 按分数排序
        scoredNews.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        return scoredNews.stream()
                .map(Map.Entry::getKey)
                .limit(limit)
                .collect(Collectors.toList());
    }
}
