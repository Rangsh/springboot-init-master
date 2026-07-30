package com.ttk.springbootinit.ai.provider;

import com.ttk.springbootinit.ai.model.ChatRequest;
import com.ttk.springbootinit.ai.model.ChatResponse;
import com.ttk.springbootinit.ai.model.StreamCallback;

/**
 * LLM Provider 统一接口（策略模式）。
 * <p>
 * 所有 LLM 厂商（OpenAI / Claude / DeepSeek / 本地模型）均实现此接口，
 * 调用方不感知底层 API 差异。
 *
 * @author Rangsh
 */
public interface LlmProvider {

    /**
     * Provider 标识，如 "openai" / "claude" / "deepseek" / "local"。
     */
    String providerName();

    /**
     * 同步聊天（阻塞等待完整结果）。
     *
     * @param request 规范请求
     * @return 完整响应
     */
    ChatResponse chat(ChatRequest request);

    /**
     * 流式聊天（通过回调推送增量）。
     * <p>
     * 实现必须是异步的——方法立即返回，增量通过 {@code callback} 推送。
     * 实现负责管理底层 HTTP 连接的生命周期。
     *
     * @param request  规范请求（调用方应将 {@code stream} 设为 true）
     * @param callback 流式回调
     */
    void chatStream(ChatRequest request, StreamCallback callback);

    /**
     * 该 Provider 支持的模型列表。
     */
    java.util.List<String> supportedModels();
}
