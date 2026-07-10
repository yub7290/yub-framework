package com.yub.framework.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT 令牌提供器
 *
 * @Author: bing.yu
 * @CreateTime: 2026-06-10
 * @Description: JWT 令牌生成与解析，支持 ADMIN/TEACHER 双身份
 * @Version: 1.1.0
 */
@Component
public class JwtProvider {

    /** Bearer Token 前缀 */
    public static final String BEARER_PREFIX = "Bearer ";

    /** 用户类型常量 */
    public static final String USER_TYPE_ADMIN = "ADMIN";
    public static final String USER_TYPE_TEACHER = "TEACHER";
    public static final String USER_TYPE_STUDENT = "STUDENT";

    /** Subject 分隔符：TYPE:ID */
    private static final String SUBJECT_SEPARATOR = ":";

    private final SecretKey secretKey;
    private final long accessExpiration;
    private final long refreshExpiration;

    /**
     * 构造 JWT 提供器，使用 HMAC-SHA 签名算法
     *
     * @param secret            签名密钥
     * @param accessExpiration  AccessToken 过期时间（毫秒，默认2小时）
     * @param refreshExpiration RefreshToken 过期时间（毫秒，默认7天）
     */
    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiration:7200000}") long accessExpiration,
            @Value("${jwt.refresh-expiration:604800000}") long refreshExpiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
    }

    /**
     * 生成 AccessToken（指定用户类型）
     *
     * @param userId   用户ID
     * @param account  账号
     * @param userType 用户类型（ADMIN/TEACHER）
     * @param claims   额外声明
     * @return AccessToken
     */
    public String generateAccessToken(Long userId, String account, String userType, Map<String, Object> claims) {
        String subject = buildSubject(userType, userId);
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .claim("account", account)
                .claim("user_type", userType)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessExpiration))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 生成 AccessToken（默认 ADMIN 类型，向后兼容）
     */
    public String generateAccessToken(Long userId, String account, Map<String, Object> claims) {
        return generateAccessToken(userId, account, USER_TYPE_ADMIN, claims);
    }

    /**
     * 生成 RefreshToken（指定用户类型）
     *
     * @param userId   用户ID
     * @param userType 用户类型
     * @return RefreshToken
     */
    public String generateRefreshToken(Long userId, String userType) {
        return Jwts.builder()
                .subject(buildSubject(userType, userId))
                .claim("user_type", userType)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 生成 RefreshToken（默认 ADMIN 类型，向后兼容）
     */
    public String generateRefreshToken(Long userId) {
        return generateRefreshToken(userId, USER_TYPE_ADMIN);
    }

    /**
     * 解析 Token
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 验证 Token 是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 解析 Token 并返回 Claims，无效时返回 null
     *
     * @param token JWT Token
     * @return Claims 或 null
     */
    public Claims parseTokenIfValid(String token) {
        try {
            return parseToken(token);
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 从 Token 中获取用户ID（向后兼容，返回纯数字部分）
     */
    public String getUserId(String token) {
        return parseSubjectUserId(parseToken(token).getSubject());
    }

    /**
     * 从 Claims 中获取用户类型
     *
     * @param claims JWT Claims
     * @return 用户类型（ADMIN/TEACHER），无则默认 ADMIN
     */
    public String getUserType(Claims claims) {
        String type = claims.get("user_type", String.class);
        if (type != null) {
            return type;
        }
        // 向后兼容：从 subject 解析
        return parseSubjectUserType(claims.getSubject());
    }

    /**
     * 从 Authorization 请求头提取 Bearer Token
     *
     * @param request HTTP 请求
     * @return Token 字符串，无则返回 null
     */
    public String getToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.isNotBlank(header) && header.startsWith(JwtProvider.BEARER_PREFIX)) {
            return header.substring(JwtProvider.BEARER_PREFIX.length());
        }
        return null;
    }

    // ========== private helpers ==========

    /**
     * 构建 Subject：TYPE:ID
     */
    private String buildSubject(String userType, Long userId) {
        return userType + SUBJECT_SEPARATOR + userId;
    }

    /**
     * 从 Subject 中解析用户类型
     * 向后兼容：无分隔符时默认 ADMIN
     */
    private String parseSubjectUserType(String subject) {
        if (subject == null) {
            return USER_TYPE_ADMIN;
        }
        int idx = subject.indexOf(SUBJECT_SEPARATOR);
        return idx > 0 ? subject.substring(0, idx) : USER_TYPE_ADMIN;
    }

    /**
     * 从 Subject 中解析用户ID
     * 向后兼容：无分隔符时直接返回整个 subject
     */
    private String parseSubjectUserId(String subject) {
        if (subject == null) {
            return subject;
        }
        int idx = subject.indexOf(SUBJECT_SEPARATOR);
        return idx > 0 ? subject.substring(idx + 1) : subject;
    }
}
