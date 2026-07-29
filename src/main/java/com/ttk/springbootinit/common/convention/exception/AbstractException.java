package com.ttk.springbootinit.common.convention.exception;

import com.ttk.springbootinit.common.convention.errorcode.IErrorCode;
import lombok.Getter;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * 抽象异常：客户端 / 服务端 / 远程调用
 *
 * @author Rangsh
 */
@Getter
public abstract class AbstractException extends RuntimeException {

    private final String errorCode;

    private final String errorMessage;

    public AbstractException(String message, Throwable throwable, IErrorCode errorCode) {
        super(message, throwable);
        this.errorCode = errorCode.code();
        this.errorMessage = Optional.ofNullable(StringUtils.hasLength(message) ? message : null)
                .orElse(errorCode.message());
    }
}
