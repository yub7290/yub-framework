package com.yub.framework.log;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 日志内存缓冲队列
 * <p>
 * 提供两个独立阻塞队列分别缓冲访问日志与操作日志事件，
 * 由 LogBatchProcessor 定时批量 drain 后写入数据库。
 * 队列满时非阻塞降级（不阻塞业务线程）。
 * </p>
 *
 * @Author: bing.yu
 * @CreateTime: 2026-06-28
 * @Description: 日志内存缓冲队列
 * @Version: 1.0.0
 */
@Slf4j
@Component
public class LogBuffer {

    /** 访问日志队列容量，默认 8192 */
    @Value("${log.buffer.access-log-capacity:8192}")
    private int accessLogCapacity;

    /** 操作日志队列容量，默认 4096 */
    @Value("${log.buffer.opera-log-capacity:4096}")
    private int operaLogCapacity;

    private LinkedBlockingQueue<AccessLogEvent> accessLogQueue;
    private LinkedBlockingQueue<OperaLogEvent> operaLogQueue;

    /** 初始化队列（由 Spring 在属性注入后调用） */
    // @PostConstruct 由 setter 注入替代

    @Override
    public String toString() {
        return "LogBuffer{" +
                "accessLogQueue.size=" + (accessLogQueue != null ? accessLogQueue.size() : "uninitialized") +
                ", operaLogQueue.size=" + (operaLogQueue != null ? operaLogQueue.size() : "uninitialized") +
                '}';
    }

    // ==================== 访问日志队列 ====================

    /**
     * 获取访问日志队列实例（懒初始化）
     */
    private LinkedBlockingQueue<AccessLogEvent> getAccessLogQueue() {
        if (accessLogQueue == null) {
            accessLogQueue = new LinkedBlockingQueue<>(accessLogCapacity);
        }
        return accessLogQueue;
    }

    /**
     * 非阻塞写入访问日志事件
     *
     * @param event 访问日志事件
     * @return true=入队成功, false=队列满已降级
     */
    public boolean offerAccessLog(AccessLogEvent event) {
        boolean offered = getAccessLogQueue().offer(event);
        if (!offered) {
            log.warn("访问日志队列已满（容量={}），丢弃1条访问日志", accessLogCapacity);
        }
        return offered;
    }

    /**
     * 尝试在超时时间内写入访问日志事件
     *
     * @param event 访问日志事件
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return true=入队成功, false=超时降级
     */
    public boolean offerAccessLog(AccessLogEvent event, long timeout, TimeUnit unit) {
        try {
            boolean offered = getAccessLogQueue().offer(event, timeout, unit);
            if (!offered) {
                log.warn("访问日志队列已满，超时{} {}后丢弃1条", timeout, unit);
            }
            return offered;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("访问日志入队被中断", e);
            return false;
        }
    }

    /**
     * 批量 drain 访问日志（非阻塞）
     *
     * @param maxElements 最大取出条数
     * @return 取出的访问日志事件列表
     */
    public List<AccessLogEvent> drainAccessLogs(int maxElements) {
        List<AccessLogEvent> list = new ArrayList<>(Math.min(maxElements, 512));
        getAccessLogQueue().drainTo(list, maxElements);
        return list;
    }

    /**
     * 获取访问日志队列当前积压量
     */
    public int getAccessLogQueueSize() {
        return getAccessLogQueue().size();
    }

    // ==================== 操作日志队列 ====================

    /**
     * 获取操作日志队列实例（懒初始化）
     */
    private LinkedBlockingQueue<OperaLogEvent> getOperaLogQueue() {
        if (operaLogQueue == null) {
            operaLogQueue = new LinkedBlockingQueue<>(operaLogCapacity);
        }
        return operaLogQueue;
    }

    /**
     * 非阻塞写入操作日志事件
     *
     * @param event 操作日志事件
     * @return true=入队成功, false=队列满已降级
     */
    public boolean offerOperaLog(OperaLogEvent event) {
        boolean offered = getOperaLogQueue().offer(event);
        if (!offered) {
            log.warn("操作日志队列已满（容量={}），丢弃1条操作日志", operaLogCapacity);
        }
        return offered;
    }

    /**
     * 批量 drain 操作日志（非阻塞）
     *
     * @param maxElements 最大取出条数
     * @return 取出的操作日志事件列表
     */
    public List<OperaLogEvent> drainOperaLogs(int maxElements) {
        List<OperaLogEvent> list = new ArrayList<>(Math.min(maxElements, 512));
        getOperaLogQueue().drainTo(list, maxElements);
        return list;
    }

    /**
     * 获取操作日志队列当前积压量
     */
    public int getOperaLogQueueSize() {
        return getOperaLogQueue().size();
    }

    // ==================== 通用 ====================

    /**
     * 应用关闭时尝试排空剩余日志（最多等待5秒）
     */
    @PreDestroy
    public void flushOnShutdown() {
        log.info("应用关闭，尝试排空日志缓冲区...");
        // 由 LogBatchProcessor 的 @PreDestroy 处理实际持久化
        log.info("当前缓冲状态: accessLogQueue={}, operaLogQueue={}",
                getAccessLogQueueSize(), getOperaLogQueueSize());
    }
}
