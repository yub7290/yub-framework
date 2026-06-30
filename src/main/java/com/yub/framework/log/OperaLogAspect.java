package com.yub.framework.log;

import com.yub.common.annotation.Log;
import com.yub.framework.security.SecurityUtils;
import com.yub.framework.util.IpUtils;
import com.yub.framework.util.JsonUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 操作日志 AOP 切面
 * <p>
 * 拦截所有标注了 {@link Log} 注解的 Controller 方法，
 * 自动采集操作内容、请求参数、响应结果、耗时等信息，
 * 异步写入 LogBuffer 内存队列。
 * </p>
 *
 * @Author: bing.yu
 * @CreateTime: 2026-06-28
 * @Description: 操作日志 AOP 切面
 * @Version: 1.0.0
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperaLogAspect {

    private final LogBuffer logBuffer;

    /** 敏感字段名集合（不区分大小写） */
    private static final List<Pattern> SENSITIVE_PATTERNS = Arrays.asList(
            Pattern.compile("password", Pattern.CASE_INSENSITIVE),
            Pattern.compile("secret", Pattern.CASE_INSENSITIVE),
            Pattern.compile("token", Pattern.CASE_INSENSITIVE),
            Pattern.compile("authorization", Pattern.CASE_INSENSITIVE)
    );

    /** 手机号脱敏正则 */
    private static final Pattern PHONE_PATTERN = Pattern.compile("(1[3-9]\\d)\\d{4}(\\d{4})");

    /** 身份证号脱敏正则 */
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("(\\d{6})\\d{8}(\\d{4}[0-9Xx])");

    /** 响应结果最大长度 */
    private static final int MAX_RESPONSE_LENGTH = 2000;

    @Around("@annotation(com.yub.common.annotation.Log)")
    public Object aroundLog(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取注解和方法信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Log logAnnotation = method.getAnnotation(Log.class);

        // 构建事件对象
        OperaLogEvent event = new OperaLogEvent();
        event.setContent(logAnnotation.value());
        event.setOperaType(logAnnotation.type());
        event.setMethodName(method.getDeclaringClass().getName() + "." + method.getName());
        event.setModule(resolveModule(method));

        // 获取当前用户（非登录态可空）
        try {
            event.setUserId(SecurityUtils.getCurrentUserId());
        } catch (Exception ignored) {
            // 未登录用户
        }

        // 获取请求参数（脱敏）
        try {
            Object[] args = joinPoint.getArgs();
            String params = argsToJson(args);
            event.setRequestParams(desensitize(params));
        } catch (Exception e) {
            log.trace("序列化请求参数异常: {}", e.getMessage());
            event.setRequestParams("[]");
        }

        // 获取 IP
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                event.setIp(IpUtils.getClientIp(request));
            }
        } catch (Exception ignored) {
            // 无法获取 IP 时忽略
        }

        // 执行目标方法
        Object result;
        try {
            result = joinPoint.proceed();
            event.setCostTime((int) (System.currentTimeMillis() - startTime));

            // 序列化响应结果（截断）
            String responseStr = JsonUtils.toJson(result);
            if (responseStr != null && responseStr.length() > MAX_RESPONSE_LENGTH) {
                responseStr = responseStr.substring(0, MAX_RESPONSE_LENGTH) + "...（截断）";
            }
            event.setResponseResult(responseStr);

            // 非阻塞入队
            logBuffer.offerOperaLog(event);

        } catch (Throwable e) {
            event.setCostTime((int) (System.currentTimeMillis() - startTime));
            event.setResponseResult("异常: " + e.getClass().getSimpleName() + ": " + e.getMessage());

            // 即使异常也记录操作日志（标记异常状态）
            logBuffer.offerOperaLog(event);

            throw e;
        }

        return result;
    }

    /**
     * 从方法所在 Controller 类解析模块名
     * <p>例如: com.yub.system.controller.SysDictController → dict</p>
     */
    private String resolveModule(Method method) {
        String className = method.getDeclaringClass().getSimpleName();
        // 去除 Controller/Manage/Service 后缀，统一小写
        String name = className
                .replaceAll("(Student|Sys|)(.*?)(Controller|Manage|Service)$", "$2")
                .replaceAll("([a-z])([A-Z])", "$1-$2")
                .toLowerCase();
        return name.isEmpty() ? className : name;
    }

    /**
     * 将方法参数转为 JSON 字符串
     * <p>跳过 HttpServletRequest/Response/Session 等 Servlet 对象</p>
     */
    private String argsToJson(Object[] args) {
        List<Object> filteredArgs = Arrays.stream(args)
                .filter(arg -> !(arg instanceof jakarta.servlet.ServletRequest
                        || arg instanceof jakarta.servlet.ServletResponse
                        || arg instanceof jakarta.servlet.http.HttpSession
                        || arg instanceof java.security.Principal))
                .collect(Collectors.toList());

        if (filteredArgs.isEmpty()) {
            return "[]";
        }
        return JsonUtils.toJson(filteredArgs);
    }

    /**
     * 脱敏处理
     * <p>
     * 对 JSON 字符串中的敏感字段值进行脱敏替换。
     * 支持：password/secret/token 字段值替换为 ***，
     * 手机号中间4位隐藏，身份证号中间8位隐藏。
     * </p>
     */
    private String desensitize(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }

        String result = json;

        // 替换敏感字段值："password":"123456" → "password":"***"
        for (Pattern pattern : SENSITIVE_PATTERNS) {
            String regex = "(\"" + pattern.pattern() + "\"\\s*:\\s*\").*?(\")";
            result = result.replaceAll(regex, "$1***$2");
        }

        // 手机号脱敏：13812345678 → 138****5678
        result = PHONE_PATTERN.matcher(result).replaceAll("$1****$2");

        // 身份证号脱敏：110101199001011234 → 110101********1234
        result = ID_CARD_PATTERN.matcher(result).replaceAll("$1********$2");

        return result;
    }
}
