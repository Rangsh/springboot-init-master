package com.ttk.springbootinit.ai.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工具调用（assistant 消息中携带）。
 *
 * @author Rangsh
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolCall {

    /** 调用唯一标识 */
    private String id;

    /** 调用类型，通常为 "function" */
    private String type;

    /** 函数调用详情 */
    private FunctionCall function;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FunctionCall {

        /** 函数名 */
        private String name;

        /** JSON 格式参数 */
        private String arguments;
    }

    /**
     * 便捷构造：创建未完成的流式工具调用片段。
     */
    public static ToolCall partial(String id, String name, String argumentsChunk) {
        return ToolCall.builder()
                .id(id)
                .type("function")
                .function(new FunctionCall(name, argumentsChunk))
                .build();
    }
}
