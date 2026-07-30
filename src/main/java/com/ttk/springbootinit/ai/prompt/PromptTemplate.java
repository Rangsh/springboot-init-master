package com.ttk.springbootinit.ai.prompt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 提示词模版。
 * <p>
 * 支持 {@code {{variable}}} 占位符语法，通过 render 方法替换变量。
 * 文件格式：YAML front matter + Markdown body（见 PromptTemplateLoader）。
 *
 * @author Rangsh
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptTemplate {

    /** 模版名称 */
    private String name;

    /** 版本号 */
    private String version;

    /** 描述 */
    private String description;

    /** 推荐模型 */
    private String model;

    /** 推荐温度 */
    private Double temperature;

    /** 最大 token */
    private Integer maxTokens;

    /** 模版内容（含 {{变量}} 占位符） */
    private String content;

    /** 变量名集合（从 content 中提取） */
    private Set<String> variables;

    // ---- 模版引擎 ----

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{(\\w+)}}");

    /**
     * 渲染模版：将所有 {@code {{varName}}} 替换为 params 中的值。
     *
     * @param params 变量名 → 值
     * @return 渲染后的文本
     * @throws IllegalArgumentException 如果有变量未提供
     */
    public String render(Map<String, Object> params) {
        if (content == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        Matcher matcher = VARIABLE_PATTERN.matcher(content);
        while (matcher.find()) {
            String varName = matcher.group(1);
            Object value = params.get(varName);
            if (value == null) {
                throw new IllegalArgumentException(
                        "模版 [" + name + " v" + version + "] 缺少变量: " + varName);
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value.toString()));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 提取模版中所有变量名。
     */
    public static Set<String> extractVariables(String content) {
        return VARIABLE_PATTERN.matcher(content).results()
                .map(r -> r.group(1))
                .collect(Collectors.toSet());
    }
}
