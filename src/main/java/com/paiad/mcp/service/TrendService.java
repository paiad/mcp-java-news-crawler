package com.paiad.mcp.service;

import com.paiad.mcp.model.NewsItem;
import com.paiad.mcp.model.TrendingTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 趋势分析服务
 * 
 * @author Paiad
 */
public class TrendService {

    private static final Logger logger = LoggerFactory.getLogger(TrendService.class);

    private final NewsService newsService;

    public TrendService(NewsService newsService) {
        this.newsService = newsService;
    }

    /**
     * 获取热门话题
     *
     * @param topN 返回 TOP N 话题
     * @return 热门话题列表
     */
    public List<TrendingTopic> getTrendingTopics(int topN) {
        // 获取所有热点新闻
        List<NewsItem> allNews = newsService.getHotNews(null, 200, false);

        if (allNews.isEmpty()) {
            return Collections.emptyList();
        }

        // 提取关键词并统计
        Map<String, KeywordStats> keywordStatsMap = new HashMap<>();

        for (NewsItem news : allNews) {
            List<String> keywords = extractKeywords(news.getTitle());
            for (String keyword : keywords) {
                KeywordStats stats = keywordStatsMap.computeIfAbsent(keyword, k -> new KeywordStats());
                stats.count++;
                stats.platforms.add(news.getPlatform());
                stats.titles.add(news.getTitle());
            }
        }

        // 过滤并排序
        List<TrendingTopic> topics = keywordStatsMap.entrySet().stream()
                .filter(e -> e.getValue().count >= 2) // 至少出现2次
                .filter(e -> e.getKey().length() >= 2) // 关键词至少2个字符
                .sorted((a, b) -> Integer.compare(b.getValue().count, a.getValue().count))
                .limit(topN > 0 ? topN : 10)
                .map(e -> {
                    KeywordStats stats = e.getValue();
                    return TrendingTopic.builder()
                            .keyword(e.getKey())
                            .count(stats.count)
                            .platforms(new ArrayList<>(stats.platforms))
                            .trend(determineTrend(stats.count))
                            .trendDesc(getTrendDesc(stats.count))
                            .relatedTitles(stats.titles.stream().limit(3).collect(Collectors.toList()))
                            .build();
                })
                .collect(Collectors.toList());

        logger.info("分析出 {} 个热门话题", topics.size());
        return topics;
    }

    /**
     * 提取关键词
     */
    private List<String> extractKeywords(String title) {
        if (title == null || title.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> keywords = new ArrayList<>();

        // 提取中文词组（2-6个汉字）
        Pattern chinesePattern = Pattern.compile("[\\u4e00-\\u9fa5]{2,6}");
        Matcher chineseMatcher = chinesePattern.matcher(title);
        while (chineseMatcher.find()) {
            String word = chineseMatcher.group();
            // 过滤常见停用词
            if (!isStopWord(word)) {
                keywords.add(word);
            }
        }

        // 提取英文单词（至少2个字母）
        Pattern englishPattern = Pattern.compile("[a-zA-Z]{2,}");
        Matcher englishMatcher = englishPattern.matcher(title);
        while (englishMatcher.find()) {
            String word = englishMatcher.group().toUpperCase();
            keywords.add(word);
        }

        return keywords;
    }

    /**
     * 判断是否为停用词
     */
    private boolean isStopWord(String word) {
        Set<String> stopWords = new HashSet<>(Arrays.asList(
                "什么", "怎么", "如何", "为什么", "哪里", "谁是", "什么样",
                "可以", "能够", "应该", "需要", "可能", "一定",
                "这个", "那个", "这些", "那些", "这里", "那里",
                "突发", "速看", "最新", "热搜", "刚刚", "重磅",
                "如何", "怎样", "为何", "何时", "哪些", "哪个"));
        return stopWords.contains(word);
    }

    /**
     * 确定趋势方向
     */
    private String determineTrend(int count) {
        if (count >= 5) {
            return "up";
        } else if (count >= 3) {
            return "stable";
        } else {
            return "down";
        }
    }

    /**
     * 获取趋势描述
     */
    private String getTrendDesc(int count) {
        if (count >= 5) {
            return "热度上升，多平台关注";
        } else if (count >= 3) {
            return "热度平稳，持续关注";
        } else {
            return "热度一般";
        }
    }

    /**
     * 关键词统计内部类
     */
    private static class KeywordStats {
        int count = 0;
        Set<String> platforms = new HashSet<>();
        List<String> titles = new ArrayList<>();
    }
}
