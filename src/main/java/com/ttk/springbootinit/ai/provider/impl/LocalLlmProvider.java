package com.ttk.springbootinit.ai.provider.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ttk.springbootinit.ai.provider.AbstractLlmProvider;
import com.ttk.springbootinit.ai.provider.AiClientException;
import com.ttk.springbootinit.ai.config.LlmProperties;
import com.ttk.springbootinit.ai.model.ChatMessage;
import com.ttk.springbootinit.ai.model.ChatRequest;
import com.ttk.springbootinit.ai.model.ChatResponse;
import com.ttk.springbootinit.ai.model.ChatRole;
import com.ttk.springbootinit.ai.model.StreamChunk;
import com.ttk.springbootinit.ai.model.StreamCallback;
import com.ttk.springbootinit.ai.model.ToolCall;
import com.ttk.springbootinit.ai.model.Usage;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 本地 LLM Provider（兼容 Ollama API）。
 * <p>
 * API 文档：<a href="https://github.com/ollama/ollama/blob/main/docs/api.md">Ollama API</a>
 * <p>
 * 与 OpenAI 的差异：
 * <ul>
 *   <li>无鉴权（本地服务）</li>
 *   <li>流式格式为 newline-delimited JSON（无 {@code data:} 前缀）</li>
 *   <li>Tool calling 格式有细微差异</li>
 * </ul>
 *
 * @author Rangsh
 */
public class LocalLlmProvider extends AbstractLlmProvider {

    public LocalLlmProvider(OkHttpClient httpClient, ObjectMapper objectMapper,
                            LlmProperties.ProviderConfig config) {
        super(httpClient, objectMapper, config);
    }

    @Override
    public String providerName() {
        return "local";
    }

    @Override
    public List<String> supportedModels() {
        return config.getModels();
    }

    @Override
    protected String getApiEndpoint() {
        String base = config.getBaseUrl();
        if (base == null || base.isBlank()) {
            base = "http://localhost:11434";
        }
        return base + (base.endsWith("/") ? "" : "/") + "api/chat";
    }

    @Override
    protected void buildAuthHeaders(Request.Builder builder) {
        // 本地 Ollama 默认无需鉴权
        if (config.getApiKey() != null && !config.getApiKey().isBlank()) {
            builder.header("Authorization", "Bearer " + config.getApiKey());
        }
        if (config.getHeaders() != null) {
            config.getHeaders().forEach(builder::header);
        }
    }

    @Override
    protected boolean usesDoneMarker() {
        return false; // Ollama 使用 done: true 标记
    }

    // ==================== 请求体构建 ====================

    @Override
    protected String buildRequestBody(ChatRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", request.getModel());

        List<Map<String, Object>> messages = new ArrayList<>();
        for (ChatMessage msg : request.getMessages()) {
            messages.add(buildMessageNode(msg));
        }
        body.put("messages", messages);

        if (request.getTemperature() != null) {
            body.put("temperature", request.getTemperature());
        }
        if (request.getMaxTokens() != null) {
            // Ollama 用 num_predict 控制输出长度
            body.put("options", Map.of("num_predict", request.getMaxTokens()));
        }
        if (request.getTopP() != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> opts = new HashMap<>((Map<String, Object>) body.getOrDefault("options", Map.of()));
            opts.put("top_p", request.getTopP());
            body.put("options", opts);
        }
        if (request.getStream() != null && request.getStream()) {
            body.put("stream", true);
        }
        if (request.getTools() != null && !request.getTools().isEmpty()) {
            body.put("tools", request.getTools());
        }

        return toJson(body);
    }

    private Map<String, Object> buildMessageNode(ChatMessage msg) {
        Map<String, Object> node = new HashMap<>();
        node.put("role", msg.getRole().value());
        if (msg.getContent() != null) {
            node.put("content", msg.getContent());
        }
        // Ollama tool_calls in assistant messages
        if (msg.getToolCalls() != null && !msg.getToolCalls().isEmpty()) {
            node.put("tool_calls", msg.getToolCalls());
        }
        if (msg.getToolCallId() != null) {
            node.put("tool_call_id", msg.getToolCallId());
        }
        return node;
    }

    // ==================== 同步响应解析 ====================

    @Override
    protected ChatResponse parseResponseBody(String body, ChatRequest request) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode messageNode = root.path("message");

            String content = pathText(messageNode, "content");
            List<ToolCall> toolCalls = parseToolCalls(messageNode.path("tool_calls"));

            ChatMessage.ChatMessageBuilder msgBuilder = ChatMessage.builder()
                    .role(ChatRole.ASSISTANT)
                    .content(content);
            if (toolCalls != null && !toolCalls.isEmpty()) {
                msgBuilder.toolCalls(toolCalls);
            }

            return ChatResponse.builder()
                    .model(pathText(root, "model"))
                    .choices(List.of(msgBuilder.build()))
                    .usage(parseUsage(root))
                    .finishReason(pathText(root, "done_reason"))
                    .provider(providerName())
                    .build();
        } catch (JsonProcessingException e) {
            throw new AiClientException("Ollama 响应解析失败", e);
        }
    }

    // ==================== 流式 SSE 解析（覆盖：Ollama 是 JSON Lines，非 SSE data: 格式） ====================

    @Override
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
                if (line.isBlank()) {
                    continue;
                }
                try {
                    JsonNode root = objectMapper.readTree(line);
                    // Ollama 的 done: true 标记流结束
                    if (root.path("done").asBoolean(false)) {
                        break;
                    }
                    JsonNode messageNode = root.path("message");
                    String content = pathText(messageNode, "content");
                    if (content != null && !content.isEmpty()) {
                        fullContent.append(content);
                        callback.onChunk(StreamChunk.text(content));
                    }
                } catch (JsonProcessingException e) {
                    log.debug("Ollama 流式行解析跳过: {}", line);
                }
            }

            ChatResponse response = ChatResponse.builder()
                    .provider(providerName())
                    .model(request.getModel())
                    .choices(List.of(ChatMessage.assistant(fullContent.toString())))
                    .build();
            callback.onComplete(response);

        } catch (IOException e) {
            callback.onError(new AiClientException(providerName() + " 流读取失败: " + e.getMessage(), e));
        }
    }

    @Override
    protected StreamChunk parseStreamDataLine(String data, ChatRequest request) {
        // 不使用——已在 parseStreamResponse 中覆盖
        return null;
    }

    // ==================== 工具方法 ====================

    private List<ToolCall> parseToolCalls(JsonNode tcNode) {
        if (!tcNode.isArray() || tcNode.isEmpty()) {
            return null;
        }
        List<ToolCall> toolCalls = new ArrayList<>();
        for (JsonNode tc : tcNode) {
            JsonNode func = tc.path("function");
            toolCalls.add(ToolCall.builder()
                    .id(pathText(tc, "id"))
                    .type("function")
                    .function(ToolCall.FunctionCall.builder()
                            .name(pathText(func, "name"))
                            .arguments(pathText(func, "arguments"))
                            .build())
                    .build());
        }
        return toolCalls;
    }

    private Usage parseUsage(JsonNode root) {
        int promptEval = root.path("prompt_eval_count").asInt();
        int evalCount = root.path("eval_count").asInt();
        if (promptEval == 0 && evalCount == 0) {
            return null;
        }
        return Usage.builder()
                .promptTokens(promptEval)
                .completionTokens(evalCount)
                .totalTokens(promptEval + evalCount)
                .build();
    }

    private static String pathText(JsonNode node, String field) {
        JsonNode child = node.path(field);
        return child.isMissingNode() || child.isNull() ? null : child.asText();
    }
}
