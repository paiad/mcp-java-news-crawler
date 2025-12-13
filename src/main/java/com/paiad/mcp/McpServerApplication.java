package com.paiad.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paiad.mcp.service.NewsService;
import com.paiad.mcp.service.TrendService;
import com.paiad.mcp.tool.GetHotNewsTool;
import com.paiad.mcp.tool.GetTrendingTopicsTool;
import com.paiad.mcp.tool.McpTool;
import com.paiad.mcp.tool.SearchNewsTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * MCP Server 启动类
 *
 * 基于 STDIO 传输的 MCP 服务器实现，支持 Cherry Studio、Claude Desktop 等客户端连接
 *
 * @author Paiad
 */
public class McpServerApplication {

    private static final Logger logger = LoggerFactory.getLogger(McpServerApplication.class);

    private static final String SERVER_NAME = "mcp-java-news-crawler";
    private static final String SERVER_VERSION = "2.1.0";
    // MCP 协议版本（"2024-11-05" - 当前最新的稳定版本）
    private static final String PROTOCOL_VERSION = "2024-11-05";

    private final ObjectMapper objectMapper;
    private final NewsService newsService;
    private final TrendService trendService;
    private final Map<String, McpTool> tools;

    private final BufferedReader reader;
    private final PrintWriter writer;

    public McpServerApplication() {
        this.objectMapper = new ObjectMapper();
        this.newsService = new NewsService();
        this.trendService = new TrendService(newsService);
        this.tools = new HashMap<>();

        // 注册工具
        registerTool(new GetHotNewsTool(newsService));
        registerTool(new SearchNewsTool(newsService));
        registerTool(new GetTrendingTopicsTool(trendService));

        this.reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        this.writer = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);

