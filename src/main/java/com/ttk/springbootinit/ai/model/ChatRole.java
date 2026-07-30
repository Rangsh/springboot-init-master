package com.ttk.springbootinit.ai.model;

/**
 * 消息角色枚举（以 OpenAI 协议为规范格式，各 Provider 自行适配）。
 *
 * @author Rangsh
 */
public enum ChatRole {

    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant"),
    TOOL("tool");

    private final String value;

    ChatRole(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
