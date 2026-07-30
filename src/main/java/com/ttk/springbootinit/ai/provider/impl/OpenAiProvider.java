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
import com.ttk.springbootinit.ai.model.ToolCall;
import com.ttk.springbootinit.ai.model.Usage;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI Provider（兼容 DeepSeek 等 OpenAI-API 兼容服务）。
 * <p>
 * API 文档：<a href="https://platform.openai.com/docs/api-reference/chat">OpenAI Chat API</a>
 *
 * @author Rangsh
 */
public class OpenAiProvider extends AbstractLlmProvider {

    public OpenAiProvider(OkHttpClient httpClient, ObjectMapper objectMapper,
                          LlmProperties.ProviderConfig config) {
        super(httpClient, objectMapper, config);
    }

    @Override
    public String providerName() {
        return "openai";
    }

    @Override
    public List<String> supportedModels() {
        return config.getModels();
    }

    @Override
    protected String getApiEndpoint() {
        String base = config.getBaseUrl();
        if (base == null || base.isBlank()) {
            base = "https://api.openai.com";
        }
        return base + (base.endsWith("/") ? "" : "/") + "v1/chat/completions";
    }

    @Override
    protected void buildAuthHeaders(Request.Builder builder) {
        builder.header("Authorization", "Bearer " + config.getApiKey());
        // 透传额外头
        if (config.getHeaders() != null) {
            config.getHeaders().forEach(builder::header);
        }
    }

    // ==================== 请求体构建 ====================

    @Override
    protected String buildRequestBody(ChatRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", request.getModel());

        // 消息列表
        List<Map<String, Object>> messages = new ArrayList<>();
        for (ChatMessage msg : request.getMessages()) {
            messages.add(buildMessageNode(msg));
        }
        body.put("messages", messages);

        if (request.getTemperature() != null) {
            body.put("temperature", request.getTemperature());
        }
        if (request.getMaxTokens() != null) {
            body.put("max_tokens", request.getMaxTokens());
        }
        if (request.getTopP() != null) {
            body.put("top_p", request.getTopP());
        }
        if (request.getStream() != null && request.getStream()) {
            body.put("stream", true);
            if (config.isStreamIncludeUsage()) {
                body.put("stream_options", Map.of("include_usage", true));
            }
        }
        if (request.getTools() != null && !request.getTools().isEmpty()) {
            body.put("tools", request.getTools());
        }
        if (request.getToolChoice() != null) {
            body.put("tool_choice", request.getToolChoice());
        }
        if (request.getStop() != null && !request.getStop().isEmpty()) {
            body.put("stop", request.getStop());
        }
        if (request.getExtraParams() != null) {
            body.putAll(request.getExtraParams());
        }

        return toJson(body);
    }

    private Map<String, Object> buildMessageNode(ChatMessage msg) {
        Map<String, Object> node = new HashMap<>();
        node.put("role", msg.getRole().value());

        if (msg.getContent() != null) {
            node.put("content", msg.getContent());
        }

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
            String id = pathText(root, "id");
            String model = pathText(root, "model");

            List<ChatMessage> choices = new ArrayList<>();
            JsonNode choicesNode = root.path("choices");
            String finishReason = null;
            for (JsonNode choice : choicesNode) {
                JsonNode messageNode = choice.path("message");
                ChatMessage msg = parseAssistantMessage(messageNode);
                choices.add(msg);
                if (finishReason == null) {
                    finishReason = pathText(choice, "finish_reason");
                }
            }

            Usage usage = parseUsage(root.path("usage"));

            return ChatResponse.builder()
                    .id(id)
                    .model(model)
                    .choices(choices)
                    .usage(usage)
                    .finishReason(finishReason)
                    .provider(providerName())
                    .build();
        } catch (JsonProcessingException e) {
            throw new AiClientException("OpenAI 响应解析失败", e);
        }
    }

    private ChatMessage parseAssistantMessage(JsonNode messageNode) {
        if (messageNode.isMissingNode()) {
            return ChatMessage.assistant(null);
        }

        String content = pathText(messageNode, "content");
        List<ToolCall> toolCalls = null;
        JsonNode tcNode = messageNode.path("tool_calls");
        if (tcNode.isArray() && tcNode.size() > 0) {
            toolCalls = new ArrayList<>();
            for (JsonNode tc : tcNode) {
                toolCalls.add(ToolCall.builder()
                        .id(pathText(tc, "id"))
                        .type(pathText(tc, "type"))
                        .function(ToolCall.FunctionCall.builder()
                                .name(pathText(tc.path("function"), "name"))
                                .arguments(pathText(tc.path("function"), "arguments"))
                                .build())
                        .build());
            }
        }

        ChatMessage.ChatMessageBuilder builder = ChatMessage.builder()
                .role(ChatRole.ASSISTANT)
                .content(content);
        if (toolCalls != null) {
            builder.toolCalls(toolCalls);
        }
        return builder.build();
    }

    // ==================== 流式解析 ====================

    @Override
    protected StreamChunk parseStreamDataLine(String data, ChatRequest request) {
        try {
            JsonNode root = objectMapper.readTree(data);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                return null;
            }
            JsonNode choice = choices.get(0);
            JsonNode delta = choice.path("delta");
            if (delta.isMissingNode()) {
                return null;
            }

            String content = pathText(delta, "content");
            String finishReason = pathText(choice, "finish_reason");

            // 工具调用增量
            ToolCall toolCall = null;
            JsonNode tcNode = delta.path("tool_calls");
            if (tcNode.isArray() && tcNode.size() > 0) {
                JsonNode tc = tcNode.get(0);
                JsonNode func = tc.path("function");
                toolCall = ToolCall.partial(
                        pathText(tc, "id"),
                        pathText(func, "name"),
                        pathText(func, "arguments"));
            }

            if (content == null && toolCall == null && finishReason == null) {
                return null;
            }

            return StreamChunk.builder()
                    .content(content)
                    .toolCall(toolCall)
                    .finishReason(finishReason)
                    .index(pathInt(choice, "index"))
                    .build();
        } catch (JsonProcessingException e) {
            log.debug("OpenAI SSE 行解析跳过: {}", data);
            return null;
        }
    }

    // ==================== 工具方法 ====================

    private Usage parseUsage(JsonNode usageNode) {
        if (usageNode.isMissingNode()) {
            return null;
        }
        return Usage.builder()
                .promptTokens(pathInt(usageNode, "prompt_tokens"))
                .completionTokens(pathInt(usageNode, "completion_tokens"))
                .totalTokens(pathInt(usageNode, "total_tokens"))
                .build();
    }

    private static String pathText(JsonNode node, String field) {
        JsonNode child = node.path(field);
        return child.isMissingNode() || child.isNull() ? null : child.asText();
    }

    private static Integer pathInt(JsonNode node, String field) {
        JsonNode child = node.path(field);
        return child.isMissingNode() || child.isNull() ? null : child.asInt();
    }
}
