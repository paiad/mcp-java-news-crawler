package com.paiad.mcp.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

public class StdioMcpServer {

    private static final Logger logger = LoggerFactory.getLogger(StdioMcpServer.class);

    private final ObjectMapper objectMapper;
    private final McpRequestHandler requestHandler;
    private final BufferedReader reader;
    private final PrintWriter writer;

    public StdioMcpServer(ObjectMapper objectMapper, McpRequestHandler requestHandler) {
        this(
                objectMapper,
                requestHandler,
                new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)),
                new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true)
        );
    }

    StdioMcpServer(
            ObjectMapper objectMapper,
            McpRequestHandler requestHandler,
            BufferedReader reader,
            PrintWriter writer
    ) {
        this.objectMapper = objectMapper;
        this.requestHandler = requestHandler;
        this.reader = reader;
        this.writer = writer;
    }

    public void start() {
        logger.info("STDIO MCP server started");
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    JsonNode request = objectMapper.readTree(line);
                    sendResponse(requestHandler.handleRequest(request));
                } catch (Exception e) {
                    logger.error("Failed to process STDIO request: {}", e.getMessage(), e);
                    sendResponse(requestHandler.createParseErrorResponse(e.getMessage()));
                }
            }
        } catch (IOException e) {
            logger.error("Failed to read STDIO input: {}", e.getMessage(), e);
        }
    }

    public void stop() {
        writer.flush();
    }

    private void sendResponse(ObjectNode response) {
        if (response == null) {
            return;
        }
        try {
            writer.println(objectMapper.writeValueAsString(response));
            writer.flush();
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize response: {}", e.getMessage(), e);
        }
    }
}
