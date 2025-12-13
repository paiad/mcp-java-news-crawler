# MCP Java News Crawler

一个基于 **Model Context Protocol (MCP)** 的 Java 热点新闻爬虫服务。它允许 AI 助手（如 Claude、Codex、Gemini、Cherry Studio 等）实时通过简单的自然语言指令，获取全网热点新闻、搜索特定事件并分析舆论趋势。

<details>
<summary>👉 点击展开：LLM调用mcp-java-news-crawler图示</summary>
<br>

![p1](https://cdn.jsdelivr.net/gh/paiad/picture-bed@main/img/mcp-news-crawler-p1.png)
![p2](https://cdn.jsdelivr.net/gh/paiad/picture-bed@main/img/mcp-news-crawler-p2.png)
![p3](https://cdn.jsdelivr.net/gh/paiad/picture-bed@main/img/mcp-news-crawler-p3.png)
![p4](https://cdn.jsdelivr.net/gh/paiad/picture-bed@main/img/mcp-news-crawler-p4.png)

</details>

## 📂 项目结构

```
src/main/java/com/paiad/mcp/
├── config/      # 平台配置
├── crawler/     # 🕷️ 各平台爬虫实现
├── model/       # 📦 数据模型
├── service/     # 🔧 业务服务层
├── tool/        # 🛠️ MCP 工具定义
└── util/        # ⭐️ 工具类
```

## ✨ 主要功能

- **🔥 获取多平台热榜** (`get_hot_news`): 获取多个平台的热点新闻。
- **🔍 关键词搜索** (`search_news`): 在聚合的新闻数据中搜索特定关键词。
- **📈 趋势话题分析** (`get_trending_topics`): 智能分析当前最热门的话题关键词及跨平台热度。

### 📡 支持的新闻媒体

| 平台 ID        | 平台名称     | 类型    | 说明         |
| -------------- | ------------ | ------- | ------------ |
| `douyin`       | 抖音         | 🇨🇳 国内 | 抖音热点     |
| `toutiao`      | 今日头条     | 🇨🇳 国内 | 头条热榜     |
| `weibo`        | 微博         | 🇨🇳 国内 | 微博热搜     |
| `bilibili`     | B 站         | 🇨🇳 国内 | B 站热门视频 |
| `baidu`        | 百度         | 🇨🇳 国内 | 百度热搜     |
| `zhihu`        | 知乎         | 🇨🇳 国内 | 知乎热榜     |
| `wallstreetcn` | 华尔街见闻   | 🇨🇳 国内 | 财经资讯     |
| `google_news`  | Google News  | 🌍 国际 | 谷歌新闻     |
| `reddit`       | Reddit       | 🌍 国际 | Reddit 热帖  |
| `bbc`          | BBC          | 🌍 国际 | BBC 新闻     |
| `reuters`      | Reuters      | 🌍 国际 | 路透社       |
| `apnews`       | AP News      | 🌍 国际 | 美联社       |
| `guardian`     | The Guardian | 🌍 国际 | 卫报         |
| `techcrunch`   | TechCrunch   | 🌍 国际 | 科技资讯     |

### 平台优先级配置

编辑 `src/main/resources/platforms.yml` 可自定义平台优先级和启用状态：

```yaml
platforms:
  zhihu:
    enabled: true # 是否启用
    priority: 90 # 优先级 (1-100)，越大越靠前
  # ... 其他平台

defaults:
  maxDefaultPlatforms: 5 # 未指定平台时返回前 N 个
```

修改后需重新打包 (`mvn clean package`) 并重启服务。

## 🛠️ 构建项目

本项目使用标准 Maven 构建，要求 JDK 17+。

```bash
mvn clean package -DskipTests
```

构建完成后，会在 `target/` 目录下生成 `mcp-java-news-crawler-jar-with-dependencies.jar`。

## 🚀 快速运行

你可以通过命令行直接运行（仅供测试，MCP 客户端会自动在后台运行它）：

```bash
java -jar target/mcp-java-news-crawler-jar-with-dependencies.jar
```

## 🐳 Docker 部署

在项目根目录下执行以下命令：

### 构建镜像

```bash
docker build -t mcp-java-news-crawler .
```

### 运行容器

由于 MCP 服务器使用 STDIO 通信，需要以交互模式运行：

```bash
docker run -it --rm mcp-java-news-crawler
```

> [!NOTE]
> Docker 镜像采用多阶段构建，最终镜像基于 `eclipse-temurin:17-jre-alpine`，体积小巧。

## 🍒 Cherry Studio 配置

打开 Cherry Studio 设置 -> MCP Server -> 添加 JSON 配置：

```json
{
  "mcpServers": {
    "news-crawler": {
      "command": "java",
      "args": [
        "-Dfile.encoding=UTF-8",
        "-jar",
        "/path/to/mcp-java-news-crawler/target/mcp-java-news-crawler-jar-with-dependencies.jar"
      ]
    }
  }
}
```

## 🍊 Claude Code (CLI) 配置

如果你使用的是命令行版的 **Claude Code**，可以通过以下指令直接添加此 MCP 服务：

1. 打开终端运行 `claude` 进入交互界面。
2. 输入以下命令添加服务（请替换为实际路径）：

```bash
/mcp add news-crawler java -Dfile.encoding=UTF-8 -jar /path/to/mcp-java-news-crawler/target/mcp-java-news-crawler-jar-with-dependencies.jar
```

添加成功后，Claude Code 就会自动识别并根据需要调用这些工具。

## 🤖 Codex (CLI) 配置

如果你使用的是 **OpenAI Codex CLI**，可以通过编辑配置文件添加此 MCP 服务：

1. 打开配置文件 `~/.codex/config.json`（不存在则创建）。
2. 添加如下内容（请替换为实际路径）：

```json
{
  "mcpServers": {
    "news-crawler": {
      "command": "java",
      "args": [
        "-Dfile.encoding=UTF-8",
        "-jar",
        "/path/to/mcp-java-news-crawler/target/mcp-java-news-crawler-jar-with-dependencies.jar"
      ]
    }
  }
}
```

保存后重启 Codex CLI，即可自动识别并调用这些工具。

## 💎 Gemini CLI 配置

如果你使用的是 **Google Gemini CLI**，可以通过以下方式配置：

1. 打开配置文件 `~/.gemini/settings.json`（不存在则创建）。
2. 添加如下内容（请替换为实际路径）：

```json
{
  "mcpServers": {
    "news-crawler": {
      "command": "java",
      "args": [
        "-Dfile.encoding=UTF-8",
        "-jar",
        "/path/to/mcp-java-news-crawler/target/mcp-java-news-crawler-jar-with-dependencies.jar"
      ]
    }
  }
}
```

保存后重启 Gemini CLI，工具将自动加载并可用于新闻爬取任务。

---

**注意**: 本项目使用标准输入输出 (STDIO) 通信，所有日志已被配置为输出到 `System.err`，请勿修改日志配置导致日志打印到标准输出。
