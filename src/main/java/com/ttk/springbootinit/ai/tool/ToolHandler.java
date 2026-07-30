package com.ttk.springbootinit.ai.tool;

import java.util.Map;

/**
 * 工具执行处理器（函数式接口）。
 * <p>
 * 接收 JSON Schema 解析后的参数 Map，返回任意对象（会被序列化为 JSON 字符串）。
 *
 * @author Rangsh
 */
@FunctionalInterface
public interface ToolHandler {

    /**
     * 执行工具逻辑。
     *
     * @param arguments 参数键值对（已从 JSON 解析）
     * @return 执行结果（会被序列化为 JSON）
     * @throws Exception 执行异常
     */
    Object execute(Map<String, Object> arguments) throws Exception;
}
