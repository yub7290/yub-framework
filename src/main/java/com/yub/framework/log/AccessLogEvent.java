package com.yub.framework.log;

import lombok.Data;

/**
 * 访问日志事件（内存队列传输对象，非持久化实体）
 *
 * @Author: bing.yu
 * @CreateTime: 2026-06-28
 * @Description: 访问日志事件，由 AccessLogFilter 采集后放入内存队列，
 *               最终由 LogBatchProcessor 转换为 SysAccessLog 实体写入数据库
 * @Version: 1.0.0
 */
@Data
public class AccessLogEvent {

    /** 用户ID（未登录为 null） */
    private Long userId;

    /** 用户昵称 */
    private String nickName;

    /** 客户端IP */
    private String ip;

    /** HTTP 方法 */
    private String method;

    /** 请求 URL */
    private String url;

    /** 浏览器 User-Agent */
    private String userAgent;

    /** 事件类型（默认 REQUEST） */
    private String eventType = "REQUEST";

    /** HTTP 状态码（1-成功 0-失败） */
    private Integer status;

    /** 错误信息 */
    private String errorMsg;

    /** 耗时（ms） */
    private Integer costTime;
}
