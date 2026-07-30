package com.ttk.springbootinit.ai.model;

/**
 * 流式回调接口（观察者模式）。
 * <p>
 * Provider 在流式响应中回调对应方法，上层可桥接到 SseEmitter / WebSocket / Reactor Flux 等。
 *
 * @author Rangsh
 */
public interface StreamCallback {

    /**
     * 收到一个增量片段。
     */
    void onChunk(StreamChunk chunk);

    /**
     * 流正常结束。
     *
     * @param response 累积后的完整响应
     */
    void onComplete(ChatResponse response);

    /**
     * 流出错。
     */
    void onError(Throwable error);
}
