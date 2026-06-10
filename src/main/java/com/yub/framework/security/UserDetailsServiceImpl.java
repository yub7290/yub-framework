package com.yub.framework.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security UserDetailsService 实现
 *
 * @Author: bing.yu
 * @CreateTime: 2026-06-10
 * @Description: 用户详情服务
 * @Version: 1.0.0
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserDetailsLoader userDetailsLoader;

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        return userDetailsLoader.loadByUserId(Long.valueOf(userId));
    }
}
