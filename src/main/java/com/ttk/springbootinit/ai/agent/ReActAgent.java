package com.ttk.springbootinit.ai.agent;

import com.ttk.springbootinit.ai.provider.LlmProvider;
import com.ttk.springbootinit.ai.model.ChatMessage;
import com.ttk.springbootinit.ai.model.ChatRequest;
import com.ttk.springbootinit.ai.model.ChatResponse;
import com.ttk.springbootinit.ai.model.ChatRole;
import com.ttk.springbootinit.ai.model.StreamCallback;
import com.ttk.springbootinit.ai.model.StreamChunk;
import com.ttk.springbootinit.ai.model.ToolCall;
import com.ttk.springbootinit.ai.model.ToolDefinition;
import com.ttk.springbootinit.ai.streaming.SseEmitterBridge;
import com.ttk.springbootinit.ai.tool.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ReAct（Reasoning + Acting）Agent。
 * <p>
 * 实现标准 ReAct 循环：
 * <ol>
 *   <li>发送用户 query + 工具定义给 LLM</li>
 *   <li>解析响应中的 tool calls</li>
 *   <li>执行工具 → 将结果追加到消息历史</li>
 *   <li>循环直到 LLM 不再请求工具，或达到最大步数</li>
 * </ol>
 * <p>
 * 使用方式：
 * <pre>{@code
 * ReActAgent agent = new ReActAgent(provider, toolRegistry);
 * ChatResponse result = agent.execute("北京今天天气怎么样？");
 * }</pre>
 *
 * @author Rangsh
 */
public class ReActAgent {

    private static final Logger log = LoggerFactory.getLogger(ReActAgent.class);

    /** ReAct 默认系统提示词 */
    private static final String DEFAULT_SYSTEM_PROMPT = """
            你是一个智能助手，可以使用提供的工具来完成任务。
            请按照以下步骤思考：
            1. 分析用户的问题
            2. 确定是否需要使用工具
            3. 如果需要，调用合适的工具获取信息
            4. 基于工具返回的结果，给出最终答案

            如果不需要工具或已经获得足够信息，直接回答用户问题。
            """;

    private final LlmProvider provider;
    private final ToolRegistry toolRegistry;
    private final String model;
    private final int maxSteps;
    private final String systemPrompt;

    public ReActAgent(LlmProvider provider, ToolRegistry toolRegistry,
                      String model, int maxSteps, String systemPrompt) {
        this.provider = provider;
        this.toolRegistry = toolRegistry;
        this.model = model;
        this.maxSteps = maxSteps;
        this.systemPrompt = systemPrompt != null ? systemPrompt : DEFAULT_SYSTEM_PROMPT;
    }

    /**
     * 同步执行 Agent 循环。
     *
     * @param userMessage 用户输入
     * @return 最终响应
     */
    public ChatResponse execute(String userMessage) {
        return execute(userMessage, Map.of());
    }

    /**
     * 同步执行（带初始上下文消息）。
     */
    public ChatResponse execute(String userMessage, Map<String, Object> context) {
        List<ChatMessage> messages = buildInitialMessages(userMessage, context);
        List<ToolDefinition> tools = toolRegistry.getDefinitions();

        for (int step = 0; step < maxSteps; step++) {
            log.debug("Agent 步骤 {}/{}", step + 1, maxSteps);

            ChatRequest request = ChatRequest.builder()
                    .model(model)
                    .messages(new ArrayList<>(messages))
                    .tools(tools.isEmpty() ? null : tools)
                    .build();

            ChatResponse response = provider.chat(request);

            if (!response.hasToolCalls()) {
                log.debug("Agent 完成：LLM 不再请求工具");
                return response;
            }

            // 记录 assistant 消息
            ChatMessage assistantMsg = safeFirstMessage(response);
            if (assistantMsg == null) {
                log.warn("Agent 步骤 {} 响应无消息，终止", step + 1);
                return response;
            }
            messages.add(assistantMsg);

            // 执行所有工具调用
            for (ToolCall tc : response.toolCalls()) {
                log.debug("Agent 执行工具: {}", tc.getFunction().getName());
                String result = toolRegistry.execute(tc);
                messages.add(ChatMessage.tool(tc.getId(), result));
            }
        }

        // 达到最大步数，做最后一次不带工具的调用
        log.warn("Agent 达到最大步数 {}，强制返回", maxSteps);
        ChatRequest finalRequest = ChatRequest.builder()
                .model(model)
                .messages(messages)
                .build();
        return provider.chat(finalRequest);
    }

