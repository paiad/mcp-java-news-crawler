package com.paiad.mcp.config;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 平台配置类
 * 统一管理平台信息、别名映射
 *
 * @author Paiad
 */
@Data
public class PlatformConfig {

    /**
     * 支持的平台信息
     */
    private static final Map<String, PlatformInfo> PLATFORMS = new HashMap<>();

    /**
     * 别名到官方 ID 的映射
     */
    private static final Map<String, String> ALIAS_MAP = new HashMap<>();

    static {
        // 国内平台
        register("zhihu", "知乎", "https://www.zhihu.com/hot", "zh", "Zhihu");
        register("weibo", "微博", "https://weibo.com/ajax/side/hotSearch", "wb", "Weibo");
        register("bilibili", "B站", "https://api.bilibili.com/x/web-interface/ranking/v2", "bili", "Bilibili");
        register("baidu", "百度", "https://top.baidu.com/board?tab=realtime", "bd", "Baidu");
        register("douyin", "抖音", "https://www.douyin.com/aweme/v1/web/hot/search/list/", "dy", "Douyin");
        register("toutiao", "头条", "https://www.toutiao.com/hot-event/hot-board/", "tt", "Toutiao");
        register("wallstreetcn", "华尔街见闻", "https://api-one-wscn.awtmt.com/apiv1/content/articles/hot", "wallstreet",
                "WallStreetCN");

        // 国际平台
        register("google_news", "Google News", "https://news.google.com/", "google", "googlenews");
        register("reddit", "Reddit", "https://www.reddit.com/r/worldnews/", "rd");
        register("bbc", "BBC", "https://www.bbc.com/news", "bbc_news");
        register("reuters", "Reuters", "https://www.reuters.com/");
        register("apnews", "AP News", "https://apnews.com/", "ap");
        register("guardian", "The Guardian", "https://www.theguardian.com/", "theguardian");
        register("techcrunch", "TechCrunch", "https://techcrunch.com/", "tc");
        register("hacker_news", "Hacker News", "https://news.ycombinator.com/", "hn", "hackernews");
    }

    /**
     * 注册平台信息和别名
     */
    private static void register(String id, String name, String url, String... aliases) {
        PLATFORMS.put(id, new PlatformInfo(id, name, url));
        // 添加 ID 自身作为别名（小写）
        ALIAS_MAP.put(id.toLowerCase(), id);
        // 添加额外的别名
        for (String alias : aliases) {
            ALIAS_MAP.put(alias.toLowerCase(), id);
        }
    }

    // ========== 平台信息 API ==========

    /**
     * 获取平台信息
     */
    public static PlatformInfo getPlatform(String platformId) {
        return PLATFORMS.get(platformId);
    }

    /**
     * 获取所有平台 ID
     */
    public static Set<String> getAllPlatformIds() {
        return PLATFORMS.keySet();
    }

    // ========== 别名解析 API ==========

    /**
     * 将输入字符串（别名或 ID）解析为官方平台 ID
     * 若未匹配到则返回 null
     */
    public static String resolveId(String input) {
        if (input == null)
            return null;
        return ALIAS_MAP.get(input.trim().toLowerCase());
    }

    /**
     * 获取平台显示名称
     */
    public static String getName(String id) {
        PlatformInfo info = PLATFORMS.get(id);
        return info != null ? info.getName() : null;
    }

    // ========== 内部类 ==========

    /**
     * 平台信息
     */
    @Data
    public static class PlatformInfo {
        private final String id;
        private final String name;
        private final String url;

        public PlatformInfo(String id, String name, String url) {
            this.id = id;
            this.name = name;
            this.url = url;
        }
    }
}
