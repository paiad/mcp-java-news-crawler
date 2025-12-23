package com.paiad.mcp.model.vo;

import com.paiad.mcp.model.pojo.NewsItem;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 新闻简化输出 VO
 * 用于 MCP 工具返回给 LLM 的简化格式
 *
 * @author Paiad
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsItemVO {

    /**
     * 新闻标题
     */
    private String title;

    /**
     * 来源平台名称
     */
    private String platform;

    /**
     * 热度描述（如: 1054.2万）
     */
    private String hot;

    /**
     * 从 NewsItem 转换为 VO
     */
    public static NewsItemVO fromNewsItem(NewsItem item) {
        if (item == null) {
            return null;
        }

        String hot = null;
        if (item.getHotScore() != null && item.getHotScore() > 0) {
            hot = item.getFormattedHotScore();
        } else if (item.getHotDesc() != null && !item.getHotDesc().isEmpty()) {
            hot = item.getHotDesc();
        }

        return NewsItemVO.builder()
                .title(item.getTitle())
                .platform(item.getPlatformName() != null ? item.getPlatformName() : item.getPlatform())
                .hot(hot)
                .build();
    }
}
