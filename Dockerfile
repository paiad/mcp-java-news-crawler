# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy pom.xml first and download dependencies (for better caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="paiad"
LABEL description="MCP Java News Crawler - Breaking the Filter Bubble"

WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/target/mcp-java-news-crawler-jar-with-dependencies.jar ./app.jar

# Set encoding for Chinese content support
ENV JAVA_OPTS="-Dfile.encoding=UTF-8"

# The MCP server uses STDIO, so no port needs to be exposed
# ENTRYPOINT allows MCP clients to communicate via stdin/stdout
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
