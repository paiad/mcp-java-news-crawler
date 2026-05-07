#!/usr/bin/env bash
set -euo pipefail

HOST="${MCP_HTTP_HOST:-127.0.0.1}"
PORT="${MCP_HTTP_PORT:-8080}"
URL="http://${HOST}:${PORT}/mcp"

curl -sS -X POST "$URL" \
  -H 'Content-Type: application/json' \
  -d '{ "jsonrpc": "2.0", "id": 1, "method": "initialize", "params": { "protocolVersion": "2024-11-05", "capabilities": {} } }'

printf '\n'

curl -sS -X POST "$URL" \
  -H 'Content-Type: application/json' \
  -d '{ "jsonrpc": "2.0", "id": 2, "method": "tools/list", "params": {} }'

printf '\n'
