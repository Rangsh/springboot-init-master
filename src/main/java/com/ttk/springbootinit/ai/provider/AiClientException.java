package com.ttk.springbootinit.ai.provider;

/**
 * AI 客户端通用异常。
 *
 * @author Rangsh
 */
public class AiClientException extends RuntimeException {

    public AiClientException(String message) {
        super(message);
    }

    public AiClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
