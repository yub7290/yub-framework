package com.yub.framework.log;

import com.yub.framework.security.SecurityUtils;
import com.yub.framework.util.IpUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 访问日志采集过滤器
 * <p>
 * 自动采集所有 HTTP 请求的访问日志（URL、Method、IP、UA、耗时、状态码等），
 * 异步写入 LogBuffer 内存队列，不阻塞业务线程。
 * 未认证请求的 userId 为 null，仅记录基础访问信息。
 * </p>
 *
 * @Author: bing.yu
 * @CreateTime: 2026-06-28
 * @Description: 访问日志采集过滤器
 * @Version: 1.0.0
 */
@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 100)
@RequiredArgsConstructor
public class AccessLogFilter extends OncePerRequestFilter {

    private final LogBuffer logBuffer;

    /** 不记录访问日志的路径前缀 */
    private static final String[] EXCLUDED_PREFIXES = {
            "/favicon.ico", "/static/", "/webjars/"
    };

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        for (String prefix : EXCLUDED_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        try {
            filterChain.doFilter(request, response);
        } finally {
            try {
                long costTime = System.currentTimeMillis() - startTime;

                AccessLogEvent event = new AccessLogEvent();
                event.setUrl(request.getRequestURI());
                event.setMethod(request.getMethod());
                event.setIp(IpUtils.getClientIp(request));
                event.setUserAgent(request.getHeader("User-Agent"));
                event.setStatus(response.getStatus() < 400 ? 1 : 0);
                event.setCostTime((int) costTime);

                if (response.getStatus() >= 400) {
                    event.setErrorMsg("HTTP " + response.getStatus());
                }

                // 尝试获取当前登录用户（未认证时不抛出异常）
                try {
                    event.setUserId(SecurityUtils.getCurrentUserId());
                } catch (Exception ignored) {
                    // 未登录用户或不走 SecurityContext 的请求
                }

                // 非阻塞入队
                logBuffer.offerAccessLog(event);

            } catch (Exception e) {
                log.warn("采集访问日志异常: {}", e.getMessage());
            }
        }
    }
}
