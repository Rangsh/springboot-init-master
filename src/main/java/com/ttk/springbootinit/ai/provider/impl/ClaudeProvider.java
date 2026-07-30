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
import com.ttk.springbootinit.ai.model.StreamCallback;
import com.ttk.springbootinit.ai.model.StreamChunk;
import com.ttk.springbootinit.ai.model.ToolCall;
import com.ttk.springbootinit.ai.model.ToolDefinition;
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
 * Claude / Anthropic Messages API Provider。
 * <p>
 * API 文档：<a href="https://docs.anthropic.com/en/api/messages">Anthropic Messages API</a>
 * <p>
 * 与 OpenAI 的关键差异：
 * <ul>
 *   <li>system prompt 是顶层字段，不是 message role</li>
 *   <li>Content 使用 content block 数组（text / tool_use / tool_result）</li>
 *   <li>鉴权使用 x-api-key + anthropic-version 头</li>
 *   <li>SSE 使用 {@code event:} + {@code data:} 格式</li>
 * </ul>
 *
 * @author Rangsh
 */
public class ClaudeProvider extends AbstractLlmProvider {

    public ClaudeProvider(OkHttpClient httpClient, ObjectMapper objectMapper,
                          LlmProperties.ProviderConfig config) {
        super(httpClient, objectMapper, config);
    }

    @Override
    public String providerName() {
        return "claude";
    }

    @Override
    public List<String> supportedModels() {
        return config.getModels();
    }

    @Override
    protected String getApiEndpoint() {
        String base = config.getBaseUrl();
        if (base == null || base.isBlank()) {
            base = "https://api.anthropic.com";
        }
        return base + (base.endsWith("/") ? "" : "/") + "v1/messages";
    }

    @Override
    protected void buildAuthHeaders(Request.Builder builder) {
        builder.header("x-api-key", config.getApiKey());
        builder.header("anthropic-version", "2023-06-01");
        if (config.getHeaders() != null) {
            config.getHeaders().forEach(builder::header);
        }
    }

    @Override
    protected boolean usesDoneMarker() {
        return false; // Claude 使用 event: message_stop
    }

    // ==================== 请求体构建 ====================

    @Override
    protected String buildRequestBody(ChatRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", request.getModel());

        // 分离 system prompt 和普通消息
        String systemPrompt = request.systemPrompt();
        if (systemPrompt != null) {
            body.put("system", systemPrompt);
        }

        // 构建 user/assistant 消息（排除 system）
        List<Map<String, Object>> messages = new ArrayList<>();
        for (ChatMessage msg : request.getMessages()) {
            if (msg.getRole() == ChatRole.SYSTEM) {
                continue; // 已作为顶层 system 字段
            }
            messages.add(buildContentBlock(msg));
        }
        body.put("messages", messages);

        // max_tokens 对 Claude 是必填
        body.put("max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : 4096);

        if (request.getTemperature() != null) {
            body.put("temperature", request.getTemperature());
        }
        if (request.getTopP() != null) {
            body.put("top_p", request.getTopP());
        }

        // 工具
        if (request.getTools() != null && !request.getTools().isEmpty()) {
            body.put("tools", convertTools(request.getTools()));
        }

        if (request.getStream() != null && request.getStream()) {
            body.put("stream", true);
        }
        if (request.getStop() != null && !request.getStop().isEmpty()) {
            body.put("stop_sequences", request.getStop());
        }
        if (request.getExtraParams() != null) {
            body.putAll(request.getExtraParams());
        }

        return toJson(body);
    }

    /**
     * OpenAI JSON Schema tools → Claude tool format。
     */
    private List<Map<String, Object>> convertTools(List<ToolDefinition> tools) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (ToolDefinition td : tools) {
            Map<String, Object> tool = new HashMap<>();
            tool.put("name", td.getFunction().getName());
            tool.put("description", td.getFunction().getDescription());
            tool.put("input_schema", td.getFunction().getParameters());
            result.add(tool);
        }
        return result;
    }

