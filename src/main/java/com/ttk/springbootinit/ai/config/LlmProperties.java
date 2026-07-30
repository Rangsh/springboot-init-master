package com.ttk.springbootinit.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LLM 配置属性（前缀 {@code llm}）。
 * <p>
 * 支持多 Provider 同时启用，每个 Provider 独立配置。
 *
 * <pre>{@code
 * llm:
 *   default-provider: openai
 *   providers:
 *     openai:
 *       enabled: true
 *       api-key: ${OPENAI_API_KEY}
 *       base-url: https://api.openai.com
 *       models:
 *         - gpt-4o
 *         - gpt-4o-mini
 * }</pre>
 *
 * @author Rangsh
 */
@Data
@ConfigurationProperties(prefix = "llm")
public class LlmProperties {

    /** 默认 Provider 名称 */
    private String defaultProvider = "openai";

    /** 各 Provider 配置，key 为名称（openai / claude / deepseek / local） */
    private Map<String, ProviderConfig> providers = new HashMap<>();

    /** HTTP 客户端配置 */
    private HttpClientConfig httpClient = new HttpClientConfig();

    @Data
    public static class ProviderConfig {

        /** 是否启用 */
        private boolean enabled = false;

        /** API Key */
        private String apiKey;

        /** API 基础地址 */
        private String baseUrl;

        /** 可用模型列表 */
        private List<String> models = List.of();

        /** 默认模型 */
        private String defaultModel;

        /** 请求超时（默认 60s，流式场景需要更长） */
        private Duration requestTimeout = Duration.ofSeconds(60);

        /** 连接超时 */
        private Duration connectTimeout = Duration.ofSeconds(10);

        /** 最大重试次数 */
        private int maxRetries = 1;

        /** 流式响应中是否携带 token 用量（仅 OpenAI 原生 API 支持） */
        private boolean streamIncludeUsage = true;

        /** 额外 HTTP 头 */
        private Map<String, String> headers = new HashMap<>();
    }

    @Data
    public static class HttpClientConfig {

        /** 连接池最大空闲连接数 */
        private int maxIdleConnections = 5;

        /** 空闲连接存活时间 */
        private Duration keepAliveDuration = Duration.ofMinutes(5);
    }

    /**
     * 获取指定 Provider 的配置。
     */
    public ProviderConfig getProviderConfig(String name) {
        ProviderConfig cfg = providers.get(name);
        if (cfg == null) {
            throw new IllegalArgumentException("未找到 Provider 配置: " + name);
        }
        return cfg;
    }

    /**
     * 获取已启用的 Provider 名称列表。
     */
    public List<String> enabledProviders() {
        return providers.entrySet().stream()
                .filter(e -> e.getValue().isEnabled())
                .map(Map.Entry::getKey)
                .toList();
    }
}
