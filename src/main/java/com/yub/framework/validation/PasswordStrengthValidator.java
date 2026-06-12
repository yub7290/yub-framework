package com.yub.framework.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * 密码强度校验器
 * <p>校验密码是否包含大写字母、小写字母、数字、特殊字符中至少三种</p>
 *
 * @Author: bing.yu
 * @CreateTime: 2026-06-12
 * @Description: 密码强度校验器
 * @Version: 1.0.0
 */
public class PasswordStrengthValidator implements ConstraintValidator<PasswordStrength, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true; // @NotBlank 会处理空值校验
        }

        int count = 0;
        if (value.matches(".*[a-z].*")) count++;      // 小写字母
        if (value.matches(".*[A-Z].*")) count++;      // 大写字母
        if (value.matches(".*\\d.*")) count++;        // 数字
        if (value.matches(".*[^a-zA-Z0-9].*")) count++; // 特殊字符

        return count >= 3;
    }
}
