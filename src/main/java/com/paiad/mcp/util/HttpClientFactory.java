package com.paiad.mcp.util;

import okhttp3.ConnectionPool;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.concurrent.TimeUnit;

/**
 * 共享 HTTP 客户端工厂
 * 解决每个爬虫创建独立 OkHttpClient 导致的连接池资源耗尽问题
 * 
 * 支持代理配置（用于访问国际平台）：
 * - 环境变量: HTTP_PROXY 或 HTTPS_PROXY
 * - .env 文件: HTTP_PROXY=http://127.0.0.1:7890
 *
 * @author Paiad
 */
public final class HttpClientFactory {

    private static final Logger logger = LoggerFactory.getLogger(HttpClientFactory.class);

    private static volatile OkHttpClient instance;
    private static volatile OkHttpClient proxyInstance;
    private static final int DOMESTIC_TIMEOUT_SECONDS = 20;
    private static final int INTERNATIONAL_TIMEOUT_SECONDS = 35;

    private HttpClientFactory() {
    }

    /**
     * 获取共享的 OkHttpClient 实例（直连，用于国内平台）
     */
    public static OkHttpClient getInstance() {
        if (instance == null) {
            synchronized (HttpClientFactory.class) {
                if (instance == null) {
                    instance = createClient(null, DOMESTIC_TIMEOUT_SECONDS);
                    logger.info("HttpClientFactory: 创建共享 OkHttpClient 实例（直连）");
                }
            }
        }
        return instance;
    }

    /**
     * 获取带代理的 OkHttpClient 实例（用于国际平台）
     * 如果未配置代理，则返回直连实例
     */
    public static OkHttpClient getProxyInstance() {
        if (proxyInstance == null) {
            synchronized (HttpClientFactory.class) {
                if (proxyInstance == null) {
                    Proxy proxy = getConfiguredProxy();
                    if (proxy != null) {
                        proxyInstance = createClient(proxy, INTERNATIONAL_TIMEOUT_SECONDS);
                        logger.info("HttpClientFactory: 创建共享 OkHttpClient 实例（代理模式）");
                    } else {
                        // 未配置代理时，回退到直连
                        logger.warn("HttpClientFactory: 未配置代理，国际平台将使用直连（可能超时）");
                        proxyInstance = getInstance();
                    }
                }
            }
        }
        return proxyInstance;
    }

    /**
     * 从环境变量或 .env 文件读取代理配置
     * 
     * @return 代理对象，如果未配置则返回 null
     */
    private static Proxy getConfiguredProxy() {
        // 1. 优先从系统环境变量读取
        String proxyUrl = System.getenv("HTTP_PROXY");
        if (proxyUrl == null || proxyUrl.isEmpty()) {
            proxyUrl = System.getenv("HTTPS_PROXY");
        }

        // 2. 从 .env 文件读取
        if (proxyUrl == null || proxyUrl.isEmpty()) {
            proxyUrl = readProxyFromEnvFile();
        }

        // 3. 解析代理 URL
        if (proxyUrl != null && !proxyUrl.isEmpty()) {
            return parseProxyUrl(proxyUrl);
        }

        return null;
    }

    /**
     * 从 .env 文件读取代理配置
     */
    private static String readProxyFromEnvFile() {
        try {
            File envFile = new File(".env");
            if (envFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.startsWith("HTTP_PROXY=") || line.startsWith("HTTPS_PROXY=")) {
                            String value = line.split("=", 2)[1].trim();
                            if (!value.isEmpty()) {
                                return value;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("读取 .env 文件失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 解析代理 URL (支持 http://host:port 和 socks5://host:port 格式)
     */
    private static Proxy parseProxyUrl(String proxyUrl) {
        try {
            // 移除协议前缀
            String url = proxyUrl;
            Proxy.Type type = Proxy.Type.HTTP;

            if (url.startsWith("socks5://") || url.startsWith("socks://")) {
                type = Proxy.Type.SOCKS;
                url = url.replaceFirst("socks5?://", "");
            } else if (url.startsWith("http://")) {
                url = url.replace("http://", "");
            } else if (url.startsWith("https://")) {
                url = url.replace("https://", "");
            }

            // 解析 host:port
            String[] parts = url.split(":");
            if (parts.length >= 2) {
                String host = parts[0];
                int port = Integer.parseInt(parts[1].replaceAll("[^0-9]", ""));
                logger.info("HttpClientFactory: 配置代理 {}:{} ({})", host, port, type);
                return new Proxy(type, new InetSocketAddress(host, port));
            }
        } catch (Exception e) {
            logger.error("解析代理 URL 失败: {} - {}", proxyUrl, e.getMessage());
        }
        return null;
    }

    /**
     * 创建配置优化的 OkHttpClient
     * 
     * @param proxy 代理配置，null 表示直连
     */
    private static OkHttpClient createClient(Proxy proxy, int timeoutSeconds) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
                // 连接池: 最多20个空闲连接，存活5分钟
                .connectionPool(new ConnectionPool(20, 5, TimeUnit.MINUTES))
                .followRedirects(true)
                // 添加重试拦截器
                .addInterceptor(new RetryInterceptor(3));

        if (proxy != null) {
            builder.proxy(proxy);
        }

        return builder.build();
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
