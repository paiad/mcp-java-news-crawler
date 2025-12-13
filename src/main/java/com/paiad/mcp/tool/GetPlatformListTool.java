package com.paiad.mcp.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paiad.mcp.config.CategoryConfig;
import com.paiad.mcp.config.PlatformPriorityConfig;

import java.util.*;

/**
 * 获取平台列表工具
 * 返回所有可用平台的信息
 */
public class GetPlatformListTool implements McpTool {

    @Override
    public String getName() {
        return "get_platform_list";
    }

    @Override
    public String getDescription() {
        return "Get the list of all available news platforms with their IDs, names, status and priority. Use this to discover what platforms are available.";
    }

    @Override
    public JsonNode getInputSchema(ObjectMapper objectMapper) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = objectMapper.createObjectNode();

        ObjectNode includeDisabledProp = objectMapper.createObjectNode();
        includeDisabledProp.put("type", "boolean");
        includeDisabledProp.put("description", "Whether to include disabled platforms, default false");
        includeDisabledProp.put("default", false);
        properties.set("include_disabled", includeDisabledProp);

        schema.set("properties", properties);
        schema.set("required", objectMapper.createArrayNode());

        return schema;
    }

    @Override
    public String execute(JsonNode arguments, ObjectMapper objectMapper) throws Exception {
        boolean includeDisabled = arguments.has("include_disabled")
                && arguments.get("include_disabled").asBoolean(false);

        PlatformPriorityConfig priorityConfig = PlatformPriorityConfig.getInstance();
        CategoryConfig categoryConfig = CategoryConfig.getInstance();

        List<Map<String, Object>> platforms = new ArrayList<>();

        // 遍历所有平台（按优先级排序）
        for (String platformId : priorityConfig.getEnabledPlatformIdsSorted()) {
            PlatformPriorityConfig.PriorityInfo info = priorityConfig.getPriorityInfo(platformId);
            if (info == null)
                continue;

            if (!includeDisabled && !info.isEnabled()) {
                continue;
            }

            Map<String, Object> platform = new LinkedHashMap<>();
            platform.put("id", platformId);
            platform.put("name", info.getDescription());
            platform.put("enabled", info.isEnabled());
            platform.put("priority", info.getPriority());

            // 查找该平台擅长的分类
            List<String> categories = new ArrayList<>();
            for (CategoryConfig.CategoryInfo cat : categoryConfig.getAllCategories()) {
                if (cat.getWeight(platformId) >= 3) {
                    categories.add(cat.getName());
                }
            }
            if (!categories.isEmpty()) {
                platform.put("categories", categories);
            }

            platforms.add(platform);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("count", platforms.size());
        result.put("timestamp", System.currentTimeMillis());
        result.put("platforms", platforms);

        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
    }
}
