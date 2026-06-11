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
 * @Description: JWT 令牌生成与解析
 * @Version: 1.0.0
 */
@Component
public class JwtProvider {

    /** Bearer Token 前缀 */
    public static final String BEARER_PREFIX = "Bearer ";

    private final SecretKey secretKey;
    private final long accessExpiration;
    private final long refreshExpiration;

    /**
     * 构造 JWT 提供器，使用 HMAC-SHA 签名算法
     *
     * @param secret            签名密钥
     * @param accessExpiration  AccessToken 过期时间（毫秒，默认30分钟）
     * @param refreshExpiration RefreshToken 过期时间（毫秒，默认7天）
     */
    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiration:1800000}") long accessExpiration,
            @Value("${jwt.refresh-expiration:604800000}") long refreshExpiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
    }

    /**
     * 生成 AccessToken
     */
    public String generateAccessToken(Long userId, String account, Map<String, Object> claims) {
        return Jwts.builder()
                .claims(claims)
                .subject(String.valueOf(userId))
                .claim("account", account)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessExpiration))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 生成 RefreshToken
     */
    public String generateRefreshToken(Long userId) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(secretKey)
                .compact();
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
     * 解析 Token 并返回 Claims，无效时返回 null（仅解析一次，避免重复签名验证）
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
     * 从 Token 中获取用户ID
     */
    public String getUserId(String token) {
        return parseToken(token).getSubject();
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
}
