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
 * 新闻分类配置加载器
 * 从 categories.yml 读取分类配置
 *
 * @author Paiad
 */
public class CategoryConfig {

    private static final Logger logger = LoggerFactory.getLogger(CategoryConfig.class);

    private static final String CONFIG_FILE_NAME = "categories.yml";
    private static final String EXTERNAL_CONFIG_PATH = "./categories.yml";

    private static CategoryConfig instance;

    /**
     * 分类信息映射
     */
    private final Map<String, CategoryInfo> categoryMap = new LinkedHashMap<>();

    private CategoryConfig() {
        loadConfig();
    }

    public static synchronized CategoryConfig getInstance() {
        if (instance == null) {
            instance = new CategoryConfig();
        }
        return instance;
    }

    @SuppressWarnings("unchecked")
    private void loadConfig() {
        Yaml yaml = new Yaml();
        Map<String, Object> config = null;

        // 优先加载外部配置文件
        Path externalPath = Paths.get(EXTERNAL_CONFIG_PATH);
        if (Files.exists(externalPath)) {
            try (InputStream is = Files.newInputStream(externalPath)) {
                config = yaml.load(is);
                logger.info("从外部文件加载分类配置: {}", externalPath.toAbsolutePath());
            } catch (Exception e) {
                logger.warn("加载外部分类配置失败: {}", e.getMessage());
            }
        }

        // 从 classpath 加载
        if (config == null) {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE_NAME)) {
                if (is != null) {
                    config = yaml.load(is);
                    logger.info("从 classpath 加载分类配置");
                }
            } catch (Exception e) {
                logger.warn("加载 classpath 分类配置失败: {}", e.getMessage());
            }
        }

        if (config != null) {
            parseConfig(config);
        } else {
            logger.warn("未找到分类配置文件，使用默认配置");
            initDefaultConfig();
        }
    }

    @SuppressWarnings("unchecked")
    private void parseConfig(Map<String, Object> config) {
        Map<String, Object> categories = (Map<String, Object>) config.get("categories");
        if (categories != null) {
            for (Map.Entry<String, Object> entry : categories.entrySet()) {
                String categoryId = entry.getKey();
                Map<String, Object> categoryData = (Map<String, Object>) entry.getValue();

                String name = (String) categoryData.getOrDefault("name", categoryId);

                List<String> keywords = new ArrayList<>();
                Object keywordsObj = categoryData.get("keywords");
                if (keywordsObj instanceof List) {
                    for (Object k : (List<?>) keywordsObj) {
                        keywords.add(k.toString().toLowerCase());
                    }
                }

                Map<String, Integer> platformWeights = new LinkedHashMap<>();
                Object platformsObj = categoryData.get("platforms");
                if (platformsObj instanceof Map) {
                    Map<String, Object> platforms = (Map<String, Object>) platformsObj;
                    for (Map.Entry<String, Object> pe : platforms.entrySet()) {
                        int weight = 0;
                        if (pe.getValue() instanceof Number) {
                            weight = ((Number) pe.getValue()).intValue();
                        }
                        platformWeights.put(pe.getKey(), Math.min(5, Math.max(0, weight)));
                    }
                }

                categoryMap.put(categoryId, new CategoryInfo(categoryId, name, keywords, platformWeights));
            }
        }
        logger.info("加载 {} 个分类配置", categoryMap.size());
    }

    private void initDefaultConfig() {
        // 默认分类
        categoryMap.put("tech", new CategoryInfo("tech", "科技",
                Arrays.asList("科技", "互联网", "ai", "人工智能"),
                Map.of("techcrunch", 5, "zhihu", 4, "bilibili", 3)));
        categoryMap.put("finance", new CategoryInfo("finance", "财经",
                Arrays.asList("股票", "基金", "经济", "金融"),
                Map.of("wallstreetcn", 5, "toutiao", 3)));
        categoryMap.put("entertainment", new CategoryInfo("entertainment", "娱乐",
                Arrays.asList("明星", "电影", "综艺"),
                Map.of("weibo", 5, "douyin", 5)));
    }

    // ========== 公共 API ==========

    /**
     * 获取所有分类
     */
    public Collection<CategoryInfo> getAllCategories() {
        return categoryMap.values();
    }

    /**
     * 获取分类信息
     */
    public CategoryInfo getCategory(String categoryId) {
        return categoryMap.get(categoryId);
    }

    /**
     * 获取所有分类ID
     */
    public Set<String> getCategoryIds() {
        return categoryMap.keySet();
    }

    /**
     * 根据分类获取推荐平台（按权重排序）
     */
    public List<String> getRecommendedPlatforms(String categoryId) {
        CategoryInfo info = categoryMap.get(categoryId);
        if (info == null) {
            return Collections.emptyList();
        }
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(info.getPlatformWeights().entrySet());
        entries.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, Integer> e : entries) {
            result.add(e.getKey());
        }
        return result;
    }

    // ========== 内部类 ==========

    /**
     * 分类信息
     */
    public static class CategoryInfo {
        private final String id;
        private final String name;
        private final List<String> keywords;
        private final Map<String, Integer> platformWeights;

        public CategoryInfo(String id, String name, List<String> keywords, Map<String, Integer> platformWeights) {
            this.id = id;
            this.name = name;
            this.keywords = keywords;
            this.platformWeights = platformWeights;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public List<String> getKeywords() {
            return keywords;
        }

        public Map<String, Integer> getPlatformWeights() {
            return platformWeights;
        }

        public int getWeight(String platformId) {
            return platformWeights.getOrDefault(platformId, 0);
        }

        @Override
        public String toString() {
            return String.format("CategoryInfo{id='%s', name='%s', keywords=%d, platforms=%d}",
                    id, name, keywords.size(), platformWeights.size());
        }
    }
}
