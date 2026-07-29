package com.ttk.springbootinit.common.convention.result;

import com.ttk.springbootinit.common.constant.TraceConstant;
import com.ttk.springbootinit.common.convention.errorcode.BaseErrorCode;
import com.ttk.springbootinit.common.convention.exception.AbstractException;
import org.slf4j.MDC;

import java.util.Optional;

/**
 * 全局返回对象构造器
 *
 * @author Rangsh
 */
public final class Results {

    private Results() {
    }

    public static Result<Void> success() {
        return fillTrace(new Result<Void>().setCode(Result.SUCCESS_CODE));
    }

    public static <T> Result<T> success(T data) {
        return fillTrace(new Result<T>().setCode(Result.SUCCESS_CODE).setData(data));
    }

    public static Result<Void> failure() {
        return fillTrace(new Result<Void>()
                .setCode(BaseErrorCode.SERVICE_ERROR.code())
                .setMessage(BaseErrorCode.SERVICE_ERROR.message()));
    }

    public static Result<Void> failure(AbstractException abstractException) {
        String errorCode = Optional.ofNullable(abstractException.getErrorCode())
                .orElse(BaseErrorCode.SERVICE_ERROR.code());
        String errorMessage = Optional.ofNullable(abstractException.getErrorMessage())
                .orElse(BaseErrorCode.SERVICE_ERROR.message());
        return fillTrace(new Result<Void>().setCode(errorCode).setMessage(errorMessage));
    }

    public static Result<Void> failure(String errorCode, String errorMessage) {
        return fillTrace(new Result<Void>().setCode(errorCode).setMessage(errorMessage));
    }

    private static <T> Result<T> fillTrace(Result<T> result) {
        String traceId = MDC.get(TraceConstant.TRACE_ID);
        if (traceId != null && !traceId.isBlank()) {
            result.setRequestId(traceId);
        }
        return result;
    }
}
