package com.ttk.springbootinit.ai.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 流式响应片段。
 *
 * @author Rangsh
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StreamChunk {

    /** 增量文本内容 */
    private String content;

    /** 流式工具调用片段 */
    private ToolCall toolCall;

    /** 停止原因（最后一个 chunk 有值） */
    private String finishReason;

    /** 当前片段序号 */
    private Integer index;

    /**
     * 创建文本增量。
     */
    public static StreamChunk text(String content) {
        return StreamChunk.builder().content(content).build();
    }

    /**
     * 创建工具调用增量。
     */
    public static StreamChunk toolCall(ToolCall toolCall) {
        return StreamChunk.builder().toolCall(toolCall).build();
    }
}
