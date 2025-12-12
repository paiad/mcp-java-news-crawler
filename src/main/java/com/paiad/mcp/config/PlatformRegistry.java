package com.paiad.mcp.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Platform Registry 用于统一管理平台的 ID 及其别名映射
 *
 * @author Paiad
 */
public class PlatformRegistry {

    private static final Map<String, String> ALIAS_MAP = new HashMap<>();
    private static final Map<String, String> OFFICIAL_NAMES = new HashMap<>();

    static {
        // Register platforms and aliases
        register("zhihu", "Zhihu", "zh");
        register("weibo", "Weibo", "wb");
        register("bilibili", "Bilibili", "bili");
        register("baidu", "Baidu", "bd");
        register("douyin", "Douyin", "dy");
        register("toutiao", "Toutiao", "tt");
        register("reddit", "Reddit", "rd");
        register("google_news", "Google News", "google", "googlenews");
        register("wallstreetcn", "WallStreetCN", "unique", "wallstreet");
    }

    private static void register(String id, String name, String... aliases) {
        OFFICIAL_NAMES.put(id, name);
        ALIAS_MAP.put(id.toLowerCase(), id);
        for (String alias : aliases) {
            ALIAS_MAP.put(alias.toLowerCase(), id);
        }
    }

    /**
     * 将输入字符串解析为官方平台 ID
     * 若未匹配到则返回 null
     */
    public static String resolveId(String input) {
        if (input == null)
            return null;
        return ALIAS_MAP.get(input.trim().toLowerCase());
    }

    public static String getName(String id) {
        return OFFICIAL_NAMES.get(id);
    }

    public static Set<String> getAllIds() {
        return OFFICIAL_NAMES.keySet();
    }
}
