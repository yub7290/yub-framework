package com.yub.framework.security;

import com.yub.common.exception.BaseException;
import com.yub.framework.exception.FrameworkErrorCode;
import com.yub.framework.exception.FrameworkException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @Author: bing.yu
 * @CreateTime: 2026-06-11
 * @Description: security工具类，支持 ADMIN/TEACHER 双身份
 * @Version: 1.1.0
 */
public class SecurityUtils {

    /**
     * 获取当前登录用户ID
     * <p>解析 Subject 中的数字部分（TYPE:ID 格式），向后兼容纯数字 Subject
     *
     * @return 用户ID
     * @throws FrameworkException 未认证时抛出 UNAUTHORIZED 异常
     */
    public static Long getCurrentUserId() {
        String subject = extractSubject();
        String userId = parseSubjectUserId(subject);
        try {
            return Long.valueOf(userId);
        } catch (NumberFormatException e) {
            throw new FrameworkException(FrameworkErrorCode.UNAUTHORIZED);
        }
    }

    /**
     * 获取当前登录用户类型
     * <p>解析 Subject 中的类型前缀（TYPE:ID 格式），向后兼容默认 ADMIN
     *
     * @return 用户类型（ADMIN / TEACHER）
     */
    public static String getCurrentUserType() {
        String subject = extractSubject();
        return parseSubjectUserType(subject);
    }

    /**
     * 提取 SecurityContext 中的 subject
     */
    private static String extractSubject() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            throw new FrameworkException(FrameworkErrorCode.UNAUTHORIZED);
        }
        return auth.getName();
    }

    /**
     * 从 Subject 中解析用户ID（冒号后的部分）
     */
    private static String parseSubjectUserId(String subject) {
        int idx = subject.indexOf(":");
        return idx > 0 ? subject.substring(idx + 1) : subject;
    }

    /**
     * 从 Subject 中解析用户类型（冒号前的部分）
     * 向后兼容：无冒号时默认 ADMIN
     */
    private static String parseSubjectUserType(String subject) {
        int idx = subject.indexOf(":");
        return idx > 0 ? subject.substring(0, idx) : JwtProvider.USER_TYPE_ADMIN;
    }
}
