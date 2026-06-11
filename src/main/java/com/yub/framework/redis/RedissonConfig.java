package com.yub.framework.redis;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 配置
 *
 * @Author: bing.yu
 * @CreateTime: 2026-06-10
 * @Description: Redisson 客户端配置
 * @Version: 1.0.0
 */
@Configuration
public class RedissonConfig {

    /**
     * Redis 主机地址
     */
    @Value("${spring.data.redis.host:localhost}")
    private String host;

    /**
     * Redis 端口号
     */
    @Value("${spring.data.redis.port:6379}")
    private int port;

    /**
     * Redis 密码
     */
    @Value("${spring.data.redis.password:}")
    private String password;

    /**
     * Redis 数据库索引
     */
    @Value("${spring.data.redis.database:0}")
    private int database;

    /**
     * 创建 RedissonClient 实例（单节点模式）
     *
     * @return RedissonClient
     */
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        String address = "redis://" + host + ":" + port;
        config.useSingleServer()
                .setAddress(address)
                .setDatabase(database)
                .setConnectionPoolSize(32)
                .setConnectionMinimumIdleSize(8)
                .setTimeout(3000)
                .setConnectTimeout(3000)
                .setRetryAttempts(3)
                .setRetryInterval(1500);
        if (password != null && !password.isEmpty()) {
            config.useSingleServer().setPassword(password);
        }
        return Redisson.create(config);
    }
}
