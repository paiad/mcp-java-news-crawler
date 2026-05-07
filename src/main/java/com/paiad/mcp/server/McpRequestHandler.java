package com.paiad.mcp.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paiad.mcp.tool.McpTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class McpRequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(McpRequestHandler.class);

    private final ObjectMapper objectMapper;
    private final Map<String, McpTool> tools;
    private final String serverName;
    private final String serverVersion;
    private final String protocolVersion;

    public McpRequestHandler(
            ObjectMapper objectMapper,
            Map<String, McpTool> tools,
            String serverName,
            String serverVersion,
            String protocolVersion
    ) {
        this.objectMapper = objectMapper;
        this.tools = tools;
        this.serverName = serverName;
        this.serverVersion = serverVersion;
        this.protocolVersion = protocolVersion;
    }

    public ObjectNode handleRequest(JsonNode request) {
        JsonNode id = request.has("id") ? request.get("id") : null;
        String method = request.has("method") ? request.get("method").asText() : "";
        JsonNode params = request.has("params") ? request.get("params") : objectMapper.createObjectNode();

        logger.debug("Received MCP request: method={}, id={}", method, id);

        return switch (method) {
            case "initialize" -> handleInitialize(id);
            case "initialized" -> null;
            case "tools/list" -> handleToolsList(id);
            case "tools/call" -> handleToolsCall(id, params);
            case "ping" -> createSuccessResponse(id, objectMapper.createObjectNode());
            default -> createErrorResponse(id, -32601, "Method not found: " + method);
        };
    }

    public ObjectNode createParseErrorResponse(String message) {
        return createErrorResponse(null, -32700, "Parse error: " + message);
    }

    private ObjectNode handleInitialize(JsonNode id) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("protocolVersion", protocolVersion);

        ObjectNode serverInfo = objectMapper.createObjectNode();
        serverInfo.put("name", serverName);
        serverInfo.put("version", serverVersion);
        result.set("serverInfo", serverInfo);

        ObjectNode capabilities = objectMapper.createObjectNode();
        ObjectNode toolsCap = objectMapper.createObjectNode();
        toolsCap.put("listChanged", false);
        capabilities.set("tools", toolsCap);
        result.set("capabilities", capabilities);

        result.put(
                "instructions",
                "This is a hot news crawling and analysis MCP server. Supports fetching hot rankings from multiple platforms, keyword search, and trend analysis."
        );

        return createSuccessResponse(id, result);
    }

    private ObjectNode handleToolsList(JsonNode id) {
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

    private ObjectNode handleToolsCall(JsonNode id, JsonNode params) {
        String toolName = params.has("name") ? params.get("name").asText() : "";
        JsonNode arguments = params.has("arguments") ? params.get("arguments") : objectMapper.createObjectNode();

        String content;
        boolean isError = false;
        McpTool tool = tools.get(toolName);

        if (tool == null) {
            content = "{\"error\": \"Unknown tool: " + toolName + "\"}";
            isError = true;
        } else {
            try {
                content = tool.execute(arguments, objectMapper);
            } catch (Exception e) {
                logger.error("Tool execution failed: {}", toolName, e);
                content = "{\"error\": \"" + e.getMessage() + "\"}";
                isError = true;
            }
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

    private ObjectNode createSuccessResponse(JsonNode id, ObjectNode result) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        if (id != null) {
            response.set("id", id.deepCopy());
        }
        response.set("result", result);
        return response;
    }

    private ObjectNode createErrorResponse(JsonNode id, int code, String message) {
        ObjectNode error = objectMapper.createObjectNode();
        error.put("code", code);
        error.put("message", message);

        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        if (id != null) {
            response.set("id", id.deepCopy());
        }
        response.set("error", error);
        return response;
    }
}
