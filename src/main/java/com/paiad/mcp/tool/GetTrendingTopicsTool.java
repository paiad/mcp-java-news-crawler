package com.paiad.mcp.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paiad.mcp.model.TrendingTopic;
import com.paiad.mcp.service.TrendService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 获取趋势话题工具
 */
public class GetTrendingTopicsTool implements McpTool {

    private final TrendService trendService;

    public GetTrendingTopicsTool(TrendService trendService) {
        this.trendService = trendService;
    }

    @Override
    public String getName() {
        return "get_trending_topics";
    }

    @Override
    public String getDescription() {
        return "Analyze hot news and extract trending topic keywords with frequency and cross-platform distribution";
    }

    @Override
    public JsonNode getInputSchema(ObjectMapper objectMapper) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = objectMapper.createObjectNode();

        ObjectNode topNProp = objectMapper.createObjectNode();
        topNProp.put("type", "integer");
        topNProp.put("description", "Return TOP N trending topics, default 10");
        topNProp.put("default", 10);
        properties.set("top_n", topNProp);

        schema.set("properties", properties);
        schema.set("required", objectMapper.createArrayNode());

        return schema;
    }

    @Override
    public String execute(JsonNode arguments, ObjectMapper objectMapper) throws Exception {
        int topN = arguments.has("top_n") ? arguments.get("top_n").asInt(10) : 10;
        topN = Math.min(topN, 50);

        List<TrendingTopic> topics = trendService.getTrendingTopics(topN);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("count", topics.size());
        result.put("timestamp", System.currentTimeMillis());
        result.put("data", topics);

        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
    }
}
