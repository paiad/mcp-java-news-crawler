package com.paiad.mcp.crawler;

import com.paiad.mcp.model.NewsItem;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 抽象爬虫基类
 * 
 * @author Paiad
 */
public abstract class AbstractCrawler {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * HTTP 客户端
     */
    protected final OkHttpClient httpClient;

    /**
     * 平台ID
     */
    protected final String platformId;

    /**
     * 平台名称
     */
    protected final String platformName;

    public AbstractCrawler(String platformId, String platformName) {
        this.platformId = platformId;
        this.platformName = platformName;
        this.httpClient = createHttpClient();
    }

    /**
     * 创建 HTTP 客户端
     */
    private OkHttpClient createHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .followRedirects(true)
                .build();
    }

    /**
     * 爬取热榜数据
     */
    public abstract List<NewsItem> crawl();

    /**
     * 发送 GET 请求
     */
    protected String doGet(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Connection", "keep-alive")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: " + response.code());
            }
            return response.body() != null ? response.body().string() : "";
        }
    }

    /**
     * 发送带自定义 Headers 的 GET 请求
     */
    protected String doGet(String url, java.util.Map<String, String> headers) throws IOException {
        Request.Builder builder = new Request.Builder()
                .url(url)
                .header("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        if (headers != null) {
            for (java.util.Map.Entry<String, String> entry : headers.entrySet()) {
                builder.header(entry.getKey(), entry.getValue());
            }
        }

        Request request = builder.build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: " + response.code());
            }
            return response.body() != null ? response.body().string() : "";
        }
    }

    /**
     * 安全执行爬取，捕获异常
     */
    public List<NewsItem> safeCrawl() {
        try {
            List<NewsItem> items = crawl();
            logger.info("[{}] 成功爬取 {} 条热榜数据", platformName, items.size());
            return items;
        } catch (Exception e) {
            logger.error("[{}] 爬取失败: {}", platformName, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public String getPlatformId() {
        return platformId;
    }

    public String getPlatformName() {
        return platformName;
    }
}
