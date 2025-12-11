# Changelog

All notable changes to this project will be documented in this file.

## [v1.1.0] - 2025-12-11

### ✨ New Features
- **新增平台支持**: 添加 Google News、Reddit、TikTok、X (Twitter) 爬虫
- **环境变量配置**: 支持通过 `.env` 文件配置 API Token（如 `TWITTER_BEARER_TOKEN`）
- **项目结构文档**: README 新增项目结构说明

### 🐛 Bug Fixes
- **缓存修复**: 修复平台缓存刷新不一致和过期数据问题
- **HTTP 重试**: 优化 HttpClientFactory 的重试机制

### 📝 Documentation
- 更新 README.md，新增 Claude Code 配置说明
- 添加 `.env.example` 配置示例文件

### 🧪 Tests
- 更新 CrawlerTest.java，完善爬虫测试用例

---

## [v1.0.0] - 2025-12-10

### ✨ New Features
- 实现 MCP 服务端，支持多平台热点新闻爬取
- 添加工具: `get_hot_news`, `search_news`, `get_trending_topics`
- 支持平台: 知乎、微博、B站、百度、抖音、头条、华尔街见闻

### 🐛 Bug Fixes
- 修复 NewsService 中的 JSON 解析问题
