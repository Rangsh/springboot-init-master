package com.ttk.springbootinit.ai.streaming;

import com.ttk.springbootinit.ai.model.ChatResponse;
import com.ttk.springbootinit.ai.model.StreamCallback;
import com.ttk.springbootinit.ai.model.StreamChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

/**
 * SSE 流式桥接——将 {@link StreamCallback} 事件转发到 Spring {@link SseEmitter}。
 * <p>
 * 设计为独立组件，不依赖 Servlet API 的 Provider 层通过此类与 HTTP 客户端通信。
 * <p>
 * 使用方式：
 * <pre>{@code
 * SseEmitter emitter = new SseEmitter(300_000L);
 * SseEmitterBridge bridge = new SseEmitterBridge(emitter);
 * provider.chatStream(request, bridge);
 * return emitter;
 * }</pre>
 *
 * @author Rangsh
 */
public class SseEmitterBridge implements StreamCallback {

    private static final Logger log = LoggerFactory.getLogger(SseEmitterBridge.class);

    private final SseEmitter emitter;
    private final StringBuilder contentBuilder = new StringBuilder();

    public SseEmitterBridge(SseEmitter emitter) {
        this.emitter = emitter;
        setupEmitterCallbacks();
    }

    private void setupEmitterCallbacks() {
        emitter.onCompletion(() -> log.debug("SSE 连接正常关闭"));
        emitter.onTimeout(() -> log.warn("SSE 连接超时"));
        emitter.onError(ex -> log.error("SSE 连接异常: {}", ex.getMessage()));
    }

    @Override
    public void onChunk(StreamChunk chunk) {
        try {
            if (chunk.getContent() != null) {
                contentBuilder.append(chunk.getContent());
            }
            emitter.send(SseEmitter.event()
                    .name("chunk")
                    .data(chunk, org.springframework.http.MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            log.warn("SSE 发送 chunk 失败: {}", e.getMessage());
            emitter.completeWithError(e);
        }
    }

    @Override
    public void onComplete(ChatResponse response) {
        try {
            // 发送最终累积消息
            emitter.send(SseEmitter.event()
                    .name("done")
                    .data(response, org.springframework.http.MediaType.APPLICATION_JSON));
            emitter.complete();
        } catch (IOException e) {
            log.error("SSE 完成事件发送失败: {}", e.getMessage());
            emitter.completeWithError(e);
        }
    }

    @Override
    public void onError(Throwable error) {
        log.error("LLM 流式错误: {}", error.getMessage(), error);
        try {
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(Map.of("error", error.getMessage())));
        } catch (IOException e) {
            // 忽略
        }
        emitter.completeWithError(error);
    }

    /**
     * 获取内部 SseEmitter（用于 Controller 返回）。
     */
    public SseEmitter emitter() {
        return emitter;
    }

    /**
     * 创建带超时设置的 SseEmitter + Bridge。
     *
     * @param timeoutMs 超时毫秒数（null 则默认 300 秒）
     */
    public static SseEmitterBridge create(Long timeoutMs) {
        long timeout = timeoutMs != null ? timeoutMs : 300_000L;
        return new SseEmitterBridge(new SseEmitter(timeout));
    }
}
