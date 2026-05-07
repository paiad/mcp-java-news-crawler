package com.paiad.mcp.server;

import java.util.Locale;

public enum TransportMode {
    STDIO,
    HTTP,
    BOTH;

    public static TransportMode fromEnv(String value) {
        if (value == null || value.isBlank()) {
            return STDIO;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "stdio" -> STDIO;
            case "http" -> HTTP;
            case "both" -> BOTH;
            default -> throw new IllegalArgumentException("Unsupported MCP transport: " + value);
        };
    }
}
