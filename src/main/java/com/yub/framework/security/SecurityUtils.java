package com.yub.framework.security;

import com.yub.common.exception.BaseException;
import com.yub.framework.exception.FrameworkErrorCode;
import com.yub.framework.exception.FrameworkException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @Author: bing.yu
 * @CreateTime: 2026-06-11
 * @Description: security工具类
 * @Version: 1.1
 */
public class SecurityUtils {

    /**
     * 获取当前登录用户ID
     *
     * @return 用户ID
     * @throws BaseException 未认证时抛出 UNAUTHORIZED 异常
     */
    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            throw new FrameworkException(FrameworkErrorCode.UNAUTHORIZED);
        }
        try {
            return Long.valueOf(auth.getName());
        } catch (NumberFormatException e) {
            throw new FrameworkException(FrameworkErrorCode.UNAUTHORIZED);
        }
    }

}
