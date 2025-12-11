package com.paiad.mcp.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paiad.mcp.model.NewsItem;
import com.paiad.mcp.service.NewsService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 获取热点新闻工具
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
        return "Get hot news from multiple platforms. Supported: zhihu, weibo, bilibili, baidu, douyin, toutiao, tiktok, x (Twitter), reddit, google_news";
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
        platformsProp.put("description",
                "Platform IDs. Options: zhihu, weibo, bilibili, baidu, douyin, toutiao, tiktok, x, reddit, google_news. Empty for all.");
        properties.set("platforms", platformsProp);

        ObjectNode limitProp = objectMapper.createObjectNode();
        limitProp.put("type", "integer");
        limitProp.put("description", "Result limit, default 50, max 200");
        limitProp.put("default", 50);
        properties.set("limit", limitProp);

        ObjectNode refreshProp = objectMapper.createObjectNode();
        refreshProp.put("type", "boolean");
        refreshProp.put("description", "Force refresh cache, default false");
        refreshProp.put("default", false);
        properties.set("refresh", refreshProp);

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

        boolean refresh = arguments.has("refresh") && arguments.get("refresh").asBoolean(false);

        List<NewsItem> news = newsService.getHotNews(platforms, limit, refresh);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("count", news.size());
        result.put("timestamp", System.currentTimeMillis());
        result.put("data", news);

        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
    }
}
