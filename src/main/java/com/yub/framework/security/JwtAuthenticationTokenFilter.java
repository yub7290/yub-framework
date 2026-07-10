package com.yub.framework.security;

import com.yub.common.constant.RedisKeyConstants;
import com.yub.framework.redis.RedisUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
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
 * @Description: 从请求头提取 JWT Token 并认证，支持 ADMIN/TEACHER/STUDENT 三身份分发
 * @Version: 1.3.0
 */
@Slf4j
@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter implements ApplicationContextAware {

    private final JwtProvider jwtProvider;
    private ApplicationContext applicationContext;

    public JwtAuthenticationTokenFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        this.applicationContext = ctx;
    }

    /**
     * 过滤请求：提取Token → 检查黑名单 → 解析并验证 → 按用户类型分发加载 → 设置SecurityContext
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
                    String userType = jwtProvider.getUserType(claims);
                    String rawSubject = claims.getSubject();
                    // ponytail: strip TYPE: prefix → loader 只接收纯数字 ID
                    String userId = rawSubject.contains(":") ? rawSubject.substring(rawSubject.indexOf(":") + 1) : rawSubject;
                    log.debug("JWT解析: userType={}, userId={}, rawSubject={}", userType, userId, rawSubject);
                    UserDetailsService loader = resolveLoader(userType);
                    log.debug("Loader resolved: {}", loader.getClass().getSimpleName());
                    UserDetails userDetails = loader.loadUserByUsername(userId);
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

    /**
     * 根据用户类型选择对应的 UserDetailsService
     * <p>通过 ApplicationContext.getBean() 按名称查找，避免 Map 注入失败
     */
    private UserDetailsService resolveLoader(String userType) {
        String beanName;
        if (JwtProvider.USER_TYPE_TEACHER.equals(userType)) {
            beanName = "teacherUserDetailsService";
        } else if (JwtProvider.USER_TYPE_STUDENT.equals(userType)) {
            beanName = "studentUserDetailsService";
        } else {
            beanName = "adminUserDetailsService";
        }
        try {
            return applicationContext.getBean(beanName, UserDetailsService.class);
        } catch (Exception e) {
            log.warn("UserDetailsService '{}' 未找到: {}", beanName, e.getMessage());
            // fallback: 尝试 admin
            String fallback = "adminUserDetailsService".equals(beanName)
                    ? "teacherUserDetailsService" : "adminUserDetailsService";
            return applicationContext.getBean(fallback, UserDetailsService.class);
        }
    }

    private boolean isBlacklisted(String token) {
        String tokenHash = DigestUtils.sha256Hex(token);
        return RedisUtils.exists(RedisKeyConstants.TOKEN_BLACKLIST_PREFIX + tokenHash);
    }
}
