# Transport Model

## Current Modes

- `stdio`
  Starts the classic stdin/stdout MCP server. This is the default mode.
- `http`
  Starts the Streamable HTTP MCP server only.
- `both`
  Starts HTTP and STDIO together, sharing one tool registry and one request handler.

## Environment Variables

```bash
MCP_TRANSPORT=stdio|http|both
MCP_HTTP_HOST=127.0.0.1
MCP_HTTP_PORT=8080
```

## HTTP v1 Scope

- Endpoint: `POST /mcp`
- Content type: `application/json`
- Request body: one JSON-RPC request per POST
- Normal request with response payload: `200 OK`
- Notification with no response payload: `202 Accepted`
- Unsupported `GET` and `DELETE`: `405 Method Not Allowed`
- Missing `Origin`: allowed
- Mismatched browser `Origin`: rejected with `403 Forbidden`

## Implementation Boundaries

- Shared protocol logic lives in `com.paiad.mcp.server.McpRequestHandler`
- STDIO transport adapter lives in `com.paiad.mcp.server.StdioMcpServer`
- Streamable HTTP transport adapter lives in `com.paiad.mcp.server.StreamableHttpMcpServer`
- Bootstrap and env selection live in `com.paiad.mcp.McpServerApplication`

## Runtime Rule

Any transport change must preserve the MCP tool contract unless the tests and references are intentionally updated together.
