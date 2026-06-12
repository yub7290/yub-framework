package com.yub.framework.exception;

import com.yub.common.exception.ErrorCode;
import lombok.AllArgsConstructor;

/**
 * @Author: bing.yu
 * @CreateTime: 2026-06-10
 * @Description: 系统框架异常码
 * @Version: 1.0
 */
@AllArgsConstructor
public enum FrameworkErrorCode implements ErrorCode {
    JSON_PARSE_ERROR(1000001, "JSON解析错误"),
    BEAN_COPY_ERROR(1000002, "Bean拷贝错误：%s -> %s"),
    UNAUTHORIZED(1000003, "未授权"),
    ;

    private final int code;
    private final String message;

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
