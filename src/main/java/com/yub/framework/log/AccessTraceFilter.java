package com.yub.framework.log;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * TraceId 链路追踪过滤器
 * <p>
 * 为每个 HTTP 请求分配唯一 traceId，注入 SLF4J MDC 以便日志输出，
 * 响应头携带 X-Trace-Id 方便前端/下游透传。
 * 此过滤器应最先执行，早于所有认证与业务过滤器。
 * </p>
 *
 * @Author: bing.yu
 * @CreateTime: 2026-06-28
 * @Description: TraceId 链路追踪过滤器
 * @Version: 1.0.0
 */
@Slf4j
@Component
@Order(1)
public class AccessTraceFilter extends OncePerRequestFilter {

    /** 请求头名称：上游透传的 traceId */
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    /** 请求属性中存储 traceId 的 key */
    private static final String TRACE_ID_ATTR = "_traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. 获取或生成 traceId
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = generateTraceId();
        }

        // 2. 注入 MDC
        MDC.put("traceId", traceId);

        // 3. 存入请求属性供后续代码获取
        request.setAttribute(TRACE_ID_ATTR, traceId);

        // 4. 响应头带回 traceId
        response.setHeader(TRACE_ID_HEADER, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // 5. 清理 MDC，防止线程复用导致 traceId 污染
            MDC.clear();
        }
    }

    /**
     * 生成唯一 traceId（UUID 简写，取前8位+时间戳后缀）
     */
    private static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8)
                + System.currentTimeMillis();
    }
}
