package com.yub.framework.exception;

import com.yub.common.exception.BaseException;
import com.yub.common.exception.ErrorCode;

/**
 * 系统框架异常
 *
 * @Author: bing.yu
 * @CreateTime: 2026-06-10
 * @Description: 系统框架异常
 * @Version: 1.0
 */
public class FrameworkException extends BaseException {
    public FrameworkException(ErrorCode errorCode) {
        super(errorCode);
    }

    public FrameworkException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public FrameworkException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }

    public FrameworkException(Throwable cause, ErrorCode errorCode, Object... args) {
        super(cause, errorCode, args);
    }
}
