package com.ttk.springbootinit.ai.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一聊天请求（以 OpenAI 协议为规范格式）。
 * <p>
 * 各 Provider 实现负责将本结构翻译为自身 API 格式。
 *
 * @author Rangsh
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatRequest {

    /** 模型名，如 gpt-4o / claude-sonnet-5-20251001 / deepseek-chat */
    private String model;

    /** 消息列表 */
    @Singular
    private List<ChatMessage> messages;

    /** 温度 0~2，默认 0.8 */
    @Builder.Default
    private Double temperature = 0.8;

    /** 最大输出 token 数 */
    @JsonProperty("max_tokens")
    private Integer maxTokens;

    /** Top-p 采样 */
    @JsonProperty("top_p")
    private Double topP;

    /** 是否流式返回 */
    @Builder.Default
    private Boolean stream = false;

    /** 可用工具列表 */
    @Singular
    private List<ToolDefinition> tools;

    /** 工具选择策略：auto / none / required / 指定工具名 */
    @JsonProperty("tool_choice")
    private Object toolChoice;

    /** 停止词 */
    @Singular("stop")
    private List<String> stop;

    /** Provider 特有扩展参数（会合并到请求体根层） */
    @Singular("extraParam")
    private Map<String, Object> extraParams;

    // ---- 便捷方法 ----

    public static ChatRequest of(String model, String systemPrompt, String userMessage) {
        List<ChatMessage> msgs = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            msgs.add(ChatMessage.system(systemPrompt));
        }
        msgs.add(ChatMessage.user(userMessage));
        return ChatRequest.builder()
                .model(model)
                .messages(msgs)
                .build();
    }

    /**
     * 获取最后一条消息。
     */
    public ChatMessage lastMessage() {
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        return messages.get(messages.size() - 1);
    }

    /**
     * 获取所有 system 消息的内容（合并）。
     */
    public String systemPrompt() {
        if (messages == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (ChatMessage msg : messages) {
            if (msg.getRole() == ChatRole.SYSTEM && msg.getContent() != null) {
                if (sb.length() > 0) {
                    sb.append("\n\n");
                }
                sb.append(msg.getContent());
            }
        }
        return sb.isEmpty() ? null : sb.toString();
    }
}
