package com.paiad.mcp.config;

import lombok.Data;
import com.paiad.mcp.registry.PlatformDescriptor;
import com.paiad.mcp.registry.PlatformRegistry;

import java.util.Set;

/**
 * 平台配置类
 * 兼容层：内部委托给 PlatformRegistry
 *
 * @author Paiad
 */
@Data
public class PlatformConfig {

    private static final PlatformRegistry REGISTRY = PlatformRegistry.getInstance();

    // ========== 平台信息 API ==========

    /**
     * 获取平台信息
     */
    public static PlatformInfo getPlatform(String platformId) {
        PlatformDescriptor descriptor = REGISTRY.getById(platformId).orElse(null);
        if (descriptor == null) {
            return null;
        }
        return new PlatformInfo(descriptor.id(), descriptor.name(), descriptor.url());
    }

    /**
     * 获取所有平台 ID
     */
    public static Set<String> getAllPlatformIds() {
        return REGISTRY.getAllPlatformIds();
    }

    // ========== 别名解析 API ==========

    /**
     * 将输入字符串（别名或 ID）解析为官方平台 ID
     * 若未匹配到则返回 null
     */
    public static String resolveId(String input) {
        return REGISTRY.resolveId(input);
    }

    /**
     * 获取平台显示名称
     */
    public static String getName(String id) {
        return REGISTRY.getName(id);
    }

    // ========== 内部类 ==========

    /**
     * 平台信息
     */
    @Data
    public static class PlatformInfo {
        private final String id;
        private final String name;
        private final String url;

        public PlatformInfo(String id, String name, String url) {
            this.id = id;
            this.name = name;
            this.url = url;
        }
    }
}
