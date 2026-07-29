package com.ttk.springbootinit.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

/**
 * JSON 工具（使用 Spring 容器中的 ObjectMapper，保证与全局 Jackson 配置一致）
 *
 * @author Rangsh
 */
public final class JsonUtils {

    private static volatile ObjectMapper OBJECT_MAPPER;

    private JsonUtils() {
    }

    /**
     * 由 Spring 初始化时注入全局 ObjectMapper
     */
    public static void init(ObjectMapper objectMapper) {
        OBJECT_MAPPER = objectMapper;
    }

    @SneakyThrows
    public static String toJsonString(Object obj) {
        return getMapper().writeValueAsString(obj);
    }

    @SneakyThrows
    public static <T> T parseObject(String json, Class<T> clazz) {
        return getMapper().readValue(json, clazz);
    }

    private static ObjectMapper getMapper() {
        ObjectMapper mapper = OBJECT_MAPPER;
        if (mapper == null) {
            throw new IllegalStateException("JsonUtils 尚未初始化，请确认 Jackson 配置已加载");
        }
        return mapper;
    }
}
