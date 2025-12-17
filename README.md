# MCP Java News Crawler

一个基于 **Model Context Protocol (MCP)** 的 Java 热点新闻爬虫服务。它允许 AI 助手（如 Claude、Codex、Gemini、Cherry Studio 等）实时通过简单的自然语言指令，获取全网热点新闻、搜索特定事件并分析舆论趋势。

<details>
<summary>👉 点击展开：LLM调用mcp-java-news-crawler图示</summary>
<br>

![p1](https://cdn.jsdelivr.net/gh/paiad/picture-bed@main/img/mcp-news-crawler-p1.png)
![p2](https://cdn.jsdelivr.net/gh/paiad/picture-bed@main/img/mcp-news-crawler-p2.png)
![p3](https://cdn.jsdelivr.net/gh/paiad/picture-bed@main/img/mcp-news-crawler-p3.png)
![p4](https://cdn.jsdelivr.net/gh/paiad/picture-bed@main/img/mcp-news-crawler-p4.png)
![p5](https://cdn.jsdelivr.net/gh/paiad/picture-bed@main/img/mcp-news-crawler-p5.png)
![p6](https://cdn.jsdelivr.net/gh/paiad/picture-bed@main/img/mcp-news-crawler-p6.png)
![p7](https://cdn.jsdelivr.net/gh/paiad/picture-bed@main/img/mcp-news-crawler-p7.png)

</details>

## 📂 项目结构

```
src/main/java/com/paiad/mcp/
├── config/      # 平台与分类配置
├── crawler/     # 🕷️ 各平台爬虫实现
├── model/       # 📦 数据模型
├── service/     # 🔧 业务服务层
├── tool/        # 🛠️ MCP 工具定义
└── util/        # ⭐️ 工具类
```

## ✨ 主要功能

### MCP 工具列表

| 工具名称               | 功能描述                              |
| ---------------------- | ------------------------------------- |
| `get_hot_news`         | 🔥 获取多平台热榜新闻                 |
| `search_news`          | 🔍 关键词搜索新闻                     |
| `get_trending_topics`  | 📈 智能分析热门话题及跨平台热度       |
| `get_platform_list`    | 📋 获取所有可用平台列表及状态         |
| `get_news_by_category` | 🏷️ 按分类获取新闻（支持用户偏好配置） |
| `get_news_summary`     | 📰 聚合多平台新闻摘要，去重排序       |

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
| `hacker_news`  | Hacker News  | 🌍 国际 | 黑客新闻     |

> [!NOTE]
> **关于国际新闻平台**：国际媒体通常较为分散，数据多通过 RSS 订阅获取。RSS 仅提供最新文章列表，包括标题、摘要和发布时间，不包含热度或互动数据（如点赞、评论数）。
> 因此在此项目中，默认通过 RSS 获取的国际平台的 `hotScore` 为 0，而`hotDesc` 一般为新闻发布时间（GMT）。

### 🏷️ 新闻分类

支持 7 个内置分类，可通过 `get_news_by_category` 工具使用：

| 分类 ID         | 名称     | 说明                         |
| --------------- | -------- | ---------------------------- |
| `ai`            | 人工智能 | ChatGPT、大模型、机器学习等  |
| `tech`          | 科技     | 互联网、芯片、手机、新能源等 |
| `finance`       | 财经     | 股票、基金、经济、投资等     |
| `entertainment` | 娱乐     | 明星、电影、综艺等           |
| `sports`        | 体育     | 足球、篮球、电竞等           |
| `world`         | 国际     | 国际新闻、外交、时政等       |
| `society`       | 社会     | 民生、教育、医疗等           |

## ⚙️ 配置文件

### 平台优先级配置

编辑 `src/main/resources/platforms.yml` 可自定义平台优先级和启用状态：

```yaml
platforms:
  zhihu:
    enabled: true # 是否启用
    priority: 90 # 优先级 (1-100)，越大越靠前
  # ... 其他平台
```

### 用户偏好配置

编辑 `src/main/resources/preferences.yml` 可自定义分类权重：

```yaml
# 分类权重 (0-5)
# 0=不感兴趣, 1=偶尔, 2=一般, 3=比较关注, 4=非常关注, 5=重点关注
category_weights:
  ai: 5 # 人工智能
  tech: 4 # 科技
  finance: 3 # 财经
  entertainment: 2 # 娱乐
  sports: 1 # 体育
  world: 3 # 国际
  society: 2 # 社会

default_limit: 30 # 默认返回条数
```

当调用 `get_news_by_category` 不指定分类时，会根据权重比例混合返回新闻。

### 分类定义配置

编辑 `src/main/resources/categories.yml` 可自定义分类的关键词和平台映射：

```yaml
categories:
  ai:
    name: "人工智能"
    keywords:
      - "ai"
      - "chatgpt"
      - "大模型"
    platforms:
      techcrunch: 5
      zhihu: 4
```

修改配置后需重新打包 (`mvn clean package`) 并重启服务。

### 🌐 代理配置（访问国际平台）

如需访问国际新闻平台（如 Reddit、Google News、BBC 等），需配置代理：

1. 复制 `.env.example` 为 `.env`：

```bash
cp .env.example .env
```

2. 编辑 `.env` 文件，设置代理地址：

```bash
# 支持 http:// 和 socks5:// 协议
HTTP_PROXY=http://127.0.0.1:7890
```

> [!NOTE]
>
> - 程序会自动读取 `.env` 文件或系统环境变量 `HTTP_PROXY`
> - 国内平台（微博、抖音等）无需代理
> - 如不配置代理，国际平台可能爬取会超时或失败

## 🛠️ 构建项目

本项目使用标准 Maven 构建，要求 JDK 21+（使用虚拟线程特性）。

```bash
mvn clean package -DskipTests
```

构建完成后，会在 `target/` 目录下生成 `mcp-java-news-crawler-jar-with-dependencies.jar`。

## 🚀 快速运行

你可以通过命令行直接运行（仅供测试，MCP 客户端会自动在后台运行它）：

```bash
java -jar target/mcp-java-news-crawler-jar-with-dependencies.jar
```

### 手动测试 MCP 协议

启动后，服务会等待 stdin 输入。你可以粘贴以下 JSON 测试连接：

**1. 初始化连接：**

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "initialize",
  "params": { "protocolVersion": "2024-11-05", "capabilities": {} }
}
```

**2. 查看可用工具：**

```json
{ "jsonrpc": "2.0", "id": 2, "method": "tools/list", "params": {} }
```

**3. 调用工具获取新闻：**

```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "method": "tools/call",
  "params": { "name": "get_hot_news", "arguments": { "limit": 5 } }
}
```

> [!TIP]
> 每条 JSON 输入后按回车发送。服务会通过 stdout 返回 JSON-RPC 响应。

## 🐳 Docker 部署

在项目根目录下执行以下命令：

### 构建镜像

```bash
docker build -t mcp-java-news-crawler .
```

### 运行容器（测试）

由于 MCP 服务器使用 STDIO 通信，需要以交互模式运行：

```bash
docker run -it --rm mcp-java-news-crawler
```

### MCP 客户端配置（Docker 方式）

如果你希望通过 Docker 运行 MCP 服务，可以在各客户端中使用以下配置：

**Cherry Studio / Claude / Codex / Gemini / Cursor 等：**

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

**通用 MCP 工具配置格式：**

```json
{
  "tools": [
    {
      "name": "news_crawler",
      "description": "Search news using mcp-java-news-crawler",
      "command": ["docker", "run", "-i", "--rm", "mcp-java-news-crawler"]
    }
  ]
}
```

> [!NOTE]
>
> - Docker 镜像采用多阶段构建，最终镜像基于 `eclipse-temurin:21-jre-alpine`，体积小巧
> - 使用 `-i` 参数保持 stdin 打开，`-it` 用于交互测试，MCP 客户端只需 `-i`
> - 确保 Docker 已安装并且镜像已构建

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

## ⚠️ 注意事项

- 本项目使用标准输入输出 (STDIO) 通信，所有日志已被配置为输出到 `System.err`，请勿修改日志配置导致日志打印到标准输出
- 部分平台可能因反爬策略调整导致爬取失败，欢迎提交 Issue 或 PR
- 请合理使用，遵守各平台的使用条款

## 🤝 贡献

欢迎贡献代码！你可以：

- 🐛 提交 Bug 报告
- 💡 提出新功能建议
- 🔧 提交 Pull Request

## ⭐ Star History

[![Star History Chart](https://api.star-history.com/svg?repos=paiad/mcp-java-news-crawler&type=date&legend=top-left)](https://www.star-history.com/#paiad/mcp-java-news-crawler&type=date&legend=top-left)

## 📄 许可证

本项目基于 [GPL-3.0 License](https://www.gnu.org/licenses/gpl-3.0.html) 开源。

---

<div align="center">

**本项目在一定程度上可以帮你告别信息茧房，用 AI 洞悉世界。如果此项目对你有帮助，
希望你点一个 ⭐ Star！当然，因为笔者实力有限，项目本身可能存在一些或多或少的问题，亦或者你对项目想提出一些进一步的意见，
笔者在此：欢迎大家提出 issues 和 pull request！**

[⬆️ 回到顶部](#mcp-java-news-crawler)

</div>
