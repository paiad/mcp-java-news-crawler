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
        return "分析热点新闻，提取热门话题关键词，统计出现频次和跨平台分布情况";
    }

    @Override
    public JsonNode getInputSchema(ObjectMapper objectMapper) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = objectMapper.createObjectNode();

        ObjectNode topNProp = objectMapper.createObjectNode();
        topNProp.put("type", "integer");
        topNProp.put("description", "返回 TOP N 热门话题，默认10");
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
