package com.yub.framework.web;

import com.yub.common.enums.ResponseCode;
import com.yub.common.exception.BaseException;
import com.yub.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * @Author: bing.yu
 * @CreateTime: 2026-06-10
 * @Description: 全局异常处理器
 * @Version: 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 捕获业务异常，返回业务错误响应
     *
     * @param e BaseException
     * @return 错误响应
     */
    @ExceptionHandler(BaseException.class)
    public Response<Void> handleBaseException(BaseException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Response.error(e.getCode(), e.getMessage());
    }

    /**
     * 捕获权限不足异常，返回403禁止访问
     *
     * @param e AccessDeniedException
     * @return 403 错误响应
     */
    @ExceptionHandler(AccessDeniedException.class)
    public Response<Void> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("权限异常: {}", e.getMessage());
        return Response.error(ResponseCode.FORBIDDEN);
    }

    /**
     * 捕获认证失败异常（如 JWT 过期、无效 Token），返回 401 未授权
     *
     * @param e AuthenticationException
     * @return 401 错误响应
     */
    @ExceptionHandler(AuthenticationException.class)
    public Response<Void> handleAuthenticationException(AuthenticationException e) {
        log.warn("认证失败: {}", e.getMessage());
        return Response.error(ResponseCode.UNAUTHORIZED);
    }

    /**
     * 捕获参数校验失败异常，返回字段级错误详情
     *
     * @param e MethodArgumentNotValidException
     * @return 400 错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Response<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("参数校验失败: {}", message);
        return Response.error(ResponseCode.BAD_REQUEST.getCode(), message);
    }

    /**
     * 捕获请求体格式错误异常
     *
     * @param e HttpMessageNotReadableException
     * @return 400 错误响应
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Response<Void> handleMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("请求体格式错误: {}", e.getMessage());
        return Response.error(ResponseCode.BAD_REQUEST);
    }

    /**
     * 兜底捕获所有未处理异常，返回500服务器错误
     *
     * @param e Exception
     * @return 500 错误响应
     */
    @ExceptionHandler(Exception.class)
    public Response<Void> handleException(Exception e) {
        log.error("系统异常: ", e);
        return Response.error(ResponseCode.INTERNAL_ERROR);
    }
}
