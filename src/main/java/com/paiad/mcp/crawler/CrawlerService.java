package com.paiad.mcp.crawler;

import com.paiad.mcp.model.NewsItem;

import java.util.List;

/**
 * 爬虫服务接口
 * 
 * @author Paiad
 */
public interface CrawlerService {

    /**
     * 获取平台ID
     */
    String getPlatformId();

    /**
     * 获取平台名称
     */
    String getPlatformName();

    /**
     * 爬取热榜数据
     */
    List<NewsItem> crawl();

    /**
     * 安全爬取，捕获异常
     */
    List<NewsItem> safeCrawl();
}
