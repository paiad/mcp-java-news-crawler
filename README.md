# MCP Java News Crawler

![Java](https://img.shields.io/badge/JDK-21+-green?logo=openjdk&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-blue)
![GitHub Stars](https://img.shields.io/github/stars/paiad/mcp-java-news-crawler?style=social)

一个基于 **Model Context Protocol (MCP)** 的 Java 热点新闻爬虫服务。AI 助手（Claude、Codex、Gemini、Cherry Studio 等）可通过自然语言指令实时获取全网热点新闻、搜索特定事件并分析舆论趋势。

<details>
<summary>👉 点击展开：LLM 调用 mcp-java-news-crawler 图示</summary>
<br>

![p1](https://img.paiad.top/img/mcp-news-crawler-p1.png)
![p2](https://img.paiad.top/img/mcp-news-crawler-p2.png)
![p3](https://img.paiad.top/img/mcp-news-crawler-p3.png)
![p4](https://img.paiad.top/img/mcp-news-crawler-p4.png)
![p5](https://img.paiad.top/img/mcp-news-crawler-p5.png)
![p6](https://img.paiad.top/img/mcp-news-crawler-p6.png)
![p7](https://img.paiad.top/img/mcp-news-crawler-p7.png)

</details>

## 功能概览

### MCP 工具

| 工具名称       | 功能描述              |
| -------------- | --------------------- |
| `get_hot_news` | 获取多平台热榜新闻 |
| `search_news`  | 关键词搜索新闻     |

### 支持的平台

| 平台 ID        | 平台名称     | 类型    | 说明         |
| -------------- | ------------ | ------- | ------------ |
| `douyin`       | 抖音         | 国内 | 抖音热点     |
| `toutiao`      | 今日头条     | 国内 | 头条热榜     |
| `weibo`        | 微博         | 国内 | 微博热搜     |
| `bilibili`     | B 站         | 国内 | B 站热门视频 |
| `baidu`        | 百度         | 国内 | 百度热搜     |
| `zhihu`        | 知乎         | 国内 | 知乎热榜     |
| `wallstreetcn` | 华尔街见闻   | 国内 | 财经资讯     |
| `google_news`  | Google News  | 国际 | 谷歌新闻     |
| `reddit`       | Reddit       | 国际 | Reddit 热帖  |
| `bbc`          | BBC          | 国际 | BBC 新闻     |
| `reuters`      | Reuters      | 国际 | 路透社       |
| `apnews`       | AP News      | 国际 | 美联社       |
| `guardian`     | The Guardian | 国际 | 卫报         |
| `techcrunch`   | TechCrunch   | 国际 | 科技资讯     |
| `hacker_news`  | Hacker News  | 国际 | 黑客新闻     |

> **关于国际平台**：国际媒体通过 RSS 获取，数据仅含标题、摘要和发布时间，不含热度或互动数据（`hotScore` 为 0，`hotDesc` 为发布时间 GMT）。

## 快速开始

### 1. 构建

要求 JDK 21+（使用虚拟线程特性）。

```bash
mvn clean package -DskipTests
```

构建产物位于 `target/mcp-java-news-crawler-jar-with-dependencies.jar`。

### 2. 运行

```bash
java -jar target/mcp-java-news-crawler-jar-with-dependencies.jar
```

默认以 **STDIO** 传输启动，适合本地 MCP 客户端。

### 3. 验证

启动后服务等待 stdin 输入，依次发送以下 JSON 测试：

```json
{ "jsonrpc": "2.0", "id": 1, "method": "initialize", "params": { "protocolVersion": "2024-11-05", "capabilities": {} } }
```

```json
{ "jsonrpc": "2.0", "id": 2, "method": "tools/list", "params": {} }
```

```json
{ "jsonrpc": "2.0", "id": 3, "method": "tools/call", "params": { "name": "get_hot_news", "arguments": { "limit": 5 } } }
```

## 传输模式

通过环境变量切换：

```bash
MCP_TRANSPORT=stdio|http|both
MCP_HTTP_HOST=127.0.0.1
MCP_HTTP_PORT=8080
```

| 模式 | 说明 |
|------|------|
| `stdio` (默认) | 通过 stdin/stdout 提供 MCP 服务 |
| `http` | Streamable HTTP，监听 `http://127.0.0.1:8080/mcp` |
| `both` | 同时启动 STDIO 和 HTTP |

### HTTP 模式测试

```bash
# 启动
MCP_TRANSPORT=http java -jar target/mcp-java-news-crawler-jar-with-dependencies.jar

# 调用
curl -X POST http://127.0.0.1:8080/mcp \
  -H 'Content-Type: application/json' \
  -d '{ "jsonrpc": "2.0", "id": 1, "method": "tools/list", "params": {} }'
```

> 仅支持 `POST /mcp`，JSON-RPC notification 返回 `202 Accepted`，`GET` / `DELETE` 返回 `405`。

## 客户端配置

所有客户端使用相同的配置模式，只需替换 `jar` 的实际路径：

```json
{
  "mcpServers": {
    "news-crawler": {
      "command": "java",
      "args": ["-Dfile.encoding=UTF-8", "-jar", "/path/to/mcp-java-news-crawler-jar-with-dependencies.jar"]
    }
  }
}
```

<details>
<summary>Cherry Studio</summary>

设置 → MCP Server → 添加 JSON 配置，粘贴上述模板即可。

</details>

<details>
<summary>Claude Code (CLI)</summary>

```bash
/mcp add news-crawler java -Dfile.encoding=UTF-8 -jar /path/to/mcp-java-news-crawler-jar-with-dependencies.jar
```

</details>

<details>
<summary>Codex / Gemini CLI</summary>

编辑各自配置文件，粘贴 JSON 模板：
- Codex: `~/.codex/config.json`
- Gemini: `~/.gemini/settings.json`

</details>

## Docker 部署

```bash
# 构建镜像
docker build -t mcp-java-news-crawler .

# 交互测试
docker run -it --rm mcp-java-news-crawler
```

客户端配置（使用 Docker 方式）：

```json
{
  "mcpServers": {
    "news-crawler": {
      "command": "docker",
      "args": ["run", "-i", "--rm", "mcp-java-news-crawler"]
    }
  }
}
```

> 镜像基于 `eclipse-temurin:21-jre-alpine` 多阶段构建，体积小巧。MCP 客户端使用 `-i`（不需要 `-t`）。

## 配置参考

### 代理（访问国际平台）

访问国际新闻平台需配置 HTTP 代理：

```bash
# 复制模板
cp .env.example .env

# 编辑 .env，支持 http:// 和 socks5://
HTTP_PROXY=http://127.0.0.1:7890
```

- 程序自动读取 `.env` 或系统环境变量 `HTTP_PROXY`
- 国内平台无需代理
- 未配置代理时，国际平台可能超时或失败

### 平台优先级

编辑 `src/main/resources/platforms.yml`：

```yaml
platforms:
  zhihu:
    enabled: true
    priority: 90  # 1-100，越大越靠前
```

修改后需 `mvn clean package` 并重启。

## 项目结构

```
src/main/java/com/paiad/mcp/
├── config/      # 平台配置
├── crawler/     # 各平台爬虫实现
├── model/
│   ├── pojo/    # 领域实体（NewsItem, CrawlResult）
│   └── vo/      # 视图对象（NewsItemVO）
├── service/     # 业务服务层
├── tool/        # MCP 工具定义
└── util/        # 工具类
```

## 注意事项

- 服务使用 STDIO 通信，日志输出到 `System.err`——请勿修改日志配置导致日志打印到 stdout
- 部分平台可能因反爬策略调整导致爬取失败，欢迎提交 Issue 或 PR
- 请合理使用，遵守各平台使用条款

## 贡献

- 提交 Bug 报告
- 提出新功能建议
- 提交 Pull Request

## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=paiad/mcp-java-news-crawler&type=date&legend=top-left)](https://www.star-history.com/#paiad/mcp-java-news-crawler&type=date&legend=top-left)

## 许可证

[MIT License](https://opensource.org/licenses/MIT)

---

<div align="center">

**本项目在一定程度上可以帮你告别信息茧房，用 AI 洞悉世界。如果对你有帮助，点一个 Star！欢迎大家提出 issues 和 pull request！**

[⬆️ 回到顶部](#mcp-java-news-crawler)

</div>
