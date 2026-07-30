package com.ttk.springbootinit.ai.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ttk.springbootinit.ai.config.LlmProperties;
import com.ttk.springbootinit.ai.model.*;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * LLM Provider 抽象基类（模板方法模式）。
 * <p>
 * 提供同步/流式调用的骨架流程，子类只需实现 Provider 特定的：
 * <ul>
 *   <li>{@code buildRequestBody} — 将规范请求翻译为 Provider API 格式</li>
 *   <li>{@code parseResponseBody} — 将 Provider 响应翻译为规范响应</li>
 *   <li>{@code parseStreamLine} — 解析一行 SSE 数据为 StreamChunk</li>
 *   <li>{@code getApiEndpoint} — API 地址</li>
 *   <li>{@code buildAuthHeaders} — 鉴权头</li>
 * </ul>
 *
 * @author Rangsh
 */
public abstract class AbstractLlmProvider implements LlmProvider {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    protected final OkHttpClient httpClient;
    protected final ObjectMapper objectMapper;
    protected final LlmProperties.ProviderConfig config;

    protected AbstractLlmProvider(OkHttpClient httpClient,
                                  ObjectMapper objectMapper,
                                  LlmProperties.ProviderConfig config) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.config = config;
    }

    // ==================== 模板方法 ====================

    @Override
    public ChatResponse chat(ChatRequest request) {
        validateRequest(request);
        String requestBody = buildRequestBody(withStream(request, false));
        log.debug("[{}] 请求体: {}", providerName(), requestBody);

        Request httpRequest = buildHttpRequest(requestBody);
        try (Response httpResponse = executeHttpRequest(httpRequest)) {
            return parseHttpResponse(httpResponse, request);
        } catch (IOException e) {
            throw new AiClientException(providerName() + " 请求失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void chatStream(ChatRequest request, StreamCallback callback) {
        validateRequest(request);
        String requestBody = buildRequestBody(withStream(request, true));
        log.debug("[{}] 流式请求体: {}", providerName(), requestBody);

        Request httpRequest = buildHttpRequest(requestBody);
        httpClient.newCall(httpRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(new AiClientException(providerName() + " 流式请求失败: " + e.getMessage(), e));
            }

            @Override
            public void onResponse(Call call, Response httpResponse) {
                try (httpResponse) {
                    parseStreamResponse(httpResponse, callback, request);
                } catch (Exception e) {
                    callback.onError(new AiClientException(providerName() + " 流式解析失败: " + e.getMessage(), e));
                }
            }
        });
    }

    // ==================== 子类必须实现 ====================

    /**
     * 将规范请求转为 Provider 原生 JSON 请求体。
     */
    protected abstract String buildRequestBody(ChatRequest request);

    /**
     * 解析 Provider 同步响应为规范 ChatResponse。
     */
    protected abstract ChatResponse parseResponseBody(String body, ChatRequest request);

    /**
     * 解析一行 SSE 数据（去掉 "data:" 前缀后的 JSON）。
     *
     * @param line    已去掉 "data: " 前缀的 JSON 行
     * @param request 原始请求（用于元数据）
     * @return StreamChunk，若该行不包含有效数据则返回 null
     */
    protected abstract StreamChunk parseStreamDataLine(String line, ChatRequest request);

    /**
     * API 端点地址。
     */
    protected abstract String getApiEndpoint();

    /**
     * 构建鉴权请求头。
     */
    protected abstract void buildAuthHeaders(Request.Builder builder);

    // ==================== 可选覆盖 ====================

    /**
     * 是否使用 [DONE] 标记流结束。
     */
    protected boolean usesDoneMarker() {
        return true;
    }

    // ==================== 内部辅助 ====================

    protected void validateRequest(ChatRequest request) {
        if (request.getModel() == null || request.getModel().isBlank()) {
            throw new AiClientException("model 不能为空");
        }
        if (request.getMessages() == null || request.getMessages().isEmpty()) {
            throw new AiClientException("messages 不能为空");
        }
    }

    protected Request buildHttpRequest(String body) {
        Request.Builder builder = new Request.Builder()
                .url(getApiEndpoint())
                .post(RequestBody.create(body, JSON));
        buildAuthHeaders(builder);
        applyConfig(builder);
        return builder.build();
    }

    /**
     * 应用超时等配置。
     */
    protected void applyConfig(Request.Builder builder) {
        // OkHttp 超时在 Client 级别配置；此处可做 per-request 调整
    }

    protected Response executeHttpRequest(Request request) throws IOException {
        return httpClient.newCall(request).execute();
    }

    protected ChatResponse parseHttpResponse(Response httpResponse, ChatRequest request) throws IOException {
        if (!httpResponse.isSuccessful()) {
            String errorBody = httpResponse.body() != null ? httpResponse.body().string() : "";
            throw new AiClientException(
                    providerName() + " 返回 HTTP " + httpResponse.code() + ": " + errorBody);
        }
        String body = httpResponse.body() != null ? httpResponse.body().string() : "";
        return parseResponseBody(body, request);
    }

    /**
     * 流式 SSE 解析——模板方法，子类通常无需覆盖。
     */
    protected void parseStreamResponse(Response httpResponse, StreamCallback callback, ChatRequest request) {
        if (!httpResponse.isSuccessful()) {
            String errorBody;
            try {
                errorBody = httpResponse.body() != null ? httpResponse.body().string() : "";
            } catch (IOException e) {
                errorBody = "无法读取错误响应体";
            }
            callback.onError(new AiClientException(
                    providerName() + " 流式请求返回 HTTP " + httpResponse.code() + ": " + errorBody));
            return;
        }

        ResponseBody body = httpResponse.body();
        if (body == null) {
            callback.onError(new AiClientException(providerName() + " 流式响应体为空"));
            return;
        }
        StringBuilder fullContent = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(body.byteStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }

                // SSE 标准格式: "data: <json>"
                if (line.startsWith("data: ")) {
                    String data = line.substring(6);
                    if (usesDoneMarker() && "[DONE]".equals(data.trim())) {
                        break;
                    }
                    StreamChunk chunk = parseStreamDataLine(data, request);
                    if (chunk != null) {
                        if (chunk.getContent() != null) {
                            fullContent.append(chunk.getContent());
                        }
                        callback.onChunk(chunk);
                    }
                }
            }

            ChatResponse response = ChatResponse.builder()
                    .provider(providerName())
                    .model(request.getModel())
                    .choices(List.of(ChatMessage.assistant(fullContent.toString())))
                    .build();
            callback.onComplete(response);

        } catch (IOException e) {
            callback.onError(new AiClientException(providerName() + " SSE 流读取失败: " + e.getMessage(), e));
        }
    }

    protected ChatRequest withStream(ChatRequest request, boolean stream) {
        if (request.getStream() == stream) {
            return request;
        }
        return ChatRequest.builder()
                .model(request.getModel())
                .messages(request.getMessages())
                .temperature(request.getTemperature())
                .maxTokens(request.getMaxTokens())
                .topP(request.getTopP())
                .stream(stream)
                .tools(request.getTools())
                .toolChoice(request.getToolChoice())
                .stop(request.getStop())
                .extraParams(request.getExtraParams())
                .build();
    }

    /**
     * JSON 序列化辅助。
     */
    protected String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new AiClientException("JSON 序列化失败", e);
        }
    }
}
