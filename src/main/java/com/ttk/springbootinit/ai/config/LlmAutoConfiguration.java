package com.ttk.springbootinit.ai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ttk.springbootinit.ai.provider.LlmProvider;
import com.ttk.springbootinit.ai.provider.impl.ClaudeProvider;
import com.ttk.springbootinit.ai.provider.impl.DeepSeekProvider;
import com.ttk.springbootinit.ai.provider.impl.LocalLlmProvider;
import com.ttk.springbootinit.ai.provider.impl.OpenAiProvider;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * LLM 模块自动配置。
 * <p>
 * 条件：至少有一个 {@code llm.providers.<name>.enabled=true} 的 Provider 被启用。
 *
 * @author Rangsh
 */
@Configuration
@EnableConfigurationProperties(LlmProperties.class)
@ConditionalOnProperty(prefix = "llm", name = "enabled", havingValue = "true", matchIfMissing = false)
public class LlmAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(LlmAutoConfiguration.class);

    private final LlmProperties properties;
    private final ObjectMapper objectMapper;

    public LlmAutoConfiguration(LlmProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * 共享 OkHttpClient（所有 Provider 复用连接池）。
     */
    @Bean
    public OkHttpClient llmOkHttpClient() {
        LlmProperties.HttpClientConfig httpCfg = properties.getHttpClient();
        return new OkHttpClient.Builder()
                .connectionPool(new ConnectionPool(
                        httpCfg.getMaxIdleConnections(),
                        httpCfg.getKeepAliveDuration().toMillis(),
                        TimeUnit.MILLISECONDS))
                .retryOnConnectionFailure(true)
                .build();
    }

    /**
     * 注册所有已启用 Provider 到 Map 中，key 为 providerName。
     */
    @Bean
    public Map<String, LlmProvider> llmProviders(OkHttpClient llmOkHttpClient) {
        Map<String, LlmProvider> map = new ConcurrentHashMap<>();

        for (String name : properties.enabledProviders()) {
            LlmProperties.ProviderConfig cfg = properties.getProviderConfig(name);
            LlmProvider provider = createProvider(name, cfg, llmOkHttpClient);
            map.put(name, provider);
            log.info("LLM Provider [{}] 已注册: baseUrl={}, models={}", name, cfg.getBaseUrl(), cfg.getModels());
        }

        if (map.isEmpty()) {
            log.warn("没有启用的 LLM Provider，请在配置中设置 llm.providers.<name>.enabled=true");
        }

        return map;
    }

    /**
     * 默认 Provider Bean（按 {@code llm.default-provider} 选择）。
     */
    @Bean
    public LlmProvider defaultLlmProvider(Map<String, LlmProvider> llmProviders) {
        String defaultName = properties.getDefaultProvider();
        LlmProvider provider = llmProviders.get(defaultName);
        if (provider == null && !llmProviders.isEmpty()) {
            // fallback: 取第一个启用的
            provider = llmProviders.values().iterator().next();
            log.info("默认 Provider [{}] 未启用，回退到 [{}]", defaultName, provider.providerName());
        }
        if (provider == null) {
            throw new IllegalStateException("默认 LLM Provider 不可用，请至少启用一个 Provider");
        }
        log.info("默认 LLM Provider: {}", provider.providerName());
        return provider;
    }

    private LlmProvider createProvider(String name, LlmProperties.ProviderConfig cfg, OkHttpClient httpClient) {
        // 每个 Provider 使用独立超时的 OkHttpClient 实例
        OkHttpClient client = httpClient.newBuilder()
                .connectTimeout(cfg.getConnectTimeout())
                .readTimeout(cfg.getRequestTimeout())
                .callTimeout(cfg.getRequestTimeout())
                .build();

        return switch (name.toLowerCase()) {
            case "openai" -> new OpenAiProvider(client, objectMapper, cfg);
            case "claude" -> new ClaudeProvider(client, objectMapper, cfg);
            case "deepseek" -> new DeepSeekProvider(client, objectMapper, cfg);
            case "local" -> new LocalLlmProvider(client, objectMapper, cfg);
            default -> throw new IllegalArgumentException("不支持的 Provider 类型: " + name
                    + "（支持: openai / claude / deepseek / local）");
        };
    }
}
