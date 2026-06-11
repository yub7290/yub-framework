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

    /**
     * 根据用户ID加载Spring Security用户详情
     *
     * @param userId 用户ID（字符串，内部转为Long）
     * @return UserDetails
     * @throws UsernameNotFoundException 用户不存在
     */
    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        try {
            return userDetailsLoader.loadByUserId(Long.valueOf(userId));
        } catch (NumberFormatException e) {
            throw new UsernameNotFoundException("无效的用户ID: " + userId);
        }
    }
}
