package com.ttk.springbootinit.common.constant;

/**
 * 链路追踪相关常量
 *
 * @author Rangsh
 */
public final class TraceConstant {

    private TraceConstant() {
    }

    /** MDC / 日志中的追踪键 */
    public static final String TRACE_ID = "traceId";

    /** 请求/响应头 */
    public static final String HEADER_TRACE_ID = "X-Trace-Id";
}
