package com.yub.framework.exception;

import com.yub.common.exception.BaseException;

/**
 * 系统框架异常
 *
 * @Author: bing.yu
 * @CreateTime: 2026-06-10
 * @Description: 系统框架异常
 * @Version: 1.0
 */
public class FrameworkException extends BaseException {
    public FrameworkException(FrameworkErrorCode errorCode) {
        super(errorCode);
    }

    public FrameworkException(FrameworkErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
