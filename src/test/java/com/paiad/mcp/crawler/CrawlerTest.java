package com.paiad.mcp.crawler;

import com.paiad.mcp.model.NewsItem;

import java.util.List;

/**
 * 爬虫测试类 - 测试每个平台的爬虫
 *
 * @author Paiad
 */
public class CrawlerTest {

    public static void main(String[] args) {
        System.out.println("========== 爬虫测试开始 ==========\n");

        // 测试所有国内平台爬虫
//        testCrawler(new WeiboCrawler());
//        testCrawler(new DouyinCrawler());
//        testCrawler(new ToutiaoCrawler());
//        testCrawler(new BilibiliCrawler());
//        testCrawler(new BaiduCrawler());
//        testCrawler(new ZhihuCrawler());
        testCrawler(new WallStreetCnCrawler());

        System.out.println("\n========== 爬虫测试结束 ==========");
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

        try {
            long startTime = System.currentTimeMillis();
            List<NewsItem> items = crawler.safeCrawl();
            long endTime = System.currentTimeMillis();

            if (items.isEmpty()) {
                System.out.println("❌ 结果: 未获取到数据");
            } else {
                System.out.println("✅ 结果: 成功获取 " + items.size() + " 条数据");
                System.out.println("⏱️ 耗时: " + (endTime - startTime) + " ms");
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
            System.out.println("❌ 错误: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println();
    }

    /**
     * 测试单个指定平台
     */
    public static void testSinglePlatform(String platformId) {
        AbstractCrawler crawler = null;

        switch (platformId.toLowerCase()) {
            case "weibo":
                crawler = new WeiboCrawler();
                break;
            case "douyin":
                crawler = new DouyinCrawler();
                break;
            case "toutiao":
                crawler = new ToutiaoCrawler();
                break;
            case "bilibili":
                crawler = new BilibiliCrawler();
                break;
            case "baidu":
                crawler = new BaiduCrawler();
                break;
            case "zhihu":
                crawler = new ZhihuCrawler();
                break;
            case "wallstreetcn":
                crawler = new WallStreetCnCrawler();
                break;
            default:
                System.out.println("未知平台: " + platformId);
                System.out.println("支持的平台: weibo, douyin, toutiao, bilibili, baidu, zhihu, wallstreetcn");
                return;
        }

        testCrawler(crawler);
    }
}
