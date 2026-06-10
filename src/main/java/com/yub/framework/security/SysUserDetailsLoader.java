package com.yub.framework.security;

import org.springframework.security.core.userdetails.UserDetails;

/**
 * 用户详情加载器接口（由 yub-system 模块注入实现）
 *
 * @Author: bing.yu
 * @CreateTime: 2026-06-10
 * @Description: 用户详情加载器接口
 * @Version: 1.0.0
 */
public interface SysUserDetailsLoader {

    /**
     * 根据用户ID加载用户详情
     */
    UserDetails loadByUserId(Long userId);
}
