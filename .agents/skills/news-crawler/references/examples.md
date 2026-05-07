# Examples

## Run STDIO Locally

```bash
./.agents/skills/news-crawler/scripts/run-stdio.sh
```

## Run Streamable HTTP Locally

```bash
./.agents/skills/news-crawler/scripts/run-http.sh
```

## Smoke Test HTTP

```bash
./.agents/skills/news-crawler/scripts/smoke-test-http.sh
```

## Manual STDIO initialize

```json
{ "jsonrpc": "2.0", "id": 1, "method": "initialize", "params": { "protocolVersion": "2024-11-05", "capabilities": {} } }
```

## Manual HTTP tools/list

```bash
curl -X POST http://127.0.0.1:8080/mcp \
  -H 'Content-Type: application/json' \
  -d '{ "jsonrpc": "2.0", "id": 2, "method": "tools/list", "params": {} }'
```

## Manual HTTP get_hot_news

```bash
curl -X POST http://127.0.0.1:8080/mcp \
  -H 'Content-Type: application/json' \
  -d '{ "jsonrpc": "2.0", "id": 3, "method": "tools/call", "params": { "name": "get_hot_news", "arguments": { "limit": 5 } } }'
```
