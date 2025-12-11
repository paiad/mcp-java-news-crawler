package com.paiad.mcp.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * JSON 工具类 - 提供共享的 ObjectMapper 实例
 * 解决每个爬虫创建独立 ObjectMapper 导致的资源浪费问题
 *
 * @author Paiad
 */
public final class JsonUtils {

    private static final ObjectMapper INSTANCE = createObjectMapper();

    private JsonUtils() {
    }

    /**
     * 获取共享的 ObjectMapper 实例
     */
    public static ObjectMapper getMapper() {
        return INSTANCE;
    }

    /**
     * 解析 JSON 字符串为 JsonNode
     */
    public static JsonNode parse(String json) throws IOException {
        return INSTANCE.readTree(json);
    }

    /**
     * 将对象转换为 JSON 字符串
     */
    public static String toJson(Object obj) throws IOException {
        return INSTANCE.writeValueAsString(obj);
    }

    /**
     * 将对象转换为格式化的 JSON 字符串
     */
    public static String toPrettyJson(Object obj) throws IOException {
        return INSTANCE.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
    }

    /**
     * 创建配置优化的 ObjectMapper
     */
    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // 忽略未知属性，提高容错性
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }
}
