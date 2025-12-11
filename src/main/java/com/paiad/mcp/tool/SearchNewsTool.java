package com.paiad.mcp.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paiad.mcp.model.NewsItem;
import com.paiad.mcp.service.NewsService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 搜索新闻工具
 */
public class SearchNewsTool implements McpTool {

    private final NewsService newsService;

    public SearchNewsTool(NewsService newsService) {
        this.newsService = newsService;
    }

    @Override
    public String getName() {
        return "search_news";
    }

    @Override
    public String getDescription() {
        return "Search hot news by keyword in titles";
    }

    @Override
    public JsonNode getInputSchema(ObjectMapper objectMapper) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = objectMapper.createObjectNode();

        ObjectNode queryProp = objectMapper.createObjectNode();
        queryProp.put("type", "string");
        queryProp.put("description", "Search keyword for fuzzy matching in news titles");
        properties.set("query", queryProp);

        ObjectNode platformsProp = objectMapper.createObjectNode();
        platformsProp.put("type", "array");
        ObjectNode items = objectMapper.createObjectNode();
        items.put("type", "string");
        platformsProp.set("items", items);
        platformsProp.put("description",
                "Limit search to platforms. Options: zhihu, weibo, bilibili, baidu, douyin, toutiao, tiktok, x, reddit, google_news");
        properties.set("platforms", platformsProp);

        ObjectNode limitProp = objectMapper.createObjectNode();
        limitProp.put("type", "integer");
        limitProp.put("description", "Result limit, default 20");
        limitProp.put("default", 20);
        properties.set("limit", limitProp);

        schema.set("properties", properties);
        ArrayNode required = objectMapper.createArrayNode();
        required.add("query");
        schema.set("required", required);

        return schema;
    }

    @Override
    public String execute(JsonNode arguments, ObjectMapper objectMapper) throws Exception {
        String query = arguments.has("query") ? arguments.get("query").asText() : "";
        if (query.isEmpty()) {
            return "{\"success\": false, \"error\": \"Query keyword cannot be empty\"}";
        }

        List<String> platforms = null;
        if (arguments.has("platforms") && arguments.get("platforms").isArray()) {
            platforms = new ArrayList<>();
            for (JsonNode p : arguments.get("platforms")) {
                platforms.add(p.asText());
            }
        }

        int limit = arguments.has("limit") ? arguments.get("limit").asInt(20) : 20;
        limit = Math.min(limit, 100);

        List<NewsItem> news = newsService.searchNews(query, platforms, limit);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("query", query);
        result.put("count", news.size());
        result.put("timestamp", System.currentTimeMillis());
        result.put("data", news);

        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
    }
}