    private static ChatMessage safeFirstMessage(ChatResponse response) {
        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            return null;
        }
        return response.getChoices().get(0);
    }

    /**
     * 流式执行 Agent 循环。
     * <p>
     * 注意：目前 ReAct 的 tool execution 阶段是非流式的（需要完整 tool call 才能执行），
     * 仅最后一步的 LLM 回复走流式通道。
     *
     * @param userMessage 用户输入
     * @param timeoutMs   SSE 超时时间（毫秒）
     * @return SseEmitter
     */
    public SseEmitter executeStream(String userMessage, Long timeoutMs) {
        SseEmitterBridge bridge = SseEmitterBridge.create(timeoutMs);
        executeStream(userMessage, bridge);
        return bridge.emitter();
    }

    /**
     * 流式执行（使用指定的 StreamCallback）。
     */
    public void executeStream(String userMessage, StreamCallback callback) {
        List<ChatMessage> messages = buildInitialMessages(userMessage, Map.of());
        List<ToolDefinition> tools = toolRegistry.getDefinitions();

        for (int step = 0; step < maxSteps; step++) {
            log.debug("Agent 流式步骤 {}/{}", step + 1, maxSteps);

            ChatRequest request = ChatRequest.builder()
                    .model(model)
                    .messages(new ArrayList<>(messages))
                    .tools(tools.isEmpty() ? null : tools)
                    .build();

            // 使用同步调用获取 tool call 判断
            ChatResponse response = provider.chat(request);

            if (!response.hasToolCalls()) {
                // 最后一步：走流式
                ChatRequest streamRequest = ChatRequest.builder()
                        .model(model)
                        .messages(new ArrayList<>(messages))
                        .stream(true)
                        .build();
                provider.chatStream(streamRequest, callback);
                return;
            }

            // 发送工具调用事件
            ChatMessage assistantMsg = safeFirstMessage(response);
            if (assistantMsg == null) {
                log.warn("Agent 流式步骤 {} 响应无消息，终止", step + 1);
                return;
            }
            messages.add(assistantMsg);

            for (ToolCall tc : response.toolCalls()) {
                callback.onChunk(StreamChunk.builder()
                        .toolCall(tc)
                        .build());
                String result = toolRegistry.execute(tc);
                messages.add(ChatMessage.tool(tc.getId(), result));
            }
        }

        // 最后一步流式
        ChatRequest finalRequest = ChatRequest.builder()
                .model(model)
                .messages(messages)
                .stream(true)
                .build();
        provider.chatStream(finalRequest, callback);
    }

    private List<ChatMessage> buildInitialMessages(String userMessage, Map<String, Object> context) {
        List<ChatMessage> messages = new ArrayList<>();

        // 系统提示词
        StringBuilder sysPrompt = new StringBuilder(systemPrompt);
        if (!context.isEmpty()) {
            sysPrompt.append("\n\n## 当前上下文\n");
            context.forEach((k, v) -> sysPrompt.append("- ").append(k).append(": ").append(v).append("\n"));
        }
        messages.add(ChatMessage.system(sysPrompt.toString()));

        // 用户消息
        messages.add(ChatMessage.user(userMessage));

        return messages;
    }

    public LlmProvider provider() { return provider; }
    public ToolRegistry toolRegistry() { return toolRegistry; }
    public String model() { return model; }
    public int maxSteps() { return maxSteps; }
}
