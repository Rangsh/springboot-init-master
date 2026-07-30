package com.ttk.springbootinit.ai.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 工具定义（JSON Schema 描述参数）。
 *
 * @author Rangsh
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolDefinition {

    /** 工具类型，通常固定为 "function" */
    @Builder.Default
    private String type = "function";

    /** 函数定义 */
    private FunctionDef function;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FunctionDef {

        /** 函数名 */
        private String name;

        /** 函数描述 */
        private String description;

        /** JSON Schema 参数定义 */
        private Parameters parameters;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Parameters {

        @Builder.Default
        private String type = "object";

        private Map<String, Object> properties;

        private java.util.List<String> required;
    }

    /**
     * 便捷构造：给定名称、描述和 JSON Schema properties。
     */
    public static ToolDefinition of(String name, String description,
                                     Map<String, Object> properties,
                                     java.util.List<String> required) {
        return ToolDefinition.builder()
                .function(FunctionDef.builder()
                        .name(name)
                        .description(description)
                        .parameters(Parameters.builder()
                                .properties(properties)
                                .required(required)
                                .build())
                        .build())
                .build();
    }
}
