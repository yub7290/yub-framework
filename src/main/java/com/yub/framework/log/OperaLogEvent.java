package com.yub.framework.log;

import lombok.Data;

/**
 * 操作日志事件（内存队列传输对象，非持久化实体）
 *
 * @Author: bing.yu
 * @CreateTime: 2026-06-28
 * @Description: 操作日志事件，由 OperaLogAspect 采集后放入内存队列，
 *               最终由 LogBatchProcessor 转换为 SysOperaLog 实体写入数据库
 * @Version: 1.0.0
 */
@Data
public class OperaLogEvent {

    /** 操作人ID */
    private Long userId;

    /** 操作人用户名 */
    private String nickName;

    /** 模块（如 dict/role/course） */
    private String module;

    /** 操作类型（CREATE/UPDATE/DELETE/QUERY/IMPORT/EXPORT） */
    private String operaType;

    /** 操作内容摘要 */
    private String content;

    /** 方法全名（包名+类名+方法名） */
    private String methodName;

    /** 请求参数（JSON 脱敏） */
    private String requestParams;

    /** 响应结果（JSON 截断） */
    private String responseResult;

    /** 错误信息 */
    private String errorMsg;

    /** 客户端IP */
    private String ip;

    /** 耗时（ms） */
    private Integer costTime;
}
