package com.ttk.springbootinit.ai.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 统一聊天响应。
 *
 * @author Rangsh
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatResponse {

    /** 响应唯一 ID（由 LLM 平台返回） */
    private String id;

    /** 模型名 */
    private String model;

    /** 回复消息列表（非流式时为最终消息，流式时为累积消息） */
    private List<ChatMessage> choices;

    /** Token 用量 */
    private Usage usage;

    /** 停止原因：stop / length / tool_calls / content_filter */
    private String finishReason;

    /** Provider 名称 */
    private String provider;

    /**
     * 提取第一条消息的文本内容。
     */
    public String firstContent() {
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        ChatMessage first = choices.get(0);
        return first != null ? first.getContent() : null;
    }

    /**
     * 是否因工具调用而停止（需要继续执行工具并回传结果）。
     */
    public boolean hasToolCalls() {
        if (choices == null || choices.isEmpty()) {
            return false;
        }
        ChatMessage first = choices.get(0);
        return first != null
                && first.getToolCalls() != null
                && !first.getToolCalls().isEmpty();
    }

    /**
     * 获取工具调用列表。
     */
    public List<ToolCall> toolCalls() {
        if (!hasToolCalls()) {
            return List.of();
        }
        return choices.get(0).getToolCalls();
    }
}
