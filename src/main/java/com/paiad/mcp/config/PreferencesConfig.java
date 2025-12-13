package com.paiad.mcp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 用户偏好配置加载器
 * 从 preferences.yml 读取用户的分类权重偏好
 *
 * @author Paiad
 */
public class PreferencesConfig {

    private static final Logger logger = LoggerFactory.getLogger(PreferencesConfig.class);

    private static final String CONFIG_FILE_NAME = "preferences.yml";
    private static final String EXTERNAL_CONFIG_PATH = "./preferences.yml";

    private static PreferencesConfig instance;

    /**
     * 分类权重映射 (0-5)
     */
    private final Map<String, Integer> categoryWeights = new LinkedHashMap<>();

    /**
     * 默认返回条数
     */
    private int defaultLimit = 30;

    private PreferencesConfig() {
        loadConfig();
    }

    public static synchronized PreferencesConfig getInstance() {
        if (instance == null) {
            instance = new PreferencesConfig();
        }
        return instance;
    }

    /**
     * 重新加载配置
     */
    public void reload() {
        categoryWeights.clear();
        loadConfig();
    }

    @SuppressWarnings("unchecked")
    private void loadConfig() {
        Yaml yaml = new Yaml();
        Map<String, Object> config = null;

        // 优先加载外部配置文件（用户自定义）
        Path externalPath = Paths.get(EXTERNAL_CONFIG_PATH);
        if (Files.exists(externalPath)) {
            try (InputStream is = Files.newInputStream(externalPath)) {
                config = yaml.load(is);
                logger.info("从外部文件加载用户偏好配置: {}", externalPath.toAbsolutePath());
            } catch (Exception e) {
                logger.warn("加载外部偏好配置失败: {}", e.getMessage());
            }
        }

        // 从 classpath 加载
        if (config == null) {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE_NAME)) {
                if (is != null) {
                    config = yaml.load(is);
                    logger.info("从 classpath 加载用户偏好配置");
                }
            } catch (Exception e) {
                logger.warn("加载 classpath 偏好配置失败: {}", e.getMessage());
            }
        }

        if (config != null) {
            parseConfig(config);
        } else {
            logger.warn("未找到偏好配置文件，使用默认配置");
            initDefaultConfig();
        }
    }

    @SuppressWarnings("unchecked")
    private void parseConfig(Map<String, Object> config) {
        // 解析分类权重
        Object weightsObj = config.get("category_weights");
        if (weightsObj instanceof Map) {
            Map<String, Object> weights = (Map<String, Object>) weightsObj;
            for (Map.Entry<String, Object> entry : weights.entrySet()) {
                int weight = 0;
                if (entry.getValue() instanceof Number) {
                    weight = ((Number) entry.getValue()).intValue();
                }
                // 限制在 0-5 范围
                weight = Math.max(0, Math.min(5, weight));
                categoryWeights.put(entry.getKey(), weight);
            }
        }

        // 解析默认 limit
        Object limitObj = config.get("default_limit");
        if (limitObj instanceof Number) {
            defaultLimit = ((Number) limitObj).intValue();
        }

        logger.info("加载 {} 个分类偏好配置", categoryWeights.size());
    }

    private void initDefaultConfig() {
        categoryWeights.put("ai", 5);
        categoryWeights.put("tech", 4);
        categoryWeights.put("finance", 3);
        categoryWeights.put("entertainment", 2);
        categoryWeights.put("sports", 1);
        categoryWeights.put("world", 3);
        categoryWeights.put("society", 2);
    }

    // ========== 公共 API ==========

    /**
     * 获取分类权重
     */
    public int getWeight(String categoryId) {
        return categoryWeights.getOrDefault(categoryId, 0);
    }

    /**
     * 获取所有分类权重
     */
    public Map<String, Integer> getAllWeights() {
        return Collections.unmodifiableMap(categoryWeights);
    }

    /**
     * 获取用户感兴趣的分类（权重 > 0），按权重降序
     */
    public List<String> getInterestedCategories() {
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(categoryWeights.entrySet());
        entries.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        List<String> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : entries) {
            if (entry.getValue() > 0) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    /**
     * 计算各分类应返回的新闻数量（基于权重比例）
     */
    public Map<String, Integer> calculateCategoryLimits(int totalLimit) {
        Map<String, Integer> limits = new LinkedHashMap<>();

        // 计算总权重
        int totalWeight = 0;
        for (int w : categoryWeights.values()) {
            if (w > 0)
                totalWeight += w;
        }

        if (totalWeight == 0)
            return limits;

        // 按权重比例分配
        int remaining = totalLimit;
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(categoryWeights.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        for (int i = 0; i < sorted.size(); i++) {
            Map.Entry<String, Integer> entry = sorted.get(i);
            if (entry.getValue() <= 0)
                continue;

            int count;
            if (i == sorted.size() - 1) {
                // 最后一个分类获取剩余所有
                count = remaining;
            } else {
                count = (int) Math.ceil((double) totalLimit * entry.getValue() / totalWeight);
                count = Math.min(count, remaining);
            }

            if (count > 0) {
                limits.put(entry.getKey(), count);
                remaining -= count;
            }
        }

        return limits;
    }

    public int getDefaultLimit() {
        return defaultLimit;
    }
}
