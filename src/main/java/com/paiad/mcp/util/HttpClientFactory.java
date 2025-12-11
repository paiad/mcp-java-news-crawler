package com.paiad.mcp.util;

import okhttp3.ConnectionPool;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 共享 HTTP 客户端工厂
 * 解决每个爬虫创建独立 OkHttpClient 导致的连接池资源耗尽问题
 *
 * @author Paiad
 */
public final class HttpClientFactory {

    private static final Logger logger = LoggerFactory.getLogger(HttpClientFactory.class);

    private static volatile OkHttpClient instance;

    private HttpClientFactory() {
    }

    /**
     * 获取共享的 OkHttpClient 实例
     */
    public static OkHttpClient getInstance() {
        if (instance == null) {
            synchronized (HttpClientFactory.class) {
                if (instance == null) {
                    instance = createClient();
                    logger.info("HttpClientFactory: 创建共享 OkHttpClient 实例");
                }
            }
        }
        return instance;
    }

    /**
     * 创建配置优化的 OkHttpClient
     */
    private static OkHttpClient createClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                // 连接池: 最多20个空闲连接，存活5分钟
                .connectionPool(new ConnectionPool(20, 5, TimeUnit.MINUTES))
                .followRedirects(true)
                // 添加重试拦截器
                .addInterceptor(new RetryInterceptor(3))
                .build();
    }

    /**
     * 重试拦截器 - 仅在网络错误时重试，不对 HTTP 错误响应重试
     * 
     * 注意：HTTP 错误响应（如 403, 429）不应重试，因为：
     * 1. 重试会加剧反爬虫检测
     * 2. 响应体中可能包含有用的错误信息
     */
    private static class RetryInterceptor implements Interceptor {
        private final int maxRetries;

        RetryInterceptor(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            IOException lastException = null;

            for (int attempt = 0; attempt <= maxRetries; attempt++) {
                try {
                    Response response = chain.proceed(request);
                    // 无论成功还是失败，都直接返回响应，让调用方处理
                    // 不要在这里关闭或重试 HTTP 错误响应
                    return response;
                } catch (IOException e) {
                    // 仅在网络层面失败时重试
                    lastException = e;
                    if (attempt < maxRetries) {
                        // 指数退避: 100ms, 200ms, 400ms...
                        long waitTime = (long) Math.pow(2, attempt) * 100;
                        logger.warn("网络请求失败: {}, 等待 {}ms 后重试 ({}/{})",
                                e.getMessage(), waitTime, attempt + 1, maxRetries);
                        try {
                            Thread.sleep(waitTime);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw e;
                        }
                    }
                }
            }
            throw lastException != null ? lastException
                    : new IOException("Request failed after " + maxRetries + " retries");
        }
    }

    /**
     * 关闭 HttpClient（用于应用关闭时清理资源）
     */
    public static void shutdown() {
        if (instance != null) {
            instance.dispatcher().executorService().shutdown();
            instance.connectionPool().evictAll();
            logger.info("HttpClientFactory: 已关闭共享 OkHttpClient");
        }
    }
}
