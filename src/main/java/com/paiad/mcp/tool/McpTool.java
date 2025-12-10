package com.paiad.mcp.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * MCP 工具接口
 */
public interface McpTool {

    /**
     * 获取工具名称
     */
    String getName();

    /**
     * 获取工具描述
     */
    String getDescription();

    /**
     * 获取输入参数 Schema
     */
    JsonNode getInputSchema(ObjectMapper objectMapper);

    /**
     * 执行工具
     *
     * @param arguments    参数
     * @param objectMapper 用于JSON序列化的 mapper
     * @return 执行结果 JSON 字符串
     * @throws Exception 执行异常
     */
    String execute(JsonNode arguments, ObjectMapper objectMapper) throws Exception;
}
