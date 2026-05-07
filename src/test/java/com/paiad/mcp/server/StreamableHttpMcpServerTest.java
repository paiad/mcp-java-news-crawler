package com.paiad.mcp.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paiad.mcp.tool.McpTool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StreamableHttpMcpServerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private StreamableHttpMcpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void postShouldReturnJsonRpcResponse() throws Exception {
        server = startServer(Map.of("demo_tool", new DemoTool()));
        HttpClient client = HttpClient.newHttpClient();

        ObjectNode requestJson = objectMapper.createObjectNode();
        requestJson.put("jsonrpc", "2.0");
        requestJson.put("id", 1);
        requestJson.put("method", "tools/list");
        requestJson.set("params", objectMapper.createObjectNode());

        HttpResponse<String> response = client.send(
                HttpRequest.newBuilder(server.endpoint())
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestJson.toString(), StandardCharsets.UTF_8))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"tools\""));
    }

    @Test
    void notificationPostShouldReturnAccepted() throws Exception {
        server = startServer(Map.of("demo_tool", new DemoTool()));
        HttpClient client = HttpClient.newHttpClient();

        ObjectNode requestJson = objectMapper.createObjectNode();
        requestJson.put("jsonrpc", "2.0");
        requestJson.put("method", "initialized");

        HttpResponse<String> response = client.send(
                HttpRequest.newBuilder(server.endpoint())
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestJson.toString(), StandardCharsets.UTF_8))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(202, response.statusCode());
    }

    @Test
    void getShouldBeRejectedInFirstVersion() throws Exception {
        server = startServer(Map.of("demo_tool", new DemoTool()));
        HttpClient client = HttpClient.newHttpClient();

        HttpResponse<String> response = client.send(
                HttpRequest.newBuilder(server.endpoint()).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(405, response.statusCode());
    }

    private StreamableHttpMcpServer startServer(Map<String, McpTool> tools) throws IOException {
        int port = findFreePort();
        McpRequestHandler handler = new McpRequestHandler(
                objectMapper,
                tools,
                "mcp-java-news-crawler",
                "3.1.0",
                "2024-11-05"
        );
        StreamableHttpMcpServer httpServer = new StreamableHttpMcpServer(
                handler,
                "127.0.0.1",
                port
        );
        httpServer.start();
        return httpServer;
    }

    private int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static class DemoTool implements McpTool {
        @Override
        public String getName() {
            return "demo_tool";
        }

        @Override
        public String getDescription() {
            return "demo";
        }

        @Override
        public com.fasterxml.jackson.databind.JsonNode getInputSchema(ObjectMapper objectMapper) {
            ObjectNode schema = objectMapper.createObjectNode();
            schema.put("type", "object");
            return schema;
        }

        @Override
        public String execute(com.fasterxml.jackson.databind.JsonNode arguments, ObjectMapper objectMapper) {
            return "{\"ok\":true}";
        }
    }
}
