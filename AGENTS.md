# AGENTS.md

This file provides guidance to AI coding agents when working with code in this repository.

## Project Overview

MCP Java News Crawler is a Java-based hot news crawler service built on the **Model Context Protocol (MCP)**. It allows AI assistants (Claude, Codex, Gemini, etc.) to fetch real-time trending news from multiple platforms via natural language commands.

## Tech Stack

- **Java 21** with Virtual Threads
- **Maven** for build management
- **OkHttp** for HTTP requests
- **Jsoup** for HTML/XML parsing
- **Lombok** for boilerplate reduction
- **SLF4J + Logback** for logging (stderr only)
- **Jackson** for JSON processing
- **SnakeYAML** for configuration

## Project Structure

```
src/main/java/com/paiad/mcp/
├── config/       # Platform configuration
├── crawler/      # Crawler implementations
│   ├── domestic/     # Chinese platforms (Weibo, Zhihu, Bilibili, etc.)
│   └── international/ # International (Reddit, BBC, HackerNews, etc.)
├── model/        # Data models
│   ├── pojo/         # Domain entities (NewsItem, CrawlResult)
│   └── vo/           # View objects (NewsItemVO)
├── service/      # Business logic (NewsService)
├── tool/         # MCP tool definitions
└── util/         # Utilities (HttpClientFactory)

src/main/resources/
├── platforms.yml         # Platform priority & enable settings
└── simplelogger.properties # Logging config (stderr only!)
```

## Key Commands

```bash
# Build with dependencies
mvn clean package -DskipTests

# Run the MCP server (STDIO mode)
java -jar target/mcp-java-news-crawler-jar-with-dependencies.jar

# Run tests
mvn test

# Build Docker image
docker build -t mcp-java-news-crawler .
```

## Architecture Notes

### MCP Communication

- Uses **STDIO** for communication (stdin/stdout)
- All logs go to **stderr** (critical: never log to stdout)
- JSON-RPC 2.0 protocol

### Crawler Design

- `AbstractCrawler`: Base class with HTTP client, retry logic
- Domestic crawlers: Direct connection
- International crawlers: Proxy via `HTTP_PROXY` env var or `.env` file
- Most international platforms use RSS feeds for stability

### Adding a New Crawler

1. Create class in `crawler/domestic/` or `crawler/international/`
2. Extend `AbstractCrawler`
3. Override `isInternational()` for proxy usage
4. Implement `crawl()` method returning `List<NewsItem>`
5. Register in `PlatformConfig.java`
6. Add config in `platforms.yml`

### Configuration Files

- **platforms.yml**: Enable/disable platforms, set priority (1-100)

## Important Conventions

1. **Never print to stdout** - MCP uses it for communication
2. **Virtual Threads** - Use `Executors.newVirtualThreadPerTaskExecutor()`
3. **Shared HTTP Client** - Use `HttpClientFactory` singleton
4. **RSS over HTML** - Prefer RSS feeds for international platforms
5. **UTF-8 encoding** - Always use `-Dfile.encoding=UTF-8` when running
