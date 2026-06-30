package com.yub.framework.web;

import com.yub.common.enums.ResponseCode;
import com.yub.common.exception.BaseException;
import com.yub.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    /** 从配置注入的 Spring multipart max-file-size，作为异常中无法获取限制值时的兜底 */
    @Value("${spring.servlet.multipart.max-file-size:5MB}")
    private String configuredMaxFileSize;

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
     * <p>注意：在无状态 JWT 架构中，认证异常由过滤器链内的
     * JwtAuthenticationEntryPoint 直接处理，不会传播到此处理器。
     * 保留以防未来架构变更。
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
     * 捕获文件上传大小超限异常
     * <p>当上传文件大小超过 spring.servlet.multipart.max-file-size 配置时触发。
     * 返回 413 Payload Too Large，并告知用户具体的限制值。
     *
     * @param e MaxUploadSizeExceededException
     * @return 错误响应
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public Response<Void> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        long maxSize = e.getMaxUploadSize();
        String maxSizeStr;

        if (maxSize > 0) {
            // 场景A：Spring 标准检测，getMaxUploadSize() 返回正确值
            maxSizeStr = formatFileSize(maxSize);
        } else if (e.getCause() != null) {
            // 场景B：Tomcat 层拦截（总请求体超过 max-request-size），
            // Spring 将其包装为 MaxUploadSizeExceededException(-1, cause)
            maxSizeStr = extractSizeFromMessage(e.getCause().getMessage());
        } else {
            // 场景C：从 Spring 自身消息中尝试提取
            maxSizeStr = extractSizeFromMessage(e.getMessage());
        }

        // 场景D：以上都提取不到，用配置值兜底
        if (maxSizeStr == null) {
            maxSizeStr = configuredMaxFileSize;
        }

        String message = "文件大小超过限制，最大允许 " + maxSizeStr;
        log.warn("文件上传大小超限: {} (max={})", e.getMessage(), maxSizeStr);
        return Response.error(ResponseCode.FILE_SIZE_EXCEEDED.getCode(), message);
    }

    /**
     * 捕获其他 Multipart 异常（如请求体过大、解析失败等）
     *
     * @param e MultipartException
     * @return 错误响应
     */
    @ExceptionHandler(MultipartException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Response<Void> handleMultipartException(MultipartException e) {
        log.warn("文件上传异常: {}", e.getMessage());
        return Response.error(ResponseCode.BAD_REQUEST.getCode(), "文件上传失败: " + e.getMessage());
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

    /**
     * 将字节数格式化为人类可读的文件大小字符串
     *
     * @param bytes 字节数
     * @return 格式化字符串，如 "5 MB"
     */
    private static String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.0f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.0f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    /** 从异常消息中提取文件大小限制，支持 "5 MB"、"10485760 bytes" 等格式 */
    private static final Pattern SIZE_PATTERN = Pattern.compile("(\\d+(\\.\\d+)?)\\s*(KB|MB|GB|bytes)", Pattern.CASE_INSENSITIVE);

    /**
     * 从错误消息中尝试提取文件大小限制的文本描述
     *
     * @param message 异常消息
     * @return 格式化后的大小字符串（如 "5 MB"），或 null 如果无法提取
     */
    private static String extractSizeFromMessage(String message) {
        if (message == null || message.isEmpty()) return null;
        Matcher matcher = SIZE_PATTERN.matcher(message);
        if (matcher.find()) {
            String valueStr = matcher.group(1);
            String unit = matcher.group(3).toLowerCase();
            // 如果单位是 bytes，直接拼接
            if ("bytes".equals(unit)) {
                try {
                    long bytes = Long.parseLong(valueStr);
                    return formatFileSize(bytes);
                } catch (NumberFormatException ignored) {
                    // fall through
                }
            }
            // unit 是 KB/MB/GB，直接使用原文
            return valueStr + " " + unit.toUpperCase();
        }
        return null;
    }
}
