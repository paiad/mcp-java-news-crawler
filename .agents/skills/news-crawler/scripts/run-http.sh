#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
JAVA_HOME_DEFAULT="/usr/local/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"

export JAVA_HOME="${JAVA_HOME:-$JAVA_HOME_DEFAULT}"
export PATH="$JAVA_HOME/bin:$PATH"
export MCP_TRANSPORT="${MCP_TRANSPORT:-http}"
export MCP_HTTP_HOST="${MCP_HTTP_HOST:-127.0.0.1}"
export MCP_HTTP_PORT="${MCP_HTTP_PORT:-8080}"

cd "$ROOT_DIR"
mvn -q -DskipTests package
exec java -jar target/mcp-java-news-crawler-jar-with-dependencies.jar
