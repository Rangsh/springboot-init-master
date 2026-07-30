package com.ttk.springbootinit.ai.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 规范消息体。
 * <ul>
 *   <li>{@code role} + {@code content} 为通用字段</li>
 *   <li>{@code toolCalls} 用于 assistant 消息携带工具调用</li>
 *   <li>{@code toolCallId} 用于 tool 消息关联调用</li>
 * </ul>
 *
 * @author Rangsh
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessage {

    private ChatRole role;

    private String content;

    /** assistant 消息中的工具调用列表 */
    @JsonProperty("tool_calls")
    private List<ToolCall> toolCalls;

    /** tool 消息关联的工具调用 ID */
    @JsonProperty("tool_call_id")
    private String toolCallId;

    // ---- 便捷构造 ----

    public static ChatMessage system(String content) {
        return ChatMessage.builder().role(ChatRole.SYSTEM).content(content).build();
    }

    public static ChatMessage user(String content) {
        return ChatMessage.builder().role(ChatRole.USER).content(content).build();
    }

    public static ChatMessage assistant(String content) {
        return ChatMessage.builder().role(ChatRole.ASSISTANT).content(content).build();
    }

    public static ChatMessage tool(String toolCallId, String content) {
        return ChatMessage.builder()
                .role(ChatRole.TOOL)
                .toolCallId(toolCallId)
                .content(content)
                .build();
    }
}
