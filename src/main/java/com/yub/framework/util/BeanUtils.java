package com.yub.framework.util;

import com.yub.framework.exception.FrameworkErrorCode;
import com.yub.framework.exception.FrameworkException;
import org.springframework.beans.BeansException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Bean 对象拷贝工具类
 *
 * @Author: bing.yu
 * @CreateTime: 2026-06-11
 * @Description: 基于 Spring BeanUtils 的泛型拷贝封装
 * @Version: 1.0.0
 */
public class BeanUtils {

    /**
     * 拷贝单个对象
     *
     * @param source      源对象
     * @param targetClass 目标类型
     * @param <T>         目标类型泛型
     * @return 拷贝后的目标对象
     */
    public static <T> T copy(Object source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }
        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            org.springframework.beans.BeanUtils.copyProperties(source, target);
            return target;
        } catch (BeansException | ReflectiveOperationException e) {
            throw new FrameworkException(e, FrameworkErrorCode.BEAN_COPY_ERROR, source.getClass().getName(), targetClass.getName());
        }
    }

    /**
     * 拷贝对象列表
     *
     * @param sourceList  源对象列表
     * @param targetClass 目标类型
     * @param <T>         目标类型泛型
     * @return 拷贝后的目标对象列表
     */
    public static <T> List<T> copyList(List<?> sourceList, Class<T> targetClass) {
        if (sourceList == null || sourceList.isEmpty()) {
            return Collections.emptyList();
        }
        List<T> resultList = new ArrayList<>(sourceList.size());
        for (Object source : sourceList) {
            resultList.add(copy(source, targetClass));
        }
        return resultList;
    }
}
