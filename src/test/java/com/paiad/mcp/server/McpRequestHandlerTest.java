package com.paiad.mcp.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paiad.mcp.tool.McpTool;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class McpRequestHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void initializeShouldReturnServerInfoAndToolsCapability() {
        McpRequestHandler handler = new McpRequestHandler(
                objectMapper,
                Map.of(),
                "mcp-java-news-crawler",
                "3.1.0",
                "2024-11-05"
        );

        ObjectNode request = objectMapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", 1);
        request.put("method", "initialize");
        request.set("params", objectMapper.createObjectNode());

        ObjectNode response = handler.handleRequest(request);

        assertNotNull(response);
        assertEquals("mcp-java-news-crawler", response.get("result").get("serverInfo").get("name").asText());
        assertTrue(response.get("result").get("capabilities").has("tools"));
    }

    @Test
    void toolsListShouldExposeRegisteredTools() {
        McpTool fakeTool = new FakeTool("demo_tool", "demo tool");
        McpRequestHandler handler = new McpRequestHandler(
                objectMapper,
                Map.of(fakeTool.getName(), fakeTool),
                "mcp-java-news-crawler",
                "3.1.0",
                "2024-11-05"
        );

        ObjectNode request = objectMapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", 2);
        request.put("method", "tools/list");
        request.set("params", objectMapper.createObjectNode());

        ObjectNode response = handler.handleRequest(request);

        JsonNode tools = response.get("result").get("tools");
        assertEquals(1, tools.size());
        assertEquals("demo_tool", tools.get(0).get("name").asText());
    }

    @Test
    void initializedNotificationShouldNotProduceResponse() {
        McpRequestHandler handler = new McpRequestHandler(
                objectMapper,
                Map.of(),
                "mcp-java-news-crawler",
                "3.1.0",
                "2024-11-05"
        );

        ObjectNode request = objectMapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("method", "initialized");

        assertNull(handler.handleRequest(request));
    }

    @Test
    void unknownMethodShouldReturnJsonRpcError() {
        McpRequestHandler handler = new McpRequestHandler(
                objectMapper,
                Map.of(),
                "mcp-java-news-crawler",
                "3.1.0",
                "2024-11-05"
        );

        ObjectNode request = objectMapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", 9);
        request.put("method", "unknown");

        ObjectNode response = handler.handleRequest(request);

        assertEquals(-32601, response.get("error").get("code").asInt());
    }

    @Test
    void toolsCallShouldWrapThrownToolErrorsAsIsError() {
        McpTool failingTool = new FakeTool("boom", "throws") {
            @Override
            public String execute(JsonNode arguments, ObjectMapper objectMapper) {
                throw new IllegalArgumentException("boom");
            }
        };
        McpRequestHandler handler = new McpRequestHandler(
                objectMapper,
                Map.of(failingTool.getName(), failingTool),
                "mcp-java-news-crawler",
                "3.1.0",
                "2024-11-05"
        );

        ObjectNode request = objectMapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", 3);
        request.put("method", "tools/call");
        ObjectNode params = objectMapper.createObjectNode();
        params.put("name", "boom");
        params.set("arguments", objectMapper.createObjectNode());
        request.set("params", params);

        ObjectNode response = handler.handleRequest(request);

        assertTrue(response.get("result").get("isError").asBoolean());
        assertTrue(response.get("result").get("content").get(0).get("text").asText().contains("boom"));
    }

    private static class FakeTool implements McpTool {
        private final String name;
        private final String description;

        private FakeTool(String name, String description) {
            this.name = name;
            this.description = description;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public JsonNode getInputSchema(ObjectMapper objectMapper) {
            ObjectNode schema = objectMapper.createObjectNode();
            schema.put("type", "object");
            return schema;
        }

        @Override
        public String execute(JsonNode arguments, ObjectMapper objectMapper) {
            return "{\"ok\":true}";
        }
    }
}
