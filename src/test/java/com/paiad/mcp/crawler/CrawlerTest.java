package com.paiad.mcp.crawler;

import com.paiad.mcp.model.NewsItem;

import java.util.*;

/**
 * 爬虫测试类 - 测试每个平台的爬虫
 *
 * 最后会输出一个表格展示各平台运行结果
 *
 * @author Paiad
 */
public class CrawlerTest {

    // 存储最终表格用的数据
    private static final List<ResultRow> resultTable = new ArrayList<>();

    public static void main(String[] args) {

        System.out.println("========== 爬虫测试开始 ==========\n");

        // 测试国内平台
         testCrawler(new WeiboCrawler());
         testCrawler(new DouyinCrawler());
         testCrawler(new ToutiaoCrawler());
         testCrawler(new BilibiliCrawler());
         testCrawler(new BaiduCrawler());
         testCrawler(new ZhihuCrawler());
         testCrawler(new WallStreetCnCrawler());

        // 测试国际平台
        testCrawler(new RedditCrawler());
        testCrawler(new GoogleNewsCrawler());

        System.out.println("\n========== 爬虫测试结束 ==========\n");

        printSummaryTable(); // ✅ 输出表格
    }

    /**
     * 测试单个爬虫
     */
    private static void testCrawler(AbstractCrawler crawler) {
        String platformName = crawler.getPlatformName();
        String platformId = crawler.getPlatformId();

        System.out.println("----------------------------------------");
        System.out.println("📰 测试平台: " + platformName + " (" + platformId + ")");
        System.out.println("----------------------------------------");

        long startTime = System.currentTimeMillis();
        try {
            List<NewsItem> items = crawler.safeCrawl();
            long endTime = System.currentTimeMillis();

            if (items.isEmpty()) {
                System.out.println("❌ 结果: 未获取到数据");
                addResult(platformName, platformId, "失败", 0, endTime - startTime, "empty result");
            } else {
                System.out.println("✅ 结果: 成功获取 " + items.size() + " 条数据");
                System.out.println("⏱️ 耗时: " + (endTime - startTime) + " ms");
                addResult(platformName, platformId, "成功", items.size(), endTime - startTime, "-");

                System.out.println("\n📋 前5条数据预览:");
                int count = Math.min(5, items.size());
                for (int i = 0; i < count; i++) {
                    NewsItem item = items.get(i);
                    System.out.println("  " + item.getRank() + ". " + item.getTitle());
                    System.out.println("     热度: " + (item.getHotDesc() != null ? item.getHotDesc() : "N/A"));
                    System.out.println("     链接: " + item.getUrl());
                }
            }
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            System.out.println("❌ 错误: " + e.getMessage());
            addResult(platformName, platformId, "异常", 0, endTime - startTime, e.getClass().getSimpleName());
        }

        System.out.println();
    }

    /**
     * 存一行结果
     */
    private static void addResult(String name, String id, String status, int count, long timeMs, String error) {
        resultTable.add(new ResultRow(name, id, status, count, timeMs, error));
    }

    /**
     * 打印最终表格
     */
    private static void printSummaryTable() {
        System.out.println("============= 平台测试结果总览 =============");

        String format = "| %-12s | %-10s | %-4s | %-6s | %-8s | %-20s |%n";
        System.out.printf(format, "平台", "ID", "状态", "条数", "耗时ms", "错误信息");
        System.out.println("|--------------|------------|------|--------|----------|----------------------|");

        for (ResultRow row : resultTable) {
            System.out.printf(
                    format,
                    row.platformName,
                    row.platformId,
                    row.status,
                    row.count,
                    row.timeMs,
                    row.error);
        }

        System.out.println("============================================");
    }

    /**
     * 结果行结构
     */
    private static class ResultRow {
        String platformName;
        String platformId;
        String status;
        int count;
        long timeMs;
        String error;

        public ResultRow(String platformName, String platformId, String status, int count, long timeMs, String error) {
            this.platformName = platformName;
            this.platformId = platformId;
            this.status = status;
            this.count = count;
            this.timeMs = timeMs;
            this.error = error;
        }
    }

    /**
     * 仅测试单个平台
     */
    public static void testSinglePlatform(String platformId) {
        AbstractCrawler crawler = switch (platformId.toLowerCase()) {
            case "weibo" -> new WeiboCrawler();
            case "douyin" -> new DouyinCrawler();
            case "toutiao" -> new ToutiaoCrawler();
            case "bilibili" -> new BilibiliCrawler();
            case "baidu" -> new BaiduCrawler();
            case "zhihu" -> new ZhihuCrawler();
            case "wallstreetcn" -> new WallStreetCnCrawler();
            case "reddit" -> new RedditCrawler();
            case "google_news" -> new GoogleNewsCrawler();
            default -> null;
        };

        if (crawler == null) {
            System.out.println("未知平台: " + platformId);
            return;
        }

        testCrawler(crawler);
        printSummaryTable(); // 单平台时也输出表格
    }
}
