#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
JAVA_HOME_DEFAULT="/usr/local/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"

export JAVA_HOME="${JAVA_HOME:-$JAVA_HOME_DEFAULT}"
export PATH="$JAVA_HOME/bin:$PATH"

cd "$ROOT_DIR"
mvn -q -DskipTests package
exec java -jar target/mcp-java-news-crawler-jar-with-dependencies.jar
