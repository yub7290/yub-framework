package com.yub.framework.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yub.common.enums.ResponseCode;
import com.yub.common.model.Response;
import com.yub.framework.util.JsonUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * JWT 认证入口点
 *
 * @Author: bing.yu
 * @CreateTime: 2026-06-10
 * @Description: 处理未认证请求，返回 JSON 格式错误
 * @Version: 1.0.0
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /**
     * 认证失败处理：返回 401 JSON 错误响应
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        Response<Void> result = Response.error(ResponseCode.UNAUTHORIZED);
        response.getWriter().write(JsonUtils.toJson(result));
    }
}
