package com.paiad.mcp.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.Executors;

public class StreamableHttpMcpServer {

    private static final Logger logger = LoggerFactory.getLogger(StreamableHttpMcpServer.class);
    private static final String CONTENT_TYPE_JSON = "application/json; charset=utf-8";
    private static final Set<String> SUPPORTED_METHODS = Set.of("POST");

    private final McpRequestHandler requestHandler;
    private final ObjectMapper objectMapper;
    private final String host;
    private final int port;
    private HttpServer server;

    public StreamableHttpMcpServer(McpRequestHandler requestHandler, String host, int port) {
        this.requestHandler = requestHandler;
        this.objectMapper = new ObjectMapper();
        this.host = host;
        this.port = port;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(host, port), 0);
        server.createContext("/mcp", new McpHttpHandler());
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
        logger.info("Streamable HTTP MCP server started at {}", endpoint());
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    public URI endpoint() {
        return URI.create("http://" + host + ":" + port + "/mcp");
    }

    private final class McpHttpHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try (exchange) {
                if (!validateOrigin(exchange)) {
                    sendPlain(exchange, 403, "Forbidden origin");
                    return;
                }

                if (!SUPPORTED_METHODS.contains(exchange.getRequestMethod())) {
                    exchange.getResponseHeaders().set("Allow", "POST");
                    sendPlain(exchange, 405, "Method Not Allowed");
                    return;
                }

                Headers headers = exchange.getResponseHeaders();
                headers.set("Content-Type", CONTENT_TYPE_JSON);

                try (InputStream body = exchange.getRequestBody()) {
                    JsonNode request = objectMapper.readTree(body);
                    ObjectNode response = requestHandler.handleRequest(request);
                    if (response == null) {
                        exchange.sendResponseHeaders(202, -1);
                        return;
                    }

                    byte[] bytes = objectMapper.writeValueAsBytes(response);
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream outputStream = exchange.getResponseBody()) {
                        outputStream.write(bytes);
                    }
                } catch (Exception e) {
                    logger.error("Failed to process HTTP MCP request: {}", e.getMessage(), e);
                    ObjectNode errorResponse = requestHandler.createParseErrorResponse(e.getMessage());
                    byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
                    exchange.sendResponseHeaders(400, bytes.length);
                    try (OutputStream outputStream = exchange.getResponseBody()) {
                        outputStream.write(bytes);
                    }
                }
            }
        }

        private boolean validateOrigin(HttpExchange exchange) {
            String origin = exchange.getRequestHeaders().getFirst("Origin");
            if (origin == null || origin.isBlank()) {
                return true;
            }
            URI originUri = URI.create(origin);
            return endpoint().getScheme().equalsIgnoreCase(originUri.getScheme())
                    && endpoint().getHost().equalsIgnoreCase(originUri.getHost())
                    && endpoint().getPort() == originUri.getPort();
        }

        private void sendPlain(HttpExchange exchange, int status, String body) throws IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(bytes);
            }
        }
    }
}
