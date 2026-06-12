package com.yub.framework.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WebMvc 配置
 *
 * @Author: bing.yu
 * @CreateTime: 2026-06-10
 * @Description: WebMvc 跨域配置
 * @Version: 1.0.0
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 全局跨域配置
     * <p>开发环境使用 Vite 代理转发 API 请求，跨域由代理处理；
     * 生产环境建议将前端静态资源部署到同域或通过反向代理统一域名。
     * 使用 allowedOriginPatterns 精确指定允许的源，避免通配符 + 凭据组合。
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
