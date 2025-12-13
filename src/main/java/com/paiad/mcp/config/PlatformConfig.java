package com.paiad.mcp.config;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 平台配置类
 * 
 * @author Paiad
 */
@Data
public class PlatformConfig {

    /**
     * 支持的平台信息
     */
    public static final Map<String, PlatformInfo> PLATFORMS = new HashMap<>();

    static {
        // 国内平台
        PLATFORMS.put("zhihu", new PlatformInfo("zhihu", "知乎", "https://www.zhihu.com/hot"));
        PLATFORMS.put("weibo", new PlatformInfo("weibo", "微博", "https://weibo.com/ajax/side/hotSearch"));
        PLATFORMS.put("bilibili",
                new PlatformInfo("bilibili", "B站", "https://api.bilibili.com/x/web-interface/ranking/v2"));
        PLATFORMS.put("baidu", new PlatformInfo("baidu", "百度", "https://top.baidu.com/board?tab=realtime"));
        PLATFORMS.put("douyin",
                new PlatformInfo("douyin", "抖音", "https://www.douyin.com/aweme/v1/web/hot/search/list/"));
        PLATFORMS.put("toutiao", new PlatformInfo("toutiao", "头条", "https://www.toutiao.com/hot-event/hot-board/"));
        PLATFORMS.put("wallstreetcn",
                new PlatformInfo("wallstreetcn", "华尔街见闻", "https://api-one-wscn.awtmt.com/apiv1/content/articles/hot"));

        // 国际平台
        PLATFORMS.put("google_news", new PlatformInfo("google_news", "Google News", "https://news.google.com/"));
        PLATFORMS.put("reddit", new PlatformInfo("reddit", "Reddit", "https://www.reddit.com/r/worldnews/"));
        PLATFORMS.put("bbc", new PlatformInfo("bbc", "BBC", "https://www.bbc.com/news"));
        PLATFORMS.put("reuters", new PlatformInfo("reuters", "Reuters", "https://www.reuters.com/"));
        PLATFORMS.put("apnews", new PlatformInfo("apnews", "AP News", "https://apnews.com/"));
        PLATFORMS.put("guardian", new PlatformInfo("guardian", "The Guardian", "https://www.theguardian.com/"));
        PLATFORMS.put("techcrunch", new PlatformInfo("techcrunch", "TechCrunch", "https://techcrunch.com/"));
    }

    /**
     * 获取平台信息
     */
    public static PlatformInfo getPlatform(String platformId) {
        return PLATFORMS.get(platformId);
    }

    /**
     * 获取所有平台ID
     */
    public static String[] getAllPlatformIds() {
        return PLATFORMS.keySet().toArray(new String[0]);
    }

    /**
     * 平台信息
     */
    @Data
    public static class PlatformInfo {
        private String id;
        private String name;
        private String url;

        public PlatformInfo(String id, String name, String url) {
            this.id = id;
            this.name = name;
            this.url = url;
        }
    }
}
