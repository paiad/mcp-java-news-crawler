package com.paiad.mcp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 热门话题实体类
 * 
 * @author Paiad
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendingTopic implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 关键词
     */
    private String keyword;

    /**
     * 出现次数
     */
    private Integer count;

    /**
     * 涉及的平台列表
     */
    private List<String> platforms;

    /**
     * 趋势方向 (up/down/stable)
     */
    private String trend;

    /**
     * 趋势描述
     */
    private String trendDesc;

    /**
     * 相关新闻标题列表
     */
    private List<String> relatedTitles;

    /**
     * 获取趋势图标
     */
    public String getTrendIcon() {
        if ("up".equals(trend)) {
            return "📈";
        } else if ("down".equals(trend)) {
            return "📉";
        } else {
            return "➡️";
        }
    }
}
