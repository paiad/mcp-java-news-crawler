package com.paiad.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paiad.mcp.server.McpRequestHandler;
import com.paiad.mcp.server.StdioMcpServer;
import com.paiad.mcp.server.StreamableHttpMcpServer;
import com.paiad.mcp.server.TransportMode;
import com.paiad.mcp.service.NewsService;
import com.paiad.mcp.tool.GetHotNewsTool;
import com.paiad.mcp.tool.McpTool;
import com.paiad.mcp.tool.SearchNewsTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
    private static final String SERVER_VERSION = "3.1.0";
    // MCP 协议版本（"2024-11-05" - 当前最新的稳定版本）
    private static final String PROTOCOL_VERSION = "2024-11-05";

    private static final String DEFAULT_HTTP_HOST = "127.0.0.1";
    private static final int DEFAULT_HTTP_PORT = 8080;

    private final ObjectMapper objectMapper;
    private final NewsService newsService;
    private final Map<String, McpTool> tools;
    private final McpRequestHandler requestHandler;

    public McpServerApplication() {
        this.objectMapper = new ObjectMapper();
        this.newsService = new NewsService();
        this.tools = new HashMap<>();

        registerTool(new GetHotNewsTool(newsService));
        registerTool(new SearchNewsTool(newsService));
        this.requestHandler = new McpRequestHandler(
                objectMapper,
                tools,
                SERVER_NAME,
                SERVER_VERSION,
                PROTOCOL_VERSION
        );

        logger.info("MCP Server 初始化完成: {}", SERVER_NAME);
    }

    private void registerTool(McpTool tool) {
        tools.put(tool.getName(), tool);
    }

    public void start() {
        TransportMode mode = TransportMode.fromEnv(System.getenv("MCP_TRANSPORT"));
        String httpHost = readHttpHost();
        int httpPort = readHttpPort();

        StreamableHttpMcpServer httpServer = null;
        try {
            if (mode == TransportMode.HTTP || mode == TransportMode.BOTH) {
                httpServer = new StreamableHttpMcpServer(requestHandler, httpHost, httpPort);
                httpServer.start();
            }

            if (mode == TransportMode.STDIO || mode == TransportMode.BOTH) {
                new StdioMcpServer(objectMapper, requestHandler).start();
            } else {
                logger.info("HTTP-only transport active at http://{}:{}/mcp", httpHost, httpPort);
                Thread.currentThread().join();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to start HTTP transport", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (httpServer != null) {
                httpServer.stop();
            }
            shutdown();
        }
    }

    private void shutdown() {
        logger.info("MCP Server 关闭");
        newsService.shutdown();
    }

    private String readHttpHost() {
        String value = System.getenv("MCP_HTTP_HOST");
        return (value == null || value.isBlank()) ? DEFAULT_HTTP_HOST : value.trim();
    }

    private int readHttpPort() {
        String value = System.getenv("MCP_HTTP_PORT");
        if (value == null || value.isBlank()) {
            return DEFAULT_HTTP_PORT;
        }
        return Integer.parseInt(value.trim());
    }

    public static void main(String[] args) {
        System.setProperty("org.slf4j.simpleLogger.logFile", "System.err");
        McpServerApplication server = new McpServerApplication();
        server.start();
    }
}
