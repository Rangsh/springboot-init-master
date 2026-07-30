package com.ttk.springbootinit.ai.provider.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ttk.springbootinit.ai.config.LlmProperties;
import okhttp3.OkHttpClient;

/**
 * DeepSeek Provider（OpenAI-API 兼容，复用 OpenAiProvider 的请求/响应格式）。
 * <p>
 * API 文档：<a href="https://platform.deepseek.com/api-docs">DeepSeek API Docs</a>
 * <p>
 * DeepSeek 特有特性：
 * <ul>
 *   <li>支持 reasoning_content（deepseek-reasoner 模型的思考链）</li>
 * </ul>
 *
 * @author Rangsh
 */
public class DeepSeekProvider extends OpenAiProvider {

    public DeepSeekProvider(OkHttpClient httpClient, ObjectMapper objectMapper,
                            LlmProperties.ProviderConfig config) {
        super(httpClient, objectMapper, config);
    }

    @Override
    public String providerName() {
        return "deepseek";
    }

    @Override
    protected String getApiEndpoint() {
        String base = config.getBaseUrl();
        if (base == null || base.isBlank()) {
            base = "https://api.deepseek.com";
        }
        return base + (base.endsWith("/") ? "" : "/") + "v1/chat/completions";
    }
    // supportedModels / buildAuthHeaders 直接复用父类 OpenAiProvider 实现
}