    /**
     * 构建 Claude content block 格式的消息。
     */
    private Map<String, Object> buildContentBlock(ChatMessage msg) {
        Map<String, Object> node = new HashMap<>();
        node.put("role", msg.getRole().value());

        if (msg.getRole() == ChatRole.TOOL) {
            // tool_result block
            List<Map<String, Object>> content = new ArrayList<>();
            Map<String, Object> toolResult = new HashMap<>();
            toolResult.put("type", "tool_result");
            toolResult.put("tool_use_id", msg.getToolCallId());
            toolResult.put("content", msg.getContent() != null ? msg.getContent() : "");
            content.add(toolResult);
            node.put("content", content);
        } else if (msg.getRole() == ChatRole.ASSISTANT && msg.getToolCalls() != null
                && !msg.getToolCalls().isEmpty()) {
            // assistant + tool_use blocks
            List<Map<String, Object>> content = new ArrayList<>();
            if (msg.getContent() != null && !msg.getContent().isBlank()) {
                Map<String, Object> textBlock = new HashMap<>();
                textBlock.put("type", "text");
                textBlock.put("text", msg.getContent());
                content.add(textBlock);
            }
            for (ToolCall tc : msg.getToolCalls()) {
                Map<String, Object> toolUse = new HashMap<>();
                toolUse.put("type", "tool_use");
                toolUse.put("id", tc.getId());
                toolUse.put("name", tc.getFunction().getName());
                try {
                    toolUse.put("input", objectMapper.readTree(tc.getFunction().getArguments()));
                } catch (JsonProcessingException e) {
                    toolUse.put("input", objectMapper.createObjectNode());
                }
                content.add(toolUse);
            }
            node.put("content", content);
        } else {
            // 普通 text content block
            List<Map<String, Object>> content = new ArrayList<>();
            Map<String, Object> textBlock = new HashMap<>();
            textBlock.put("type", "text");
            textBlock.put("text", msg.getContent() != null ? msg.getContent() : "");
            content.add(textBlock);
            node.put("content", content);
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
            String stopReason = pathText(root, "stop_reason");

            // 解析 content blocks
            JsonNode contentBlocks = root.path("content");
            StringBuilder textContent = new StringBuilder();
            List<ToolCall> toolCalls = new ArrayList<>();

            for (JsonNode block : contentBlocks) {
                String type = pathText(block, "type");
                if ("text".equals(type)) {
                    textContent.append(pathText(block, "text"));
                } else if ("tool_use".equals(type)) {
                    toolCalls.add(ToolCall.builder()
                            .id(pathText(block, "id"))
                            .type("function")
                            .function(ToolCall.FunctionCall.builder()
                                    .name(pathText(block, "name"))
                                    .arguments(block.path("input").toString())
                                    .build())
                            .build());
                }
            }

            ChatMessage.ChatMessageBuilder msgBuilder = ChatMessage.builder()
                    .role(ChatRole.ASSISTANT)
                    .content(textContent.isEmpty() ? null : textContent.toString());
            if (!toolCalls.isEmpty()) {
                msgBuilder.toolCalls(toolCalls);
            }

            Usage usage = parseUsage(root.path("usage"));

            // 映射 stop_reason
            String finishReason = mapStopReason(stopReason);

            return ChatResponse.builder()
                    .id(id)
                    .model(model)
                    .choices(List.of(msgBuilder.build()))
                    .usage(usage)
                    .finishReason(finishReason)
                    .provider(providerName())
                    .build();
        } catch (JsonProcessingException e) {
            throw new AiClientException("Claude 响应解析失败", e);
        }
    }

    // ==================== 流式 SSE 解析（覆盖父类以处理 Claude 的 event: 格式） ====================

    @Override
    protected void parseStreamResponse(Response httpResponse,
                                        StreamCallback callback,
                                        ChatRequest request) {
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

            String eventType = null;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("event: ")) {
                    eventType = line.substring(7).trim();
                } else if (line.startsWith("data: ")) {
                    String data = line.substring(6);
                    if ("message_stop".equals(eventType)) {
                        break;
                    }
                    if ("error".equals(eventType)) {
                        callback.onError(new AiClientException("Claude SSE 错误: " + data));
                        return;
                    }
                    StreamChunk chunk = parseStreamDataLine(data, request);
                    if (chunk != null) {
                        if (chunk.getContent() != null) {
                            fullContent.append(chunk.getContent());
                        }
                        callback.onChunk(chunk);
                    }
                }
                // 空行表示事件结束，重置 eventType
                if (line.isEmpty()) {
                    eventType = null;
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

    @Override
    protected StreamChunk parseStreamDataLine(String data, ChatRequest request) {
        try {
            JsonNode root = objectMapper.readTree(data);
            String type = pathText(root, "type");

            if ("content_block_delta".equals(type)) {
                JsonNode delta = root.path("delta");
                String deltaType = pathText(delta, "type");
                if ("text_delta".equals(deltaType)) {
                    return StreamChunk.text(pathText(delta, "text"));
                } else if ("input_json_delta".equals(deltaType)) {
                    // 工具参数增量
                    return StreamChunk.toolCall(ToolCall.partial(
                            null, null, pathText(delta, "partial_json")));
                }
            } else if ("content_block_start".equals(type)) {
                JsonNode contentBlock = root.path("content_block");
                String blockType = pathText(contentBlock, "type");
                if ("tool_use".equals(blockType)) {
                    return StreamChunk.toolCall(ToolCall.partial(
                            pathText(contentBlock, "id"),
                            pathText(contentBlock, "name"),
                            ""));
                }
            } else if ("message_delta".equals(type)) {
                JsonNode delta = root.path("delta");
                String stopReason = pathText(delta, "stop_reason");
                if (stopReason != null) {
                    return StreamChunk.builder().finishReason(mapStopReason(stopReason)).build();
                }
            }

            return null;
        } catch (JsonProcessingException e) {
            log.debug("Claude SSE 行解析跳过: {}", data);
            return null;
        }
    }

    // ==================== 工具方法 ====================

    private Usage parseUsage(JsonNode usageNode) {
        if (usageNode.isMissingNode()) {
            return null;
        }
        return Usage.builder()
                .promptTokens(pathInt(usageNode, "input_tokens"))
                .completionTokens(pathInt(usageNode, "output_tokens"))
                .totalTokens(null) // Claude 不直接返回 total，需要加起来
                .build();
    }

    private String mapStopReason(String stopReason) {
        if (stopReason == null) return null;
        return switch (stopReason) {
            case "end_turn" -> "stop";
            case "max_tokens" -> "length";
            case "tool_use" -> "tool_calls";
            case "stop_sequence" -> "stop";
            default -> stopReason;
        };
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
