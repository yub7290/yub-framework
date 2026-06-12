package com.yub.framework.security;

import com.yub.common.constant.RedisKeyConstants;
import com.yub.framework.redis.RedisUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器
 *
 * @Author: bing.yu
 * @CreateTime: 2026-06-10
 * @Description: 从请求头提取 JWT Token 并认证
 * @Version: 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserDetailsService userDetailsService;

    /**
     * 过滤请求：提取Token → 检查黑名单（轻量）→ 解析并验证（仅一次）→ 设置SecurityContext
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = jwtProvider.getToken(request);
        if (StringUtils.isNotBlank(token)) {
            if (isBlacklisted(token)) {
                filterChain.doFilter(request, response);
                return;
            }
            Claims claims = jwtProvider.parseTokenIfValid(token);
            if (claims != null) {
                try {
                    String userId = claims.getSubject();
                    UserDetails userDetails = userDetailsService.loadUserByUsername(userId);
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } catch (Exception e) {
                    log.debug("JWT认证失败: {}", e.getMessage());
                    SecurityContextHolder.clearContext();
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean isBlacklisted(String token) {
        String tokenHash = DigestUtils.sha256Hex(token);
        return RedisUtils.exists(RedisKeyConstants.TOKEN_BLACKLIST_PREFIX + tokenHash);
    }
}
