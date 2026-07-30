package com.ttk.springbootinit.ai.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ttk.springbootinit.ai.model.ToolCall;
import com.ttk.springbootinit.ai.model.ToolDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具注册中心（注册表模式）。
 * <p>
 * 管理所有可用工具的定义与执行函数。Agent 在执行循环中通过此注册中心
 * 查找并调用工具。
 * <p>
 * 使用方式：
 * <pre>{@code
 * toolRegistry.register(
 *     ToolDefinition.of("get_weather", "获取城市天气",
 *         Map.of("city", Map.of("type", "string", "description", "城市名")),
 *         List.of("city")),
 *     args -> "北京今天晴，25°C"
 * );
 * }</pre>
 *
 * @author Rangsh
 */
@Component
public class ToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(ToolRegistry.class);

    private final Map<String, ToolEntry> tools = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public ToolRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 注册工具。
     *
     * @param definition 工具定义（含 JSON Schema）
     * @param handler    工具执行函数
     */
    public void register(ToolDefinition definition, ToolHandler handler) {
        String name = definition.getFunction().getName();
        tools.put(name, new ToolEntry(definition, handler));
        log.info("工具已注册: {}", name);
    }

    /**
     * 批量注册。
     */
    public void registerAll(Map<ToolDefinition, ToolHandler> toolMap) {
        toolMap.forEach(this::register);
    }

    /**
     * 注销工具。
     */
    public void unregister(String name) {
        tools.remove(name);
        log.info("工具已注销: {}", name);
    }

    /**
     * 获取所有工具定义（用于拼入 LLM 请求）。
     */
    public List<ToolDefinition> getDefinitions() {
        List<ToolDefinition> defs = new ArrayList<>();
        for (ToolEntry entry : tools.values()) {
            defs.add(entry.definition);
        }
        return defs;
    }

    /**
     * 执行工具调用。
     *
     * @param toolCall 工具调用信息
     * @return 工具执行结果（JSON 字符串）
     */
    public String execute(ToolCall toolCall) {
        String name = toolCall.getFunction().getName();
        ToolEntry entry = tools.get(name);
        if (entry == null) {
            log.warn("未知工具: {}", name);
            return errorResult("未知工具: " + name);
        }

        try {
            Map<String, Object> args = parseArguments(toolCall.getFunction().getArguments());
            Object result = entry.handler.execute(args);
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("工具执行失败: {} - {}", name, e.getMessage(), e);
            return errorResult("工具执行失败: " + e.getMessage());
        }
    }

    /**
     * 判断工具是否存在。
     */
    public boolean has(String name) {
        return tools.containsKey(name);
    }

    /**
     * 已注册工具数量。
     */
    public int size() {
        return tools.size();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseArguments(String argsJson) throws JsonProcessingException {
        return objectMapper.readValue(argsJson, Map.class);
    }

    /**
     * 构造 JSON 格式的错误结果（安全转义）。
     */
    private String errorResult(String message) {
        try {
            return objectMapper.writeValueAsString(Map.of("error", message));
        } catch (JsonProcessingException e) {
            return "{\"error\":\"ToolRegistry internal error\"}";
        }
    }

    /**
     * 内部条目。
     */
    private record ToolEntry(ToolDefinition definition, ToolHandler handler) {
    }
}
