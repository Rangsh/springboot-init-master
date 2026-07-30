package com.ttk.springbootinit.ai.controller;

import com.ttk.springbootinit.ai.agent.ReActAgent;
import com.ttk.springbootinit.ai.provider.LlmProvider;
import com.ttk.springbootinit.ai.model.ChatMessage;
import com.ttk.springbootinit.ai.model.ChatRequest;
import com.ttk.springbootinit.ai.model.ChatResponse;
import com.ttk.springbootinit.ai.prompt.PromptTemplate;
import com.ttk.springbootinit.ai.prompt.PromptTemplateLoader;
import com.ttk.springbootinit.ai.streaming.SseEmitterBridge;
import com.ttk.springbootinit.ai.tool.ToolRegistry;
import com.ttk.springbootinit.common.constant.TraceConstant;
import com.ttk.springbootinit.common.convention.result.Result;
import com.ttk.springbootinit.common.convention.result.Results;
import org.slf4j.MDC;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * AI 接口示例 Controller。
 * <p>
 * 展示 LLM 调用的三种核心模式：同步聊天、流式聊天、Agent 工具调用。
 *
 * @author Rangsh
 */
@Tag(name = "AI 接口")
@RestController
@RequestMapping("/ai")
@ConditionalOnProperty(prefix = "llm", name = "enabled", havingValue = "true")
public class AiController {

    private static final Logger log = LoggerFactory.getLogger(AiController.class);

    private final LlmProvider llmProvider;
    private final Map<String, LlmProvider> llmProviders;
    private final ToolRegistry toolRegistry;
    private final PromptTemplateLoader templateLoader;

    public AiController(LlmProvider llmProvider,
                        Map<String, LlmProvider> llmProviders,
                        ToolRegistry toolRegistry,
                        PromptTemplateLoader templateLoader) {
        this.llmProvider = llmProvider;
        this.llmProviders = llmProviders;
        this.toolRegistry = toolRegistry;
        this.templateLoader = templateLoader;
    }

    // ==================== 同步聊天 ====================

    @Operation(summary = "同步聊天", description = "发送消息并等待完整回复")
    @PostMapping("/chat")
    public Result<ChatResponse> chat(@Valid @RequestBody ChatRequestDTO dto) {
        LlmProvider provider = resolveProvider(dto.getProvider());
        ChatRequest request = ChatRequest.builder()
                .model(dto.getModel())
                .messages(List.of(ChatMessage.user(dto.getMessage())))
                .temperature(dto.getTemperature())
                .maxTokens(dto.getMaxTokens())
                .tools(dto.getEnableTools() ? toolRegistry.getDefinitions() : null)
                .build();
        ChatResponse response = provider.chat(request);
        return Results.success(response);
    }

    // ==================== 流式聊天（SSE） ====================

    @Operation(summary = "流式聊天（SSE）", description = "通过 Server-Sent Events 流式返回 LLM 回复。"
            + "Event 类型：chunk（增量文本）, done（完整响应）, error（错误信息）")
    @PostMapping("/chat/stream")
    public SseEmitter chatStream(@Valid @RequestBody ChatRequestDTO dto) {
        LlmProvider provider = resolveProvider(dto.getProvider());
        ChatRequest request = ChatRequest.builder()
                .model(dto.getModel())
                .messages(List.of(ChatMessage.user(dto.getMessage())))
                .temperature(dto.getTemperature())
                .maxTokens(dto.getMaxTokens())
                .stream(true)
                .build();

        SseEmitterBridge bridge = SseEmitterBridge.create(300_000L);
        provider.chatStream(request, bridge);
        return bridge.emitter();
    }

    // ==================== Agent ====================

    @Operation(summary = "Agent 执行（同步）", description = "ReAct Agent：LLM 可自主调用注册的工具获取信息后回答")
    @PostMapping("/agent")
    public Result<ChatResponse> agent(@Valid @RequestBody ChatRequestDTO dto) {
        LlmProvider provider = resolveProvider(dto.getProvider());
        ReActAgent agent = new ReActAgent(
                provider, toolRegistry,
                dto.getModel(),
                dto.getMaxSteps() != null ? dto.getMaxSteps() : 5,
                dto.getSystemPrompt());
        ChatResponse response = agent.execute(dto.getMessage());
        return Results.success(response);
    }

    @Operation(summary = "Agent 执行（流式 SSE）", description = "ReAct Agent 流式返回")
    @PostMapping("/agent/stream")
    public SseEmitter agentStream(@Valid @RequestBody ChatRequestDTO dto) {
        LlmProvider provider = resolveProvider(dto.getProvider());
        ReActAgent agent = new ReActAgent(
                provider, toolRegistry,
                dto.getModel(),
                dto.getMaxSteps() != null ? dto.getMaxSteps() : 5,
                dto.getSystemPrompt());
        return agent.executeStream(dto.getMessage(), 300_000L);
    }

    // ==================== Prompt 模版 ====================

    @Operation(summary = "Prompt 模版列表", description = "查看已加载的所有 Prompt 模版")
    @GetMapping("/prompts")
    public Result<List<PromptTemplate>> listPrompts() {
        return Results.success(templateLoader.cachedTemplates());
    }

    @Operation(summary = "渲染 Prompt 模版", description = "加载模版并渲染变量")
    @PostMapping("/prompt/render")
    public Result<Object> renderPrompt(@Parameter(description = "模版名称") @RequestParam String name,
                                        @Parameter(description = "模版版本（可选，不传则用最新）") @RequestParam(required = false) String version,
                                        @RequestBody Map<String, Object> variables) {
        PromptTemplate template = version != null
                ? templateLoader.load(name, version)
                : templateLoader.load(name);
        if (template == null) {
            return failure("A000301", "Prompt 模版不存在: " + name);
        }
        String rendered = template.render(variables);
        return Results.success(rendered);
    }

    // ==================== Provider 信息 ====================

    @Operation(summary = "已启用 Provider 列表")
    @GetMapping("/providers")
    public Result<List<String>> providers() {
        return Results.success(llmProviders.keySet().stream().toList());
    }

    // ---- 内部 ----

    /**
     * 构造泛型失败 Result（绕过 {@link Results#failure} 返回 {@code Result<Void>} 的限制）。
     */
    @SuppressWarnings("unchecked")
    private static <T> Result<T> failure(String code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        String traceId = MDC.get(TraceConstant.TRACE_ID);
        if (traceId != null && !traceId.isBlank()) {
            result.setRequestId(traceId);
        }
        return result;
    }

    private LlmProvider resolveProvider(String name) {
        if (name == null || name.isBlank()) {
            return llmProvider; // 默认
        }
        LlmProvider p = llmProviders.get(name);
        if (p == null) {
            log.warn("Provider [{}] 不可用，回退到默认 [{}]", name, llmProvider.providerName());
            return llmProvider;
        }
        return p;
    }

    @Data
    public static class ChatRequestDTO {
        @NotBlank(message = "消息内容不能为空")
        @Parameter(description = "消息内容")
        private String message;
        @Parameter(description = "模型名（可选，默认用配置值）")
        private String model;
        @Parameter(description = "Provider 名（可选，默认用 llm.default-provider）")
        private String provider;
        @Parameter(description = "温度 0~2")
        private Double temperature;
        @Parameter(description = "最大输出 token")
        private Integer maxTokens;
        @Parameter(description = "是否启用工具调用")
        private Boolean enableTools;
        @Parameter(description = "Agent 最大步数")
        private Integer maxSteps;
        @Parameter(description = "系统提示词（Agent 模式）")
        private String systemPrompt;
    }
}
