package com.paiad.mcp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 新闻实体类
 * 
 * @author Paiad
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 唯一标识
     */
    private String id;

    /**
     * 新闻标题
     */
    private String title;

    /**
     * 新闻链接
     */
    private String url;

    /**
     * 来源平台 (zhihu/weibo/bilibili/baidu/douyin)
     */
    private String platform;

    /**
     * 平台中文名
     */
    private String platformName;

    /**
     * 排名
     */
    private Integer rank;

    /**
     * 热度值
     */
    private Long hotScore;

    /**
     * 热度描述 (如: 1234万热度)
     */
    private String hotDesc;

    /**
     * 标签 (如: 热/新/沸 等)
     */
    private String tag;

    /**
     * 抓取时间戳
     */
    private Long timestamp;

    /**
     * 获取格式化的热度描述
     */
    public String getFormattedHotScore() {
        if (hotScore == null) {
            return hotDesc != null ? hotDesc : "";
        }
        if (hotScore >= 100000000) {
            return String.format("%.1f亿", hotScore / 100000000.0);
        } else if (hotScore >= 10000) {
            return String.format("%.1f万", hotScore / 10000.0);
        } else {
            return String.valueOf(hotScore);
        }
    }
}
