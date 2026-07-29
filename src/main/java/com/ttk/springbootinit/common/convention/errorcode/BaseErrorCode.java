package com.ttk.springbootinit.common.convention.errorcode;

/**
 * 基础错误码（A 客户端 / B 系统 / C 第三方）
 */
public enum BaseErrorCode implements IErrorCode {

    CLIENT_ERROR("A000001", "用户端错误"),
    PARAMS_ERROR("A000100", "请求参数错误"),
    NOT_LOGIN_ERROR("A000200", "未登录或登录已过期"),
    NO_AUTH_ERROR("A000201", "权限不足"),
    NOT_FOUND_ERROR("A000300", "请求资源不存在"),
    FLOW_LIMIT_ERROR("A000400", "当前系统繁忙，请稍后再试"),

    SERVICE_ERROR("B000001", "系统执行出错"),
    SERVICE_TIMEOUT_ERROR("B000100", "系统执行超时"),

    REMOTE_ERROR("C000001", "调用第三方服务出错");

    private final String code;
    private final String message;

    BaseErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
