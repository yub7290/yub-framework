package com.yub.framework.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * IP工具类
 *
 * @Author: bing.yu
 * @CreateTime: 2026-06-10
 * @Description: IP工具类
 * @Version: 1.0
 */
public class IpUtils {

    /**
     * 获取客户端IP地址
     * @param request HTTP请求
     * @return 客户端IP地址
     */
    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
