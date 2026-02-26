package com.paiad.mcp.registry;

import com.paiad.mcp.config.PlatformPriorityConfig;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Centralized platform registry.
 */
public class PlatformRegistry {

    private static PlatformRegistry instance;

    private final Map<String, PlatformDescriptor> descriptorsById;
    private final Map<String, String> aliasToId;
    private final int defaultPlatformCount;

    private PlatformRegistry() {
        PlatformPriorityConfig priorityConfig = PlatformPriorityConfig.getInstance();
        this.defaultPlatformCount = priorityConfig.getDefaultPlatformCount();
        this.descriptorsById = new LinkedHashMap<>();
        this.aliasToId = new HashMap<>();
        init(priorityConfig);
    }

    public static synchronized PlatformRegistry getInstance() {
        if (instance == null) {
            instance = new PlatformRegistry();
        }
        return instance;
    }

    private void init(PlatformPriorityConfig priorityConfig) {
        register(priorityConfig, "zhihu", "知乎", "https://www.zhihu.com/hot", Set.of("zh", "Zhihu"));
        register(priorityConfig, "weibo", "微博", "https://weibo.com/ajax/side/hotSearch", Set.of("wb", "Weibo"));
        register(priorityConfig, "bilibili", "B站", "https://api.bilibili.com/x/web-interface/ranking/v2",
                Set.of("bili", "Bilibili"));
        register(priorityConfig, "baidu", "百度", "https://top.baidu.com/board?tab=realtime", Set.of("bd", "Baidu"));
        register(priorityConfig, "douyin", "抖音", "https://www.douyin.com/aweme/v1/web/hot/search/list/",
                Set.of("dy", "Douyin"));
        register(priorityConfig, "toutiao", "头条", "https://www.toutiao.com/hot-event/hot-board/",
                Set.of("tt", "Toutiao"));
        register(priorityConfig, "wallstreetcn", "华尔街见闻", "https://api-one-wscn.awtmt.com/apiv1/content/articles/hot",
                Set.of("wallstreet", "WallStreetCN"));

        register(priorityConfig, "google_news", "Google News", "https://news.google.com/",
                Set.of("google", "googlenews"));
        register(priorityConfig, "reddit", "Reddit", "https://www.reddit.com/r/worldnews/", Set.of("rd"));
        register(priorityConfig, "bbc", "BBC", "https://www.bbc.com/news", Set.of("bbc_news"));
        register(priorityConfig, "reuters", "Reuters", "https://www.reuters.com/", Collections.emptySet());
        register(priorityConfig, "apnews", "AP News", "https://apnews.com/", Set.of("ap"));
        register(priorityConfig, "guardian", "The Guardian", "https://www.theguardian.com/", Set.of("theguardian"));
        register(priorityConfig, "techcrunch", "TechCrunch", "https://techcrunch.com/", Set.of("tc"));
        register(priorityConfig, "hacker_news", "Hacker News", "https://news.ycombinator.com/",
                Set.of("hn", "hackernews"));
    }

    private void register(PlatformPriorityConfig priorityConfig, String id, String name, String url, Set<String> aliases) {
        PlatformPriorityConfig.PriorityInfo info = priorityConfig.getPriorityInfo(id);
        boolean enabled = info == null || info.isEnabled();
        int priority = info != null ? info.getPriority() : 0;
        String description = info != null ? info.getDescription() : id;

        Set<String> allAliases = new LinkedHashSet<>(aliases);
        allAliases.add(id);
        PlatformDescriptor descriptor = new PlatformDescriptor(id, name, url, allAliases, enabled, priority, description);
        descriptorsById.put(id, descriptor);

        for (String alias : descriptor.aliases()) {
            aliasToId.put(alias, id);
        }
    }

    public Optional<PlatformDescriptor> getById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(descriptorsById.get(id));
    }

    public Optional<PlatformDescriptor> resolve(String aliasOrId) {
        if (aliasOrId == null) {
            return Optional.empty();
        }
        String key = aliasOrId.trim().toLowerCase();
        String id = aliasToId.get(key);
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(descriptorsById.get(id));
    }

    public String resolveId(String aliasOrId) {
        return resolve(aliasOrId).map(PlatformDescriptor::id).orElse(null);
    }

    public String getName(String id) {
        return getById(id).map(PlatformDescriptor::name).orElse(null);
    }

    public boolean isEnabled(String id) {
        return getById(id).map(PlatformDescriptor::enabled).orElse(true);
    }

    public int getPriority(String id) {
        return getById(id).map(PlatformDescriptor::priority).orElse(0);
    }

    public Set<String> getAllPlatformIds() {
        return Collections.unmodifiableSet(descriptorsById.keySet());
    }

    public List<String> getEnabledPlatformIdsSorted() {
        return descriptorsById.values().stream()
                .filter(PlatformDescriptor::enabled)
                .sorted(byPriority())
                .map(PlatformDescriptor::id)
                .collect(Collectors.toList());
    }

    public List<String> getDefaultPlatformIds() {
        List<String> all = getEnabledPlatformIdsSorted();
        if (defaultPlatformCount > 0 && defaultPlatformCount < all.size()) {
            return all.subList(0, defaultPlatformCount);
        }
        return all;
    }

    public List<String> sortByPriority(Collection<String> platformIds) {
        return platformIds.stream()
                .distinct()
                .sorted(Comparator.comparingInt(this::getPriority).reversed().thenComparing(String::compareTo))
                .collect(Collectors.toList());
    }

    private Comparator<PlatformDescriptor> byPriority() {
        return Comparator.comparingInt(PlatformDescriptor::priority).reversed()
                .thenComparing(PlatformDescriptor::id);
    }
}
