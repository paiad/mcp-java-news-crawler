package com.paiad.mcp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 平台优先级配置加载器
 * 从 platforms.yml 读取平台优先级配置，支持用户自定义
 *
 * @author Paiad
 */
public class PlatformPriorityConfig {

    private static final Logger logger = LoggerFactory.getLogger(PlatformPriorityConfig.class);

    private static final String CONFIG_FILE_NAME = "platforms.yml";
    private static final String EXTERNAL_CONFIG_PATH = "./platforms.yml";

    private static PlatformPriorityConfig instance;

    /**
     * 平台优先级信息
     */
    private final Map<String, PriorityInfo> priorityInfoMap = new HashMap<>();

    /**
     * 默认调用的平台数量（0 表示使用所有启用的平台）
     */
    private int defaultPlatformCount = 0;

    private PlatformPriorityConfig() {
        loadConfig();
    }

    public static synchronized PlatformPriorityConfig getInstance() {
        if (instance == null) {
            instance = new PlatformPriorityConfig();
        }
        return instance;
    }

    /**
     * 重新加载配置（支持热加载）
     */
    public void reload() {
        priorityInfoMap.clear();
        loadConfig();
    }

    @SuppressWarnings("unchecked")
    private void loadConfig() {
        Yaml yaml = new Yaml();
        Map<String, Object> config = null;

        // 优先加载外部配置文件（允许用户自定义）
        Path externalPath = Paths.get(EXTERNAL_CONFIG_PATH);
        if (Files.exists(externalPath)) {
            try (InputStream is = Files.newInputStream(externalPath)) {
                config = yaml.load(is);
                logger.info("从外部文件加载平台优先级配置: {}", externalPath.toAbsolutePath());
            } catch (Exception e) {
                logger.warn("加载外部配置文件失败: {}", e.getMessage());
            }
        }

        // 若外部文件不存在或加载失败，从 classpath 加载
        if (config == null) {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE_NAME)) {
                if (is != null) {
                    config = yaml.load(is);
                    logger.info("从 classpath 加载平台优先级配置");
                }
            } catch (Exception e) {
                logger.warn("加载 classpath 配置文件失败: {}", e.getMessage());
            }
        }

        // 解析配置
        if (config != null) {
            parseConfig(config);
        } else {
            logger.warn("未找到平台配置文件，使用默认配置");
            initDefaultConfig();
        }
    }

    @SuppressWarnings("unchecked")
    private void parseConfig(Map<String, Object> config) {
        // 读取默认平台数量配置
        Object countObj = config.get("default_platform_count");
        if (countObj instanceof Number) {
            this.defaultPlatformCount = ((Number) countObj).intValue();
        }
        logger.info("默认平台数量配置: {}", defaultPlatformCount > 0 ? defaultPlatformCount : "全部");

        // 解析平台配置
        Map<String, Object> platforms = (Map<String, Object>) config.get("platforms");
        if (platforms != null) {
            for (Map.Entry<String, Object> entry : platforms.entrySet()) {
                String platformId = entry.getKey();
                Map<String, Object> platformData = (Map<String, Object>) entry.getValue();

                boolean enabled = getBoolean(platformData, "enabled", true);
                int priority = getInt(platformData, "priority", 50);
                String description = getString(platformData, "description", platformId);

                priorityInfoMap.put(platformId, new PriorityInfo(platformId, enabled, priority, description));
            }
        }

        logger.info("加载 {} 个平台优先级配置", priorityInfoMap.size());
    }

    private void initDefaultConfig() {
        // 不再提供硬编码的默认配置,强制要求使用 platforms.yml
        logger.warn("未找到 platforms.yml 配置文件,请在项目根目录或 classpath 中提供该文件");
        logger.warn("参考格式请查看项目文档");
    }

    // ========== 工具方法 ==========

    private boolean getBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        Object val = map.get(key);
        if (val instanceof Boolean) {
            return (Boolean) val;
        }
        return defaultValue;
    }

    private int getInt(Map<String, Object> map, String key, int defaultValue) {
        Object val = map.get(key);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        return defaultValue;
    }

    private String getString(Map<String, Object> map, String key, String defaultValue) {
        Object val = map.get(key);
        if (val instanceof String) {
            return (String) val;
        }
        return defaultValue;
    }

    // ========== 公共 API ==========

    /**
     * 获取平台优先级信息
     */
    public PriorityInfo getPriorityInfo(String platformId) {
        return priorityInfoMap.get(platformId);
    }

    /**
     * 获取平台优先级
     */
    public int getPriority(String platformId) {
        PriorityInfo info = priorityInfoMap.get(platformId);
        return info != null ? info.getPriority() : 0;
    }

    /**
     * 检查平台是否启用
     */
    public boolean isEnabled(String platformId) {
        PriorityInfo info = priorityInfoMap.get(platformId);
        // 如果配置中没有该平台，默认启用
        return info == null || info.isEnabled();
    }

    /**
     * 获取所有启用的平台 ID（按优先级降序）
     */
    public List<String> getEnabledPlatformIdsSorted() {
        return priorityInfoMap.values().stream()
                .filter(PriorityInfo::isEnabled)
                .sorted(Comparator.comparingInt(PriorityInfo::getPriority).reversed()
                        .thenComparing(PriorityInfo::getId))
                .map(PriorityInfo::getId)
                .collect(Collectors.toList());
    }

    /**
     * 获取默认平台 ID 列表（按优先级排序，受 default_platform_count 限制）
     */
    public List<String> getDefaultPlatformIds() {
        List<String> all = getEnabledPlatformIdsSorted();
        if (defaultPlatformCount > 0 && defaultPlatformCount < all.size()) {
            return all.subList(0, defaultPlatformCount);
        }
        return all;
    }

    /**
     * 获取默认平台数量配置
     */
    public int getDefaultPlatformCount() {
        return defaultPlatformCount;
    }

    /**
     * 按优先级对平台列表排序
     */
    public List<String> sortByPriority(Collection<String> platformIds) {
        return platformIds.stream()
                .sorted(Comparator.comparingInt(this::getPriority).reversed()
                        .thenComparing(String::compareTo))
                .collect(Collectors.toList());
    }

    // ========== 内部类 ==========

    /**
     * 平台优先级信息
     */
    public static class PriorityInfo {
        private final String id;
        private final boolean enabled;
        private final int priority;
        private final String description;

        public PriorityInfo(String id, boolean enabled, int priority, String description) {
            this.id = id;
            this.enabled = enabled;
            this.priority = priority;
            this.description = description;
        }

        public String getId() {
            return id;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public int getPriority() {
            return priority;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return String.format("PriorityInfo{id='%s', enabled=%s, priority=%d}", id, enabled, priority);
        }
    }
}
