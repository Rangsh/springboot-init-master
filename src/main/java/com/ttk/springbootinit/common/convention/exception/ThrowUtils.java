package com.ttk.springbootinit.common.convention.exception;

import com.ttk.springbootinit.common.convention.errorcode.IErrorCode;

/**
 * 抛异常工具
 *
 * @author Rangsh
 */
public final class ThrowUtils {

    private ThrowUtils() {
    }

    public static void throwIf(boolean condition, RuntimeException runtimeException) {
        if (condition) {
            throw runtimeException;
        }
    }

    public static void throwIf(boolean condition, IErrorCode errorCode) {
        throwIf(condition, new ClientException(errorCode));
    }

    public static void throwIf(boolean condition, IErrorCode errorCode, String message) {
        throwIf(condition, new ClientException(message, errorCode));
    }
}
