package com.yub.framework.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yub.framework.exception.FrameworkErrorCode;
import com.yub.framework.exception.FrameworkException;

/**
 * JSON工具类
 *
 * @Author: bing.yu
 * @CreateTime: 2026-06-10
 * @Description: JSON工具类
 * @Version: 1.0
 */
public class JsonUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 将对象转换为JSON字符串
     *
     * @param object 对象
     * @return JSON字符串
     */
    public static String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new FrameworkException(FrameworkErrorCode.JSON_PARSE_ERROR, e);
        }
    }

}
