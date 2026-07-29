package com.ttk.springbootinit.common.web;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.ttk.springbootinit.common.convention.errorcode.BaseErrorCode;
import com.ttk.springbootinit.common.convention.exception.AbstractException;
import com.ttk.springbootinit.common.convention.result.Result;
import com.ttk.springbootinit.common.convention.result.Results;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Optional;

/**
 * 全局异常处理
 *
 * @author Rangsh
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Value("${spring.servlet.multipart.max-file-size:10MB}")
    private String maxFileSize;

    @Value("${spring.servlet.multipart.max-request-size:20MB}")
    private String maxRequestSize;

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<Void> validExceptionHandler(HttpServletRequest request, Exception ex) {
        BindingResult bindingResult = ex instanceof MethodArgumentNotValidException manv
                ? manv.getBindingResult()
                : ((BindException) ex).getBindingResult();
        FieldError firstFieldError = CollectionUtil.getFirst(bindingResult.getFieldErrors());
        String message = Optional.ofNullable(firstFieldError)
                .map(FieldError::getDefaultMessage)
                .orElse(StrUtil.EMPTY);
        log.error("参数校验失败 [{}] {} 原因: {}", request.getMethod(), getUrl(request), message);
        return Results.failure(BaseErrorCode.PARAMS_ERROR.code(), message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> constraintViolationExceptionHandler(HttpServletRequest request, ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .findFirst()
                .map(item -> item.getMessage())
                .orElse(StrUtil.EMPTY);
        log.error("约束校验失败 [{}] {} 原因: {}", request.getMethod(), getUrl(request), message);
        return Results.failure(BaseErrorCode.PARAMS_ERROR.code(), message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> httpMessageNotReadableExceptionHandler(HttpServletRequest request, HttpMessageNotReadableException ex) {
        log.error("请求体解析失败 [{}] {} 原因: {}", request.getMethod(), getUrl(request), ex.getMessage());
        return Results.failure(BaseErrorCode.PARAMS_ERROR.code(), "请求体不能为空或格式错误");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> illegalArgumentExceptionHandler(HttpServletRequest request, IllegalArgumentException ex) {
        log.error("非法参数 [{}] {} 原因: {}", request.getMethod(), getUrl(request), ex.getMessage());
        return Results.failure(BaseErrorCode.PARAMS_ERROR.code(), ex.getMessage());
    }

    @ExceptionHandler(AbstractException.class)
    public Result<Void> abstractException(HttpServletRequest request, AbstractException ex) {
        if (ex.getCause() != null) {
            log.error("业务异常 [{}] {} 详情: {}", request.getMethod(), getUrl(request), ex.toString(), ex.getCause());
        } else {
            log.error("业务异常 [{}] {} 详情: {}", request.getMethod(), getUrl(request), ex.toString());
        }
        return Results.failure(ex);
    }

    @ExceptionHandler(NotLoginException.class)
    public Result<Void> notLoginException(HttpServletRequest request, NotLoginException ex) {
        log.warn("鉴权失败-未登录 [{}] {} 原因: {}", request.getMethod(), getUrl(request), ex.getMessage());
        return Results.failure(BaseErrorCode.NOT_LOGIN_ERROR.code(), BaseErrorCode.NOT_LOGIN_ERROR.message());
    }

    @ExceptionHandler(NotRoleException.class)
    public Result<Void> notRoleException(HttpServletRequest request, NotRoleException ex) {
        log.warn("鉴权失败-无权限 [{}] {} 原因: {}", request.getMethod(), getUrl(request), ex.getMessage());
        return Results.failure(BaseErrorCode.NO_AUTH_ERROR.code(), BaseErrorCode.NO_AUTH_ERROR.message());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> maxUploadSizeExceededException(HttpServletRequest request, MaxUploadSizeExceededException ex) {
        log.warn("上传超限 [{}] {} 原因: {}", request.getMethod(), getUrl(request), ex.getMessage());
        String message = "上传大小超过限制，单个文件最大 " + maxFileSize + "，单次请求最大 " + maxRequestSize;
        return Results.failure(BaseErrorCode.PARAMS_ERROR.code(), message);
    }

    @ExceptionHandler(Throwable.class)
    public Result<Void> defaultErrorHandler(HttpServletRequest request, Throwable throwable) {
        log.error("系统异常 [{}] {}", request.getMethod(), getUrl(request), throwable);
        return Results.failure();
    }

    private String getUrl(HttpServletRequest request) {
        if (StrUtil.isBlank(request.getQueryString())) {
            return request.getRequestURL().toString();
        }
        return request.getRequestURL() + "?" + request.getQueryString();
    }
}
