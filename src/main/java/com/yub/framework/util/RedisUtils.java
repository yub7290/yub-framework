package com.yub.framework.util;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Redis 缓存操作工具类，基于 Redisson 封装常用缓存操作
 *
 * @Author: bing.yu
 * @CreateTime: 2026-06-11
 * @Description: 提供 String / 泛型对象的存取、过期管理、原子计数等功能
 * @Version: 1.1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisUtils {

    private static RedissonClient redissonClient;

    private final RedissonClient redissonClientInstance;

    @PostConstruct
    public void init() {
        redissonClient = redissonClientInstance;
    }

    /**
     * 设置泛型对象缓存（带过期时间）
     *
     * @param key            缓存键
     * @param value          缓存值
     * @param timeoutMinutes 过期时间（分钟）
     */
    public static <T> void set(String key, T value, int timeoutMinutes) {
        Objects.requireNonNull(key, "redis key 不能为 null");
        try {
            RBucket<T> bucket = redissonClient.getBucket(key);
            bucket.set(value, Duration.ofMinutes(timeoutMinutes));
        } catch (Exception e) {
            log.error("Redis set failed, key={}", key, e);
        }
    }

    /**
     * 设置泛型对象缓存（带过期时间）
     *
     * @param key         缓存键
     * @param value       缓存值
     * @param timeoutDuration 过期时间（Duration）
     */
    public static <T> void set(String key, T value, Duration timeoutDuration) {
        Objects.requireNonNull(key, "redis key 不能为 null");
        try {
            RBucket<T> bucket = redissonClient.getBucket(key);
            bucket.set(value, timeoutDuration);
        } catch (Exception e) {
            log.error("Redis set failed, key={}", key, e);
        }
    }

    /**
     * 获取泛型对象缓存
     *
     * @param key 缓存键
     * @return Optional 包装的缓存对象
     */
    @SuppressWarnings("unchecked")
    public static <T> Optional<T> get(String key) {
        Objects.requireNonNull(key, "redis key 不能为 null");
        try {
            RBucket<T> bucket = (RBucket<T>) redissonClient.getBucket(key);
            return Optional.ofNullable(bucket.get());
        } catch (Exception e) {
            log.error("Redis get failed, key={}", key, e);
            return Optional.empty();
        }
    }

    // ==================== 键管理 ====================

    /**
     * 删除缓存
     *
     * @param key 缓存键
     */
    public static void delete(String key) {
        Objects.requireNonNull(key, "redis key 不能为 null");
        try {
            redissonClient.getBucket(key).delete();
        } catch (Exception e) {
            log.error("Redis delete failed, key={}", key, e);
        }
    }

    /**
     * 判断缓存键是否存在
     *
     * @param key 缓存键
     * @return true 存在
     */
    public static boolean exists(String key) {
        Objects.requireNonNull(key, "redis key 不能为 null");
        try {
            return redissonClient.getBucket(key).isExists();
        } catch (Exception e) {
            log.error("Redis exists check failed, key={}", key, e);
            return false;
        }
    }

    /**
     * 设置缓存过期时间
     *
     * @param key            缓存键
     * @param timeoutMinutes 过期时间（分钟）
     * @return true 设置成功
     */
    public static boolean expire(String key, int timeoutMinutes) {
        Objects.requireNonNull(key, "redis key 不能为 null");
        try {
            return redissonClient.getBucket(key).expire(Duration.ofMinutes(timeoutMinutes));
        } catch (Exception e) {
            log.error("Redis expire failed, key={}", key, e);
            return false;
        }
    }

    /**
     * 获取缓存剩余存活时间
     *
     * @param key 缓存键
     * @return Optional 包装的剩余毫秒数，-1 表示未设置过期，-2 表示键不存在
     */
    public static Optional<Long> getExpire(String key) {
        Objects.requireNonNull(key, "redis key 不能为 null");
        try {
            return Optional.of(redissonClient.getBucket(key).remainTimeToLive());
        } catch (Exception e) {
            log.error("Redis getExpire failed, key={}", key, e);
            return Optional.empty();
        }
    }

    // ==================== 原子计数 ====================

    /**
     * 原子自增（默认 +1）
     *
     * @param key 缓存键
     * @return 自增后的值
     */
    public static long increment(String key) {
        return increment(key, 1);
    }

    /**
     * 原子自增（指定步长）
     *
     * @param key   缓存键
     * @param delta 步长（正数增加，负数减少）
     * @return 操作后的值
     */
    public static long increment(String key, long delta) {
        Objects.requireNonNull(key, "redis key 不能为 null");
        try {
            return redissonClient.getAtomicLong(key).addAndGet(delta);
        } catch (Exception e) {
            log.error("Redis increment failed, key={}", key, e);
            return 0;
        }
    }

    /**
     * 原子自减（默认 -1）
     *
     * @param key 缓存键
     * @return 自减后的值
     */
    public static long decrement(String key) {
        return increment(key, -1);
    }

    /**
     * 原子自减（指定步长）
     *
     * @param key   缓存键
     * @param delta 步长
     * @return 操作后的值
     */
    public static long decrement(String key, long delta) {
        return increment(key, -delta);
    }

    // ==================== 条件写入 ====================

    /**
     * 仅当键不存在时设置缓存（分布式锁场景）
     *
     * @param key            缓存键
     * @param value          缓存值
     * @param timeoutMinutes 过期时间（分钟）
     * @return true 设置成功
     */
    public static boolean setIfAbsent(String key, String value, int timeoutMinutes) {
        Objects.requireNonNull(key, "redis key 不能为 null");
        try {
            RBucket<String> bucket = redissonClient.getBucket(key);
            return bucket.setIfAbsent(value, Duration.ofMinutes(timeoutMinutes));
        } catch (Exception e) {
            log.error("Redis setIfAbsent failed, key={}", key, e);
            return false;
        }
    }
}