        logger.info("MCP Server 初始化完成: {}", SERVER_NAME);
    }

    private void registerTool(McpTool tool) {
        tools.put(tool.getName(), tool);
    }

    /**
     * 启动服务器
     */
    public void start() {
        logger.info("MCP Server 启动，等待客户端连接...");

        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    JsonNode request = objectMapper.readTree(line);
                    ObjectNode response = handleRequest(request);
                    sendResponse(response);
                } catch (Exception e) {
                    logger.error("处理请求失败: {}", e.getMessage(), e);
                    sendErrorResponse(null, -32700, "Parse error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            logger.error("读取输入失败: {}", e.getMessage(), e);
        } finally {
            shutdown();
        }
    }

    /**
     * 处理请求
     */
    private ObjectNode handleRequest(JsonNode request) {
        Object id = request.has("id") ? request.get("id") : null;
        String method = request.has("method") ? request.get("method").asText() : "";
        JsonNode params = request.has("params") ? request.get("params") : objectMapper.createObjectNode();

        logger.debug("收到请求: method={}, id={}", method, id);

        switch (method) {
            case "initialize":
                return handleInitialize(id, params);
            case "initialized":
                return null; // notification, no response
            case "tools/list":
                return handleToolsList(id);
            case "tools/call":
                return handleToolsCall(id, params);
            case "ping":
                return handlePing(id);
            default:
                return createErrorResponse(id, -32601, "Method not found: " + method);
        }
    }

    /**
     * 处理 initialize 请求
     */
    private ObjectNode handleInitialize(Object id, JsonNode params) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("protocolVersion", PROTOCOL_VERSION);

        ObjectNode serverInfo = objectMapper.createObjectNode();
        serverInfo.put("name", SERVER_NAME);
        serverInfo.put("version", SERVER_VERSION);
        result.set("serverInfo", serverInfo);

        ObjectNode capabilities = objectMapper.createObjectNode();
        ObjectNode toolsCap = objectMapper.createObjectNode();
        toolsCap.put("listChanged", false);
        capabilities.set("tools", toolsCap);
        result.set("capabilities", capabilities);

        result.put("instructions",
                "This is a hot news crawling and analysis MCP server. Supports fetching hot rankings from multiple platforms, keyword search, and trend analysis.");

        logger.info("客户端初始化完成");
        return createSuccessResponse(id, result);
    }

    /**
     * 处理 tools/list 请求
     */
    private ObjectNode handleToolsList(Object id) {
        ArrayNode toolsArray = objectMapper.createArrayNode();

        for (McpTool tool : tools.values()) {
            ObjectNode toolNode = objectMapper.createObjectNode();
            toolNode.put("name", tool.getName());
            toolNode.put("description", tool.getDescription());
            toolNode.set("inputSchema", tool.getInputSchema(objectMapper));
            toolsArray.add(toolNode);
        }

        ObjectNode result = objectMapper.createObjectNode();
        result.set("tools", toolsArray);

        return createSuccessResponse(id, result);
    }

    /**
     * 处理 tools/call 请求
     */
    private ObjectNode handleToolsCall(Object id, JsonNode params) {
        String toolName = params.has("name") ? params.get("name").asText() : "";
        JsonNode arguments = params.has("arguments") ? params.get("arguments") : objectMapper.createObjectNode();

        logger.info("调用工具: {}", toolName);

        String content;
        boolean isError = false;

        McpTool tool = tools.get(toolName);
        if (tool != null) {
            try {
                content = tool.execute(arguments, objectMapper);
            } catch (Exception e) {
                logger.error("工具执行失败: {}", e.getMessage(), e);
                content = "{\"error\": \"" + e.getMessage() + "\"}";
                isError = true;
            }
        } else {
            content = "{\"error\": \"Unknown tool: " + toolName + "\"}";
            isError = true;
        }

        ObjectNode result = objectMapper.createObjectNode();
        ArrayNode contentArray = objectMapper.createArrayNode();
        ObjectNode textContent = objectMapper.createObjectNode();
        textContent.put("type", "text");
        textContent.put("text", content);
        contentArray.add(textContent);
        result.set("content", contentArray);
        result.put("isError", isError);

        return createSuccessResponse(id, result);
    }

    /**
     * 处理 ping 请求
     */
    private ObjectNode handlePing(Object id) {
        return createSuccessResponse(id, objectMapper.createObjectNode());
    }

    /**
     * 创建成功响应
     */
    private ObjectNode createSuccessResponse(Object id, ObjectNode result) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        if (id instanceof Number) {
            response.put("id", ((Number) id).intValue());
        } else if (id instanceof String) {
            response.put("id", (String) id);
        } else if (id instanceof JsonNode) {
            response.set("id", (JsonNode) id);
        }
        response.set("result", result);
        return response;
    }

    /**
     * 创建错误响应
     */
    private ObjectNode createErrorResponse(Object id, int code, String message) {
        ObjectNode error = objectMapper.createObjectNode();
        error.put("code", code);
        error.put("message", message);

        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        if (id instanceof Number) {
            response.put("id", ((Number) id).intValue());
        } else if (id instanceof String) {
            response.put("id", (String) id);
        } else if (id instanceof JsonNode) {
            response.set("id", (JsonNode) id);
        }
        response.set("error", error);
        return response;
    }

    /**
     * 发送响应
     */
    private void sendResponse(ObjectNode response) {
        if (response == null) {
            return; // notification, no response needed
        }
        try {
            String json = objectMapper.writeValueAsString(response);
            writer.println(json);
            writer.flush();
            logger.debug("发送响应: {}", json.length() > 200 ? json.substring(0, 200) + "..." : json);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize response: {}", e.getMessage());
        }
    }

    /**
     * 发送错误响应
     */
    private void sendErrorResponse(Object id, int code, String message) {
        sendResponse(createErrorResponse(id, code, message));
    }

    /**
     * 关闭服务器
     */
    private void shutdown() {
        logger.info("MCP Server 关闭");
        newsService.shutdown();
    }

    /**
     * 主入口
     */
    public static void main(String[] args) {
        // 配置日志输出到 stderr，不干扰 STDIO 通信
        System.setProperty("org.slf4j.simpleLogger.logFile", "System.err");

        McpServerApplication server = new McpServerApplication();
        server.start();
    }
}
