package com.paiad.mcp.crawler;

import com.paiad.mcp.crawler.domestic.*;
import com.paiad.mcp.crawler.international.*;
import com.paiad.mcp.model.pojo.NewsItem;

import java.util.*;
import java.util.concurrent.*;

/**
 * 爬虫测试类 - 测试每个平台的爬虫
 *
 * 使用虚拟线程 (Java 21+) 并发测试所有爬虫
 * 最后会输出一个表格展示各平台运行结果
 *
 * @author Paiad
 */
public class CrawlerTest {

    // 存储最终表格用的数据（线程安全）
    private static final List<ResultRow> resultTable = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) {

        System.out.println("========== 爬虫测试开始 (虚拟线程并发模式) ==========\n");

        // 所有爬虫列表
        List<AbstractCrawler> crawlers = List.of(
                // 国内平台
                new WeiboCrawler(),
                new DouyinCrawler(),
                new ToutiaoCrawler(),
                new BilibiliCrawler(),
                new BaiduCrawler(),
                new ZhihuCrawler(),
                new WallStreetCnCrawler(),
                // 国际平台
                new RedditCrawler(),
                new GoogleNewsCrawler(),
                new BBCCrawler(),
                new ReutersCrawler(),
                new APNewsCrawler(),
                new GuardianCrawler(),
                new TechCrunchCrawler(),
                new HackerNewsCrawler());

        long totalStartTime = System.currentTimeMillis();

        // 使用虚拟线程并发测试所有爬虫
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>();
            for (AbstractCrawler crawler : crawlers) {
                futures.add(executor.submit(() -> testCrawler(crawler)));
            }

            // 等待所有任务完成
            for (Future<?> future : futures) {
                try {
                    future.get(60, TimeUnit.SECONDS); // 单个爬虫最多等待60秒
                } catch (TimeoutException e) {
                    System.err.println("⚠️ 某个爬虫测试超时");
                } catch (Exception e) {
                    System.err.println("⚠️ 爬虫测试异常: " + e.getMessage());
                }
            }
        }

        long totalEndTime = System.currentTimeMillis();

        System.out.println("\n========== 爬虫测试结束 ==========\n");
        System.out.println("🚀 总耗时: " + (totalEndTime - totalStartTime) + " ms (虚拟线程并发执行)\n");

        printSummaryTable(); // ✅ 输出表格
    }

    // 用于同步打印输出的锁对象
    private static final Object PRINT_LOCK = new Object();

    /**
     * 测试单个爬虫（线程安全，输出不会交织）
     */
    private static void testCrawler(AbstractCrawler crawler) {
        String platformName = crawler.getPlatformName();
        String platformId = crawler.getPlatformId();

        // 使用 StringBuilder 收集所有输出
        StringBuilder output = new StringBuilder();
        output.append("\n----------------------------------------\n");
        output.append("📰 测试平台: ").append(platformName).append(" (").append(platformId).append(")\n");
        output.append("----------------------------------------\n");

        long startTime = System.currentTimeMillis();
        try {
            List<NewsItem> items = crawler.safeCrawl();
            long endTime = System.currentTimeMillis();

            if (items.isEmpty()) {
                output.append("❌ 结果: 未获取到数据\n");
                addResult(platformName, platformId, "失败", 0, endTime - startTime, "empty result");
            } else {
                output.append("✅ 结果: 成功获取 ").append(items.size()).append(" 条数据\n");
                output.append("⏱️ 耗时: ").append(endTime - startTime).append(" ms\n");
                addResult(platformName, platformId, "成功", items.size(), endTime - startTime, "-");

                output.append("\n📋 前5条数据预览:\n");
                int count = Math.min(5, items.size());
                for (int i = 0; i < count; i++) {
                    NewsItem item = items.get(i);
                    output.append("  ").append(item.getRank()).append(". ").append(item.getTitle()).append("\n");
                    output.append("     热度: ").append(item.getHotDesc() != null ? item.getHotDesc() : "N/A")
                            .append("\n");
                    output.append("     链接: ").append(item.getUrl()).append("\n");
                }
            }
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            output.append("❌ 错误: ").append(e.getMessage()).append("\n");
            addResult(platformName, platformId, "异常", 0, endTime - startTime, e.getClass().getSimpleName());
        }

        // 原子性打印：确保每个平台的输出不会被其他线程打断
        synchronized (PRINT_LOCK) {
            System.out.print(output);
        }
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
            case "bbc" -> new BBCCrawler();
            case "reuters" -> new ReutersCrawler();
            case "apnews" -> new APNewsCrawler();
            case "guardian" -> new GuardianCrawler();
            case "techcrunch" -> new TechCrunchCrawler();
            case "hacker_news" -> new HackerNewsCrawler();
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
